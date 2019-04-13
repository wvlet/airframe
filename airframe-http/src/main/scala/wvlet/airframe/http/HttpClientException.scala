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
import wvlet.airframe.control.{Failed, NonRetryableError, ResultClass, RetryableError}
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

//  def defaultResponseHandler[Res](implicit res: HttpResponse[Res]): PartialFunction[Res, ResultClass] = {
//    case e if res.statusOf(e).isServerError =>
//  }

  def defaultHttpExceptionHandler(
      clientErrorHandler: HttpClientException => Failed = defaultClientErrorHandler,
      executionFailureHandler: Throwable => Failed = defaultExecutionFailureHandler): ExceptionClassifier = {
    case e: HttpClientException =>
      e.status match {
        case s if s.isServerError => // 5xx
          // We should retry on any server side errors
          RetryableError
        case s if s.isClientError => // 4xx
          // OK to retry for some specific 400 errors
          clientErrorHandler(e)
        case other =>
          // Throw an exception upon non recoverable errors
          NonRetryableError(e)
      }
    case e: TimeoutException =>
      // Just retry upon timeout
      RetryableError
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
      NonRetryableError(other)
  }

  private val retriable400ErrorMessage = Seq(
    // OkHttp might closes the client connection
    // https://stackoverflow.com/questions/48277426/java-lang-nosuchmethoderror-okio-bufferedsource-rangeequalsjlokio-bytestring
    "Idle connections will be closed".r
  )

  private def retryableClientErrorMessage(m: String): Boolean = {
    retriable400ErrorMessage.find { pattern =>
      pattern.findFirstIn(m).isDefined
    }.isDefined
  }

  def defaultClientErrorHandler(ex: HttpClientException): Failed = {
    ex.status match {
      case HttpStatus.BadRequest_400 if retryableClientErrorMessage(ex.getMessage) =>
        // Some 400 errors can be caused by a client side error
        RetryableError
      case HttpStatus.TooManyRequests_429 =>
        // e.g., Server might return this code when busy. 429 should be retryable in general
        RetryableError
      case HttpStatus.Gone_410 =>
        // e.g., Server might have failed to process the request
        RetryableError
      case HttpStatus.ClientClosedRequest_499 =>
        // e.g., client-side might have closed the connection.
        RetryableError
      case other =>
        NonRetryableError(ex) // Non-retryable error
    }
  }

  def defaultExecutionFailureHandler(ex: Throwable): Failed = {
    // Other types of exception that can happen inside HTTP clients (e.g., Jetty)
    ex match {
      case e: java.lang.InterruptedException =>
        // Retryable when the http client thread execution is interrupted.
        RetryableError
      case e: ProtocolException => RetryableError
      case e: ConnectException  => RetryableError
      case e: EOFException      => RetryableError
      case e: TimeoutException  => RetryableError
      case e: SocketException =>
        e match {
          case se: BindException            => RetryableError
          case se: ConnectException         => RetryableError
          case se: NoRouteToHostException   => RetryableError
          case se: PortUnreachableException => RetryableError
          case other =>
            NonRetryableError(other)
        }
      case e: SSLException =>
        e match {
          // Deterministic SSL exceptions are not retryable
          case se: SSLHandshakeException      => NonRetryableError(ex)
          case se: SSLKeyException            => NonRetryableError(ex)
          case s3: SSLPeerUnverifiedException => NonRetryableError(ex)
          case other                          =>
            // SSLProtocolException and uncategorized SSL exceptions (SSLException) such as unexpected_message may be retryable
            RetryableError
        }
      case e: InvocationTargetException =>
        defaultExecutionFailureHandler(e.getTargetException)
      case other if ex.getCause != null =>
        // Trace the true cause
        defaultExecutionFailureHandler(ex.getCause)
      case other =>
        NonRetryableError(other)
    }
  }

}
