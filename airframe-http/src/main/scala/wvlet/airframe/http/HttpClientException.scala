/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.http
import java.io.EOFException
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.channels.ClosedChannelException
import java.util.concurrent.{ExecutionException, TimeoutException}

import javax.net.ssl.{SSLException, SSLHandshakeException, SSLKeyException, SSLPeerUnverifiedException}
import wvlet.airframe.control.{ResultClass, Retry}
import wvlet.airframe.control.ResultClass.{Failed, Succeeded, nonRetryableFailure, retryableFailure}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.log.LogSupport

import scala.language.existentials

/**
  */
class HttpClientException(val response: HttpResponse[_], val status: HttpStatus, message: String, cause: Throwable)
    extends Exception(message, cause) {
  def this(response: HttpResponse[_], status: HttpStatus) = this(response, status, response.status.toString, null)
  def this(response: HttpResponse[_], status: HttpStatus, message: String) =
    this(response, status, s"${status} ${message}", null)
  def this(response: HttpResponse[_], status: HttpStatus, cause: Throwable) =
    this(response, status, s"${status} ${cause.getMessage}", cause)
  def statusCode: Int = status.code
}

case class HttpClientMaxRetryException(
    override val response: HttpResponse[_],
    retryContext: RetryContext,
    cause: Throwable
) extends HttpClientException(
      response = response,
      status = {
        cause match {
          case e: HttpClientException =>
            e.status
          case _ =>
            HttpStatus.Unknown_000
        }
      },
      message = s"Reached the max retry count ${retryContext.retryCount}/${retryContext.maxRetry}: ${cause.getMessage}",
      cause
    )

/**
  * Common classifiers for HTTP client responses and exceptions in order to retry HTTP requests.
  */
object HttpClientException extends LogSupport {
  private def requestFailure[Resp](response: Resp)(implicit adapter: HttpResponseAdapter[Resp]): HttpClientException = {
    val status  = adapter.statusOf(response)
    val content = adapter.contentStringOf(response)
    if (content == null || content.isEmpty) {
      new HttpClientException(adapter.wrap(response), status)
    } else {
      new HttpClientException(adapter.wrap(response), status, s"Request failed: ${content}")
    }
  }

  /**
    * The default classifier of http responses to determine whether the request has succeeded or not.
    *
    * @param response
    * @param adapter
    * @tparam Resp
    * @return
    */
  def classifyHttpResponse[Resp](response: Resp)(implicit adapter: HttpResponseAdapter[Resp]): ResultClass = {
    val status = adapter.statusOf(response)
    status match {
      case s if s.isSuccessful =>
        Succeeded
      case s if s.isServerError =>
        // We should retry on any server side errors
        val f = retryableFailure(requestFailure(response))
        if (status == HttpStatus.ServiceUnavailable_503) {
          // Server is busy (e.g., S3 slow down). We need to reduce the request rate.
          f.withExtraWaitFactor(0.5)
        } else {
          f
        }
      case s if s.isClientError => // 4xx
        s match {
          case HttpStatus.BadRequest_400 if isRetryable400ErrorMessage(adapter.contentStringOf(response)) =>
            // Some 400 errors can be caused by a client side error
            retryableFailure(requestFailure(response))
          case HttpStatus.RequestTimeout_408 =>
            // #1171: Client side timeout should be retryable
            retryableFailure(requestFailure(response))
          case HttpStatus.Gone_410 =>
            // e.g., Server might have failed to process the request
            retryableFailure(requestFailure(response))
          case HttpStatus.TooManyRequests_429 =>
            // e.g., Server might return this code when busy. 429 should be retryable in general
            retryableFailure(requestFailure(response))
          case HttpStatus.ClientClosedRequest_499 =>
            // e.g., client-side might have closed the connection.
            retryableFailure(requestFailure(response))
          case _ =>
            nonRetryableFailure(requestFailure(response))
        }
      case _ =>
        nonRetryableFailure(requestFailure(response))
    }
  }

  private def isRetryable400ErrorMessage(m: String): Boolean = {
    val retriable400ErrorMessage = Seq(
      // OkHttp might closes the client connection
      // https://stackoverflow.com/questions/48277426/java-lang-nosuchmethoderror-okio-bufferedsource-rangeequalsjlokio-bytestring
      "Idle connections will be closed".r
    )

    retriable400ErrorMessage.find { pattern =>
      pattern.findFirstIn(m).isDefined
    }.isDefined
  }

  /**
    * The default classifier for http client exception
    *
    * @param ex
    * @return
    */
  def classifyExecutionFailure(ex: Throwable): Failed = {
    executionFailureClassifier.applyOrElse(ex, nonRetryable)
  }

  def executionFailureClassifier: PartialFunction[Throwable, Failed] =
    connectionExceptionClassifier orElse
      sslExceptionClassifier orElse
      invocationTargetExceptionClassifier

  def connectionExceptionClassifier: PartialFunction[Throwable, Failed] = {
    // Other types of exception that can happen inside HTTP clients (e.g., Jetty)
    case e: java.lang.InterruptedException =>
      // Retryable when the http client thread execution is interrupted.
      retryableFailure(e)
    case e: ProtocolException      => retryableFailure(e)
    case e: ConnectException       => retryableFailure(e)
    case e: EOFException           => retryableFailure(e)
    case e: TimeoutException       => retryableFailure(e)
    case e: ClosedChannelException => retryableFailure(e)
    case e: SocketTimeoutException => retryableFailure(e)
    case e: SocketException =>
      e match {
        case se: BindException                      => retryableFailure(e)
        case se: ConnectException                   => retryableFailure(e)
        case se: NoRouteToHostException             => retryableFailure(e)
        case se: PortUnreachableException           => retryableFailure(e)
        case se if se.getMessage == "Socket closed" => retryableFailure(e)
        case other =>
          nonRetryableFailure(e)
      }
    // Exceptions from Finagle. Using the string class names so as not to include Finagle dependencies.
    case e: Throwable if finagleRetryableExceptionClasses.contains(e.getClass().getName) =>
      retryableFailure(e)
  }

  private[http] val finagleRetryableExceptionClasses = Set(
    "com.twitter.finagle.ChannelClosedException",
    "com.twitter.finagle.ReadTimedOutException",
    "com.twitter.finagle.WriteTimedOutException"
  )

  def sslExceptionClassifier: PartialFunction[Throwable, Failed] = { case e: SSLException =>
    e match {
      // Deterministic SSL exceptions are not retryable
      case se: SSLHandshakeException      => nonRetryableFailure(e)
      case se: SSLKeyException            => nonRetryableFailure(e)
      case s3: SSLPeerUnverifiedException => nonRetryableFailure(e)
      case other                          =>
        // SSLProtocolException and uncategorized SSL exceptions (SSLException) such as unexpected_message may be retryable
        retryableFailure(e)
    }
  }

  def invocationTargetExceptionClassifier: PartialFunction[Throwable, Failed] = {
    case e: ExecutionException if e.getCause != null =>
      classifyExecutionFailure(e.getCause)
    case e: InvocationTargetException =>
      classifyExecutionFailure(e.getTargetException)
    case e if e.getCause != null =>
      // Trace the true cause
      classifyExecutionFailure(e.getCause)
  }

  def nonRetryable: Throwable => Failed = { case e =>
    // We canot retry when an unknown exception is thrown
    nonRetryableFailure(e)
  }
}
