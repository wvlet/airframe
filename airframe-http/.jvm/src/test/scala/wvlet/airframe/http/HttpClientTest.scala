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
import java.util.concurrent.{ExecutionException, TimeoutException}

import javax.net.ssl.{SSLHandshakeException, SSLKeyException, SSLPeerUnverifiedException}
import wvlet.airspec.AirSpec

/**
  *
  */
class HttpClientTest extends AirSpec {
  import HttpClient._
  abstract class RetryTest(expectedRetryCount: Int, expectedExecCount: Int) {
    val retryer = defaultHttpClientRetry[SimpleHttpRequest, SimpleHttpResponse]
      .withBackOff(initialIntervalMillis = 0)
    var retryCount = 0
    var execCount  = 0

    def body: SimpleHttpResponse

    def run = {
      retryer.run {
        if (execCount > 0) {
          retryCount += 1
        }
        execCount += 1
        val ret = if (retryCount == 0) {
          body
        } else {
          SimpleHttpResponse(HttpStatus.Ok_200)
        }
        ret
      }
    }

    try {
      run
    } catch {
      case e: Throwable =>
    } finally {
      retryCount shouldBe expectedRetryCount
      execCount shouldBe expectedExecCount
    }
  }

  def `retry on failed http requests`: Unit = {
    val retryableResponses: Seq[SimpleHttpResponse] = Seq(
      SimpleHttpResponse(HttpStatus.ServiceUnavailable_503),
      SimpleHttpResponse(HttpStatus.TooManyRequests_429),
      SimpleHttpResponse(HttpStatus.InternalServerError_500),
      SimpleHttpResponse(
        HttpStatus.BadRequest_400,
        "Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed."
      )
    )
    retryableResponses.foreach { r =>
      new RetryTest(expectedRetryCount = 1, expectedExecCount = 2) {
        override def body = r
      }
    }
  }
  def `never retry on deterministic http request failrues`: Unit = {
    val nonRetryableResponses: Seq[SimpleHttpResponse] = Seq(
      SimpleHttpResponse(HttpStatus.BadRequest_400, "bad request"),
      SimpleHttpResponse(HttpStatus.Unauthorized_401, "permission deniend"),
      SimpleHttpResponse(HttpStatus.Forbidden_403, "forbidden"),
      SimpleHttpResponse(HttpStatus.NotFound_404, "not found"),
      SimpleHttpResponse(HttpStatus.Conflict_409, "conflict")
    )

    nonRetryableResponses.foreach { r =>
      new RetryTest(expectedRetryCount = 0, expectedExecCount = 1) {
        def body = r
      }
    }
  }

  def `retry on non-deterministic failures`: Unit = {
    val retryableExceptions: Seq[Throwable] = Seq(
      new TimeoutException("timeout"),
      new ExecutionException(new InterruptedException("exception")),
      new ExecutionException(new ProtocolException("protocol exception")),
      new ExecutionException(new ConnectException("connect")),
      new ExecutionException(new EOFException("eof")),
      new ExecutionException(new TimeoutException("timeout")),
      new ExecutionException(new BindException("exception")),
      new ExecutionException(new ConnectException("exception")),
      new ExecutionException(new NoRouteToHostException("exception")),
      new ExecutionException(new PortUnreachableException("exception")),
      new InvocationTargetException(new TimeoutException("timeout at reflection call"))
    )

    retryableExceptions.foreach { e =>
      new RetryTest(expectedRetryCount = 1, expectedExecCount = 2) {
        override def body = throw e
      }
    }
  }

  def `never retry on deterministic failures`: Unit = {
    val nonRetryableExceptions: Seq[Throwable] = Seq(
      new ExecutionException(new SSLHandshakeException("exception")),
      new ExecutionException(new SSLKeyException("exception")),
      new ExecutionException(new SSLPeerUnverifiedException("exception")),
      new IllegalArgumentException("illegal argument exception"),
      new InvocationTargetException(new IllegalArgumentException("reflection call-site error"))
    )

    nonRetryableExceptions.foreach { e =>
      new RetryTest(expectedRetryCount = 0, expectedExecCount = 1) {
        def body = throw e
      }
    }
  }
}
