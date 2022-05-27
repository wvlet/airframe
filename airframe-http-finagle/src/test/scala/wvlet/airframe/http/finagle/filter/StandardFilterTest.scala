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
package wvlet.airframe.http.finagle.filter

import wvlet.airframe.Design
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.finagle.{Finagle, FinagleContext, FinagleServer}
import wvlet.airframe.http.{Endpoint, Http, HttpContext, HttpMessage, Router}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import scala.concurrent.Future

object StandardFilterTest extends AirSpec {

  object MyFilter extends Http.Filter with LogSupport {
    override def apply(request: HttpMessage.Request, context: Context): Future[HttpMessage.Response] = {
      debug(s"request: ${request}")
      context.setThreadLocal("request-id", "xxxx")
      context(request.withHeader("X-App", "myapp")).map { resp =>
        debug(s"response: ${resp}")
        val msg = resp.contentString
        resp.withContent(s"[Filtered] ${msg}!")
      }
    }
  }

  trait MyAPI {
    @Endpoint(path = "/")
    def hello(
        name: String = "Finagle",
        request: HttpMessage.Request,
        // TODO: Support Http.Context
        context: FinagleContext
    ): String = {
      val requestId = context.getThreadLocal("request-id").getOrElse("yyyy")
      s"[${requestId}] Hello ${request.header.getOrElse("X-App", "unknown")}"
    }
  }

  private val router = Router.add(MyFilter).andThen[MyAPI]

  protected override def design =
    Design.newDesign
      .add(Finagle.server.withRouter(router).design)
      .bind[SyncClient]
      .toProvider { server: FinagleServer =>
        Http.client
          .withRetryContext(_.noRetry)
          .newSyncClient(server.localAddress)
      }

  test("use standard filter") { client: SyncClient =>
    val resp = client.send(Http.request("/"))
    resp.contentString shouldBe "[Filtered] [xxxx] Hello myapp!"
  }

}
