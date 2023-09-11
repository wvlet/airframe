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
package wvlet.airframe.http.client

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.TimeoutException

object HttpClientFilterTest extends AirSpec {

  private def newDummyClient(config: HttpClientConfig, f: PartialFunction[Request, Response]): AsyncClient =
    new AsyncClientImpl(new DummyHttpChannel(f), config)

  private class DummyHttpChannel(reply: PartialFunction[Request, Response]) extends HttpChannel {
    private val requestCount = new AtomicInteger(0)

    override def send(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Response = {
      if (reply.isDefinedAt(req)) {
        reply(req)
      } else {
        throw RPCStatus.NOT_FOUND_U5.newException(s"RPC method not found: ${req.path}")
      }
    }
    override def sendAsync(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Rx[Response] = {
      Rx.single(send(req, channelConfig))
    }
    override def close(): Unit = {}
  }

  private val dummyRPCMethod =
    RPCMethod("/rpc_method", "demo.RPCClass", "hello", Surface.of[Map[String, Any]], Surface.of[String])

  test("get the last error response after retry attempts") {
    val requestCount  = new AtomicInteger(0)
    var observedError = false

    val client = newDummyClient(
      Http.client.withClientFilter(new RxHttpFilter {
        override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[Response] = {
          next(request).tapOnFailure { e =>
            observedError = true
          }
        }
      }),
      { case req: Request =>
        requestCount.getAndIncrement() match {
          case 0 =>
            Http.response(HttpStatus.InternalServerError_500)
          case _ =>
            throw RPCStatus.INVALID_REQUEST_U1.newException("RPC dummy error")
        }
      }
    )
    client.rpc[Map[String, Any], String](dummyRPCMethod, Map("message" -> "world")).recover {
      case e: RPCException if e.status == RPCStatus.INVALID_REQUEST_U1 =>
        e.getMessage shouldContain "RPC dummy error"
        observedError shouldBe true
    }
  }

  test("handle error inside the client filter") {
    val client = newDummyClient(
      Http.client.withClientFilter(new RxHttpFilter {
        override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
          throw RPCStatus.PERMISSION_DENIED_U14.newException("test client filter error")
        }
      }),
      { case req: Request =>
        Http.response(HttpStatus.Ok_200)
      }
    )

    client.sendSafe(Http.request("/")).recover {
      case e: RPCException if e.status == RPCStatus.PERMISSION_DENIED_U14 =>
        e.getMessage shouldContain "test client filter error"
    }
  }

  test("handle last error response") {
    val client = newDummyClient(
      Http.client.withRetryContext(_.noRetry),
      { case req: Request => throw new TimeoutException("timeout") }
    )

    client.sendSafe(Http.request("/")).map { resp =>
      resp.status shouldBe HttpStatus.InternalServerError_500
    }
  }

  test("add a custom header") {
    val client = newDummyClient(
      Http.client.withRequestFilter(_.withAuthorization("Bearer xxx")),
      { case req: Request =>
        req.authorization shouldBe Some("Bearer xxx")
        Http.response(HttpStatus.Ok_200)
      }
    )

    client.sendSafe(Http.request("/")).map { resp =>
      resp.status shouldBe HttpStatus.Ok_200
    }
  }

}
