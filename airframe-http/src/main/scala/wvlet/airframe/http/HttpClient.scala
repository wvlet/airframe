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
import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.{RetryContext, Retryer}
import wvlet.log.LogSupport

/**
  *
  */
trait HttpClient {}

class HttpClientException(val status: HttpStatus, cause: Option[Exception]) extends Exception {
  def statusCode: Int              = status.code
  override def getCause: Throwable = cause.getOrElse(null)
}

object HttpClient extends LogSupport {

  def defaultRetryer: Retryer = {
    Retry
      .withBackOff(maxRetry = 10)
      .withErrorHandler(defaultErrorHandler)
      .beforeRetry(defaultBeforeRetryAction)
  }

  def defaultErrorHandler(ctx: RetryContext): Unit = {
    warn(s"Request failed: ${ctx.lastError.getMessage}")
    defaultHttpExceptionHandler().applyOrElse(ctx.lastError, { x: Throwable =>
      x match {
        case _ => throw x
      }
    })
  }

  def defaultHttpExceptionHandler(
      clientErrorHandler: HttpClientException => Unit = defaultClientErrorHandler,
      executionFailureHandler: Throwable => Unit = defaultExecutionFailureHandler): PartialFunction[Throwable, Unit] = {
    case e: HttpClientException =>
      e.status match {
        case s if s.isServerError => // 5xx
        // We should retry on any server side errors
        case s if s.isClientError => // 4xx
          // OK to retry for some specific 400 errors
          clientErrorHandler(e)
        case other =>
          // Throw an exception upon non recoverable errors
          throw e
      }
    case e: TimeoutException =>
    // Just retry upon timeout
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
      throw other
  }

  def defaultClientErrorHandler(ex: HttpClientException): Unit = {
    ex.status match {
      case HttpStatus.TooManyRequests_429 =>
      // e.g., Server might return this code when busy. 429 should be retryable in general
      case HttpStatus.Gone_410 =>
      // e.g., Server might have failed to process the request
      case HttpStatus.ClientClosedRequest_499 =>
      // e.g., client-side might have closed the connection.
      case other =>
        throw ex // Non-retryable error
    }
  }

  def defaultBeforeRetryAction(ctx: RetryContext): Unit = {
    val extraWaitMillis =
      ctx.lastError match {
        case e: HttpClientException if e.status == HttpStatus.ServiceUnavailable_503 =>
          // Server is busy (e.g., S3 slow down). We need to reduce the request rate.
          (ctx.nextWaitMillis * 0.5).toLong // Add an extra wait
        case _ =>
          0
      }
    val nextWaitMillis = ctx.nextWaitMillis + extraWaitMillis
    warn(f"[${ctx.retryCount}/${ctx.maxRetry}] Retry the request in ${nextWaitMillis / 1000.0}%.2f sec.")
    Thread.sleep(extraWaitMillis)
  }

  def defaultExecutionFailureHandler(ex: Throwable): Unit = {
    // Other types of exception that can happen inside HTTP clients (e.g., Jetty)
    ex match {
      case e: java.lang.InterruptedException =>
      // Retryable when the http client thread execution is interrupted.
      case e: ProtocolException => // Retryable
      case e: ConnectException  => // Retryable
      case e: EOFException      => // Retryable
      case e: TimeoutException  => // Retryable
      case e: SocketException => // Retryable
        e match {
          case se: BindException            => // Retryable
          case se: ConnectException         => // Retryable
          case se: NoRouteToHostException   => // Retryable
          case se: PortUnreachableException => // Retryable
          case other =>
            throw ex
        }
      case e: SSLException =>
        e match {
          // Deterministic SSL exceptions are not retryable
          case se: SSLHandshakeException      => throw ex
          case se: SSLKeyException            => throw ex
          case s3: SSLPeerUnverifiedException => throw ex
          case other                          =>
          // SSLProtocolException and uncategorized SSL exceptions (SSLException) such as unexpected_message may be retryable
        }
      case e: InvocationTargetException =>
        defaultExecutionFailureHandler(e.getTargetException)
      case other if ex.getCause != null =>
        // Trace the true cause
        defaultExecutionFailureHandler(ex.getCause)
      case other =>
        throw ex
    }
  }
}
