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
package wvlet.airframe.http.finagle
import wvlet.airframe.Design
import wvlet.airframe.http.{Endpoint, HttpClientException, HttpStatus, Router, StaticContent}
import wvlet.airspec.AirSpec


object StaticContentTest {

  trait StaticContentServer {
    @Endpoint(path = "/html/*path")
    def staticContent(path:String) = StaticContent.fromResource(basePath=s"/wvlet/airframe/http/finagle/static", path)
  }
}

/**
 *
 */
class StaticContentTest extends AirSpec {

  override protected def design: Design = {
    val r = Router.add[StaticContentTest.StaticContentServer]
    newFinagleServerDesign(name = "static-content-test", router = r)
      .add(finagleSyncClientDesign)
  }

  def `serve static contents from resources in classpath`(client: FinagleSyncClient): Unit = {
    val html = client.get[String]("/html/index.html")
    debug(html)
    html.contains("Hello Airframe HTTP!") shouldBe true
  }

  def `forbid accessing parent resources`(client: FinagleSyncClient): Unit = {
    val ex = intercept[HttpClientException] {
      client.get[String]("/html/../hidden/secret.txt")
    }
    ex.status shouldBe HttpStatus.Forbidden_403
  }

}
