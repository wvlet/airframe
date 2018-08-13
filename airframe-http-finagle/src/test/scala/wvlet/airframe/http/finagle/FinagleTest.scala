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

import com.twitter.finagle.http
import wvlet.airframe.AirframeSpec
import wvlet.airframe.http.HttpMethod

/**
  *
  */
class FinagleTest extends AirframeSpec {
  "Finagle pacage" should {
    "provide facade of http requests" in {
      import wvlet.airframe.http.finagle._

      Seq(http.Method.Get, http.Method.Post, http.Method.Delete, http.Method.Put)
        .foreach { m =>
          val req = http.Request(m, "/hello")
          req.setContentString("hello finagle")
          val r = req.asAirframeHttpRequest
          r.method shouldBe toHttpMethod(m)
          r.path shouldBe "/hello"
          r.query shouldBe Map.empty
          r.contentString shouldBe "hello finagle"
        }
    }
  }
}
