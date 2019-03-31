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

  def httpClientRetryer(isRetryableClientException: HttpClientException => Boolean): Retryer = {
    Retry
      .withBackOff(maxRetry = 10)
      .withErrorHandler { ctx: RetryContext =>
        warn(s"Request failed: ${ctx.lastError.getMessage}")
        ctx.lastError match {
          case e: HttpClientException =>
            e.status match {
              case HttpStatus.TooManyRequests => // TOO_MANY_REQUESTS
              // e.g., AWS S3 will not return this code, but 429 should be retryable in general
              case HttpStatus.ServiceUnavailable => // SERVICE_UNAVAILABLE
                // S3 slow down. We need to reduce the request rate.
                Thread.sleep((ctx.nextWaitMillis * 0.5).toLong) // Add an additional wait
              case s if s.isServerError => // 5xx
              // We should retry on any server side errors
              case s if s.isClientError && isRetryableClientException(e) =>
              // OK to retry for some specific 400 errors
              case other =>
                // Throw an exception upon non recoverable errors
                throw e
            }
          case e: TimeoutException =>
          // Just retry upon timeout
          case ex: IOException =>
            // InputStreamResponseListner may wrap the error with IOException
            handleExecutionException(ex)
          case ex: ExecutionException =>
            handleExecutionException(ex)
          case other =>
            // We canot retry when an unknown exception is thrown
            throw other
        }
      }
      .beforeRetry { ctx =>
        warn(f"[${ctx.retryCount}/${ctx.maxRetry}] Retry the request in ${ctx.nextWaitMillis / 1000.0}%.2f sec.")
      }
  }

  def handleExecutionException(ex: Exception): Unit = {
    // Other types of exception that happen inside jetty client
    ex.getCause match {
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
      case other =>
        throw ex
    }

  }

}
