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
package wvlet.airframe.http.netty

import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.{Endpoint, Http, HttpHeader, HttpMessage, RxRouter, StaticContent}
import wvlet.airspec.AirSpec

object StaticContentTest extends AirSpec {
  class StaticContentServer {
    private val content: StaticContent = StaticContent.fromDirectory("./airframe-http-netty/src/test/resources/static")

    @Endpoint(path = "/*path")
    def staticContent(path: String): HttpMessage.Response = {
      if path.isEmpty then {
        content("index.html")
      } else
        content(path)
    }
  }

  override protected def design = {
    Netty.server
      .withRouter(RxRouter.of[StaticContentServer])
      .designWithSyncClient
  }

  test("read index.html") { (client: SyncClient) =>
    val resp = client.send(Http.GET("/"))
    resp.contentString shouldContain "<html>"
    resp.contentType shouldBe Some("text/html")
  }
}
