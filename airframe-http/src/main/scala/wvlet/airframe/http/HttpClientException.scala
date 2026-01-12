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
import wvlet.airframe.control.ResultClass.{Failed, Succeeded, nonRetryableFailure, retryableFailure}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{CircuitBreakerOpenException, ResultClass}
import wvlet.log.LogSupport

import java.io.EOFException
import java.util.concurrent.TimeoutException
import scala.annotation.tailrec
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
            HttpStatus.InternalServerError_500
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
    val status                  = adapter.statusOf(response)
    val isRPCException: Boolean = adapter.headerOf(response).get(HttpHeader.xAirframeRPCStatus).isDefined
    if (isRPCException) {
      val cause = RPCException.fromResponse(adapter.httpResponseOf(response))
      new HttpClientException(adapter.wrap(response), status, cause)
    } else {
      val content = adapter.contentStringOf(response)
      if (content == null || content.isEmpty || isRPCException) {
        new HttpClientException(adapter.wrap(response), status)
      } else {
        new HttpClientException(adapter.wrap(response), status, s"Request failed: ${content}")
      }
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
      case HttpStatus.NotModified_304 =>
        // 304 Not Modified is a successful response indicating cached content is still valid
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

  /**
    * The default exception classifier for Scala.js, which does not reference any JVM-specific pakckages, such as
    * java.net, java.reflect, etc.
    * @param ex
    * @return
    */
  def classifyExecutionFailureScalaJS(ex: Throwable): Failed = {
    (scalajsCompatibleFailureClassifier orElse
      rootCauseExceptionClassifierScalaJS).applyOrElse(ex, nonRetryable)
  }

  def executionFailureClassifier: PartialFunction[Throwable, Failed] = {
    scalajsCompatibleFailureClassifier orElse
      Compat.connectionExceptionClassifier orElse
      Compat.sslExceptionClassifier orElse
      Compat.rootCauseExceptionClassifier
  }

  def scalajsCompatibleFailureClassifier: PartialFunction[Throwable, Failed] = {
    // Make it non-retryable to fail-fast if the circuit is open
    case e: CircuitBreakerOpenException => nonRetryableFailure(e)
    case e: EOFException                => retryableFailure(e)
    case e: TimeoutException            => retryableFailure(e)
  }

  private[http] val finagleRetryableExceptionClasses = Set(
    "com.twitter.finagle.ChannelClosedException",
    "com.twitter.finagle.ReadTimedOutException",
    "com.twitter.finagle.WriteTimedOutException"
  )

  def isRetryableFinagleException(e: Throwable): Boolean = {
    @tailrec
    def iter(cl: Any): Boolean = {
      cl match {
        case null => false
        case e: Throwable =>
          iter(e.getClass)
        case cl: Class[_] if classOf[Throwable].isAssignableFrom(cl) =>
          if (finagleRetryableExceptionClasses.contains(cl.getName)) {
            true
          } else {
            // Traverse the parent exception class
            iter(cl.getSuperclass)
          }
        case _ => false
      }
    }
    iter(e)
  }

  /**
    * For ScalaJs and Scala Native, which don't have InvocationTargetException or ExecutionException
    * @return
    */
  def rootCauseExceptionClassifierScalaJS: PartialFunction[Throwable, Failed] = {
    case e if e.getCause != null =>
      // Trace the true cause
      classifyExecutionFailureScalaJS(e.getCause)
  }

  def nonRetryable: Throwable => Failed = { case e =>
    // We cannot retry when an unknown exception is thrown
    nonRetryableFailure(e)
  }
}
