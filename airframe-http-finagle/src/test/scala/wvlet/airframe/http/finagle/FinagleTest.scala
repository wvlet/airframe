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

import java.nio.charset.StandardCharsets

import com.twitter.finagle.http
import com.twitter.finagle.http.Status
import wvlet.airframe.http.HttpStatus
import wvlet.airspec.AirSpec

/**
  *
  */
class FinagleTest extends AirSpec {
  import wvlet.airframe.http.finagle._

  def `provide facade of http requests`: Unit = {
    Seq(http.Method.Get,
        http.Method.Post,
        http.Method.Delete,
        http.Method.Put,
        http.Method.Patch,
        http.Method.Head,
        http.Method.Options,
        http.Method.Trace)
      .foreach { m =>
        val req = http.Request(m, "/hello")
        req.setContentString("hello finagle")
        req.setContentTypeJson()
        val r = req.toHttpRequest
        r.method shouldBe toHttpMethod(m)
        r.path shouldBe "/hello"
        r.query shouldBe Map.empty
        r.contentString shouldBe "hello finagle"
        r.contentBytes shouldBe "hello finagle".getBytes(StandardCharsets.UTF_8)
        r.contentType shouldBe Some("application/json;charset=utf-8")
        r.toRaw shouldBe req
      }
  }

  def `provide facade of http responses`: Unit = {
    val orig = http.Response(Status.Forbidden)
    orig.setContentString("hello world")
    orig.setContentTypeJson()

    val r = orig.toHttpResponse

    r.status shouldBe HttpStatus.Forbidden_403
    r.statusCode shouldBe 403
    r.contentString shouldBe "hello world"
    r.contentType shouldBe Some("application/json;charset=utf-8")
    r.contentBytes shouldBe "hello world".getBytes(StandardCharsets.UTF_8)
    r.toRaw shouldBeTheSameInstanceAs orig
  }
}
