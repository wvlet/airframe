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
import wvlet.airframe.Design
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.filter.Cors
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

object CorsFilterTest {

  // Test SimpleFilter of Finagle
  object CheckFilter extends RxHttpFilter with LogSupport {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      debug(s"checking request: ${request}")
      next(request)
    }
  }

  object LogFilter extends RxHttpFilter with LogSupport {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      debug(s"logging request: ${request}")
      next(request)
    }
  }

  object SimpleEndpoint extends RxHttpEndpoint with LogSupport {
    override def apply(request: HttpMessage.Request): Rx[HttpMessage.Response] =
      Rx.single {
        debug(s"handling request: ${request.header}")
        Http.response(HttpStatus.Ok_200, request.header.toMultiMap.mkString(", "))
      }
  }
}

/**
  */
class CorsFilterTest extends AirSpec {
  import CorsFilterTest.*

  initDesign { d =>
    val r = RxRouter
      .filter(CheckFilter)
      .andThen(LogFilter)
      .andThen(Cors.newFilter(Cors.unsafePermissivePolicy))
      .andThen(SimpleEndpoint)
    debug(r)

    Netty.server.withRouter(r).designWithSyncClient
  }

  test("support CORS filter") { (client: SyncClient) =>
    val resp = client.send(Http.GET("/").withHeader("Origin", "https://example.com"))
    debug(resp.header)
    // Check Cors headers
    resp.header.get("access-control-allow-credentials") shouldBe Some("true")
    resp.header.get("access-control-allow-origin") shouldBe Some("https://example.com")
  }

}
