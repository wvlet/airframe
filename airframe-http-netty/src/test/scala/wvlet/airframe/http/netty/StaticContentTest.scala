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
import wvlet.airframe.http.{Endpoint, Http, HttpHeader, HttpMessage, HttpStatus, RxRouter, StaticContent}
import wvlet.airspec.AirSpec

object StaticContentTest extends AirSpec {
  class StaticContentServer {
    private val content: StaticContent = StaticContent.fromDirectory("./airframe-http-netty/src/test/resources/static")

    @Endpoint(path = "/*path")
    def staticContent(path: String): HttpMessage.Response = {
      if (path.isEmpty) {
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

  test("reject path traversal attacks") { (client: SyncClient) =>
    // Test absolute path
    val respAbsolute = client.sendSafe(Http.GET("//../../etc/passwd"))
    respAbsolute.status shouldBe HttpStatus.Forbidden_403

    // Test directory traversal using ../
    val respTraversal = client.sendSafe(Http.GET("/../../../etc/passwd"))
    respTraversal.status shouldBe HttpStatus.Forbidden_403

    // Test directory traversal using // (double slashes)
    val respDoubleSlash = client.sendSafe(Http.GET("//etc/passwd"))
    respDoubleSlash.status shouldBe HttpStatus.Forbidden_403
  }

  test("reject path traversal attacks with normalized paths") { (client: SyncClient) =>
    // Test normalized path traversal
    val respNormalized = client.sendSafe(Http.GET("/foo/./bar/../../etc/passwd"))
    respNormalized.status shouldBe HttpStatus.NotFound_404
  }

  test("reject absolute path traversal") { (client: SyncClient) =>
    // Test absolute path traversal
    val respAbsolute = client.sendSafe(Http.GET("/////etc/passwd"))
    respAbsolute.status shouldBe HttpStatus.Forbidden_403

    // Test absolute path traversal with double slashes
    val respDoubleSlash = client.sendSafe(Http.GET("///etc/passwd"))
    respDoubleSlash.status shouldBe HttpStatus.Forbidden_403
  }

  test("reject relative parent path traversal") { (client: SyncClient) =>
    // Test relative path traversal
    val respRelative = client.sendSafe(Http.GET("..//etc/passwd"))
    respRelative.status shouldBe HttpStatus.Forbidden_403

    // Test relative path traversal with double slashes
    val respRelativeDoubleSlash = client.sendSafe(Http.GET("..//..//etc/passwd"))
    respRelativeDoubleSlash.status shouldBe HttpStatus.Forbidden_403

    // Test relative path traversal with single dot
    val respRelativeSingleDot = client.sendSafe(Http.GET("../etc/passwd"))
    respRelativeSingleDot.status shouldBe HttpStatus.Forbidden_403

    // Test relative path traversal with double dots
    val respRelativeDoubleDot = client.sendSafe(Http.GET("..//..//etc/passwd"))
    respRelativeDoubleDot.status shouldBe HttpStatus.Forbidden_403
  }
}
