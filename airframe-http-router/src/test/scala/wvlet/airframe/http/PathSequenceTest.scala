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
import wvlet.airframe.http.router.ControllerRoute
import wvlet.airspec.AirSpec

/**
  */
object PathSequenceTest extends AirSpec {

  class MyService {
    @Endpoint(path = "/html/*path")
    def static(path: String): String = path
  }

  class MyHTTPService {
    @Endpoint(path = "/")
    def defaultContent: String = "index.html"
    @Endpoint(path = "/*path")
    def content(path: String): String = path
  }

  private val r = Router.of[MyService]

  test("handle empty path") {
    val m = r.findRoute(Http.GET("/html/"))
    m shouldBe defined
    m.get.params("path") shouldBe ""
  }

  test("handle long paths") {
    val m = r.findRoute(Http.GET("/html/long/path"))
    m shouldBe defined
    m.get.params("path") shouldBe "long/path"
  }

  test("handle the root path for path sequence") {
    val r2 = Router.of[MyHTTPService]
    val m  = r2.findRoute(Http.GET("/"))
    m shouldBe defined
    m.get.route match {
      case c: ControllerRoute =>
        c.methodSurface.name shouldBe "defaultContent"
      case _ =>
        fail("failed")
    }

    val m2 = r2.findRoute(Http.GET("/css/main.css"))
    m2 shouldBe defined
    m2.get.route match {
      case c: ControllerRoute =>
        c.methodSurface.name shouldBe "content"
      case _ =>
        fail("failed")
    }
  }
}
