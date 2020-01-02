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
import com.twitter.finagle.http.Response
import wvlet.airframe.Design
import wvlet.airframe.control.Control
import wvlet.airframe.http._
import wvlet.airspec.AirSpec
import wvlet.log.io.{IOUtil, Resource}

object StaticContentTest {

  trait StaticContentServer {
    @Endpoint(path = "/html/*path")
    def staticContent(path: String) =
      StaticContent.fromResource(basePath = s"/wvlet/airframe/http/finagle/static", path)

    @Endpoint(path = "/html2/*path")
    def staticContent2(path: String) = {
      StaticContent
        .fromResource("/wvlet/airframe/http/finagle/static")
        .fromResource("/wvlet/airframe/http/finagle/static2")
        .apply(path)
    }

    @Endpoint(path = "/html3/*path")
    def fileContent(path: String) = {
      StaticContent.fromDirectory("./airframe-http-finagle/src/test/resources/wvlet/airframe/http/finagle/static", path)
    }
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
    val res  = client.get[Response]("/html/index.html")
    val html = res.contentString
    debug(html)
    html.contains("Hello Airframe HTTP!") shouldBe true
    res.contentType shouldBe Some("text/html")
  }

  def `forbid accessing parent resources`(client: FinagleSyncClient): Unit = {
    val ex = intercept[HttpClientException] {
      client.get[String]("/html/../hidden/secret.txt")
    }
    ex.status shouldBe HttpStatus.Forbidden_403

    val ex2 = intercept[HttpClientException] {
      client.get[String]("/html/dummy/../../hidden/secret.txt")
    }
    ex2.status shouldBe HttpStatus.Forbidden_403
  }

  def `support safe relative paths`(client: FinagleSyncClient): Unit = {
    // OK
    val html = client.get[String]("/html/asset/../index.html")
    html.contains("Hello Airframe HTTP!") shouldBe true
  }

  def `set content-type`(client: FinagleSyncClient): Unit = {
    def check(path: String, expectedContentType: String): Unit = {
      val r = client.get[Response](path)
      r.contentType shouldBe Some(expectedContentType)
    }

    check("/html/index.html", "text/html")
    check("/html/asset/style.css", "text/css")
    check("/html/data/sample.json", "application/json")
    check("/html/asset/test.js", "application/javascript")
    check("/html/asset/airframe_icon_small.png", "image/png")

    // TODO add more coverage
  }

  def `read binary file`(client: FinagleSyncClient): Unit = {
    val resp   = client.get[Response]("/html/asset/airframe_icon_small.png")
    val img    = resp.contentBytes
    val imgUrl = Resource.find("/wvlet/airframe/http/finagle/static/asset/airframe_icon_small.png").get
    Control.withResource(imgUrl.openStream()) { in =>
      IOUtil.readFully(in) { bytes =>
        img shouldBe bytes
      }
    }
  }

  def `read from an alternative static content path`(client: FinagleSyncClient): Unit = {
    val html = client.get[String]("/html2/index2.html")
    html.contains("static2") shouldBe true
  }

  def `read from a directory`(client: FinagleSyncClient): Unit = {
    val html = client.get[String]("/html3/index.html")
    html.contains("Hello Airframe HTTP!") shouldBe true
  }
}
