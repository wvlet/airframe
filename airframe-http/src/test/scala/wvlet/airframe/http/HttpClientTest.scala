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
import wvlet.airframe.AirframeSpec

/**
  *
  */
class HttpClientTest extends AirframeSpec {

  trait RetryTestBase {
    val retryer    = HttpClient.defaultRetryer
    var retryCount = 0
    var execCount  = 0

    def throwError: Nothing

    def run = {
      retryer.run {
        if (retryCount == 0) {
          retryCount += 1
          throwError
        }
        execCount += 1
      }
    }
  }

  trait RetryTest extends RetryTestBase {
    run
    retryCount shouldBe 1
    execCount shouldBe 1
  }

  trait NonRetryTest extends RetryTestBase {
    try {
      run
    } catch {
      case e: Throwable =>
    } finally {
      retryCount shouldBe 1
      execCount shouldBe 0
    }
  }

  "retry on non-deterministic failures" in {
    val retryableExceptions: Seq[Throwable] = Seq(
      new HttpClientException(HttpStatus.ServiceUnavailable_503),
      new HttpClientException(HttpStatus.TooManyRequests_429),
      new HttpClientException(HttpStatus.InternalServerError_500),
      new HttpClientException(
        HttpStatus.BadRequest_400,
        "Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed."
      ),
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
      new RetryTest {
        def throwError = throw e
      }
    }

  }

  "never retry on deterministic failures" in {
    val nonRetryableExceptions: Seq[Throwable] = Seq(
      new HttpClientException(HttpStatus.BadRequest_400, "bad request"),
      new HttpClientException(HttpStatus.Unauthorized_401, "permission deniend"),
      new HttpClientException(HttpStatus.Forbidden_403, "forbidden"),
      new HttpClientException(HttpStatus.NotFound_404, "not found"),
      new HttpClientException(HttpStatus.Conflict_409, "conflict"),
      new ExecutionException(new SSLHandshakeException("exception")),
      new ExecutionException(new SSLKeyException("exception")),
      new ExecutionException(new SSLPeerUnverifiedException("exception")),
      new IllegalArgumentException("exception"),
      new InvocationTargetException(new IllegalArgumentException("reflection call-site error"))
    )

    nonRetryableExceptions.foreach { e =>
      new NonRetryTest {
        def throwError = throw e
      }
    }
  }

}
