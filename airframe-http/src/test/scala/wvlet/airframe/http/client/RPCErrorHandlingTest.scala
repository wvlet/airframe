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

import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Http, HttpMessage, HttpStatus, RPCMethod, RPCStatus, RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success}

object RPCErrorHandlingTest extends AirSpec {

  private class DummyHttpChannel extends HttpChannel {
    private val requestCount = new AtomicInteger(0)

    override def send(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Response = ???
    override def sendAsync(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Rx[Response] = {
      requestCount.getAndIncrement() match {
        case 0 =>
          // For retry test
          Rx.single(Http.response(HttpStatus.InternalServerError_500))
        case _ =>
          throw RPCStatus.INVALID_REQUEST_U1.newException("RPC error")
      }
    }
    override def close(): Unit = {}
  }

  initDesign {
    _.bind[AsyncClient].toInstance {
      val config = Http.client // .withClientFilter(null)
      new AsyncClientImpl(new DummyHttpChannel, config)
    }
  }

  test("test error response") { (client: AsyncClient) =>
    val dummyRPCMerthod =
      RPCMethod("/rpc_method", "demo.RPCClass", "hello", Surface.of[Map[String, Any]], Surface.of[String])

    var observedRetry = false
    val myClient = client.withClientFilter(new RxHttpFilter {
      override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[Response] = {
        next(request).tapOn {
          case Success(x) =>
            x.status match {
              case HttpStatus.InternalServerError_500 =>
                observedRetry = true
              case _ =>
            }
          case Failure(e) =>
            logger.info(e)
        }
      }
    })

    myClient.rpc[Map[String, Any], String](dummyRPCMerthod, Map("message" -> "world")).transform {
      case scala.util.Failure(e) =>
        observedRetry shouldBe true
        e.getMessage shouldContain "RPC error"
      case _ =>
        fail("should fail")
    }
  }
}
