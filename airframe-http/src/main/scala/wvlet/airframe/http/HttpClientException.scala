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
import java.io.{EOFException, IOException}
import java.lang.reflect.InvocationTargetException
import java.net._
import java.util.concurrent.{ExecutionException, TimeoutException}

import javax.net.ssl.{SSLException, SSLHandshakeException, SSLKeyException, SSLPeerUnverifiedException}
import wvlet.airframe.control.ResultClass
import wvlet.airframe.control.ResultClass.{Failed, NonRetryableFailure, RetryableFailure, Successful}
import wvlet.airframe.control.Retry.ExceptionClassifier

/**
  *
  */
class HttpClientException(val status: HttpStatus, message: String, cause: Throwable) extends Exception(message, cause) {
  def this(status: HttpStatus) = this(status, status.toString, null)
  def this(status: HttpStatus, message: String) = this(status, s"${status} ${message}", null)
  def this(status: HttpStatus, cause: Throwable) = this(status, s"${status} ${cause.getMessage}", cause)
  def statusCode: Int = status.code
}

/**
  * Common retry patterns for HTTP client exceptions
  */
object HttpClientException {

  def defaultResponseClassifier[Resp](res: Resp)(implicit adapter: HttpResponseAdapter[Resp]): ResultClass = {
    val status = adapter.statusOf(res)
    status match {
      case s if s.isSuccessful =>
        Successful
      case other =>
        retryOnHttpStatusError(other)
    }
  }

  def retryOnHttpStatusError: HttpStatus => Failed = {
    case s if s.isServerError =>
      // We should retry on any server side errors
      RetryableFailure
    case s if s.isClientError => // 4xx
      s match {
        case HttpStatus.TooManyRequests_429 =>
          // e.g., Server might return this code when busy. 429 should be retryable in general
          RetryableFailure
        case HttpStatus.Gone_410 =>
          // e.g., Server might have failed to process the request
          RetryableFailure
        case HttpStatus.ClientClosedRequest_499 =>
          // e.g., client-side might have closed the connection.
          RetryableFailure
        case other =>
          NonRetryableFailure // Non-retryable error
      }
  }

  def retryOnHttpClientException(
      executionFailureHandler: Throwable => Failed = defaultExecutionFailureClassifier): ExceptionClassifier = {
    case e: HttpClientException =>
      e.status match {
        // OK to retry for some specific 400 errors
        case HttpStatus.BadRequest_400 if isRetryableErrorMessage(e.getMessage) =>
          // Some 400 errors can be caused by a client side error
          RetryableFailure
        case other =>
          retryOnHttpStatusError(e.status)
      }
    case e: TimeoutException =>
      // Just retry upon timeout
      RetryableFailure
    case ex: IOException =>
      // Timeout, SSL related exception,
      // InputStreamResponseListner of Jetty may wrap the error with IOException
      executionFailureHandler(ex)
    case ex: ExecutionException =>
      executionFailureHandler(ex)
    case ex: InvocationTargetException =>
      executionFailureHandler(ex)
    case other =>
      // We canot retry when an unknown exception is thrown
      NonRetryableFailure
  }

  private val retriable400ErrorMessage = Seq(
    // OkHttp might closes the client connection
    // https://stackoverflow.com/questions/48277426/java-lang-nosuchmethoderror-okio-bufferedsource-rangeequalsjlokio-bytestring
    "Idle connections will be closed".r
  )

  private def isRetryableErrorMessage(m: String): Boolean = {
    retriable400ErrorMessage.find { pattern =>
      pattern.findFirstIn(m).isDefined
    }.isDefined
  }

  def defaultExecutionFailureClassifier(ex: Throwable): Failed = {
    // Other types of exception that can happen inside HTTP clients (e.g., Jetty)
    ex match {
      case e: java.lang.InterruptedException =>
        // Retryable when the http client thread execution is interrupted.
        RetryableFailure
      case e: ProtocolException => RetryableFailure
      case e: ConnectException  => RetryableFailure
      case e: EOFException      => RetryableFailure
      case e: TimeoutException  => RetryableFailure
      case e: SocketException =>
        e match {
          case se: BindException            => RetryableFailure
          case se: ConnectException         => RetryableFailure
          case se: NoRouteToHostException   => RetryableFailure
          case se: PortUnreachableException => RetryableFailure
          case other =>
            NonRetryableFailure
        }
      case e: SSLException =>
        e match {
          // Deterministic SSL exceptions are not retryable
          case se: SSLHandshakeException      => NonRetryableFailure
          case se: SSLKeyException            => NonRetryableFailure
          case s3: SSLPeerUnverifiedException => NonRetryableFailure
          case other                          =>
            // SSLProtocolException and uncategorized SSL exceptions (SSLException) such as unexpected_message may be retryable
            RetryableFailure
        }
      case e: InvocationTargetException =>
        defaultExecutionFailureClassifier(e.getTargetException)
      case other if ex.getCause != null =>
        // Trace the true cause
        defaultExecutionFailureClassifier(ex.getCause)
      case other =>
        NonRetryableFailure
    }
  }

}
