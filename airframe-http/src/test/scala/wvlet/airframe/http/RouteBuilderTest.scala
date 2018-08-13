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

import wvlet.airframe.AirframeSpec

/**
  *
  */
class RouteBuilderTest extends AirframeSpec {

  "RouteBuilder" should {
    "reject invalid path" in {
      val e = intercept[IllegalArgumentException] {
        RouteBuilder()
          .add[InvalidService]
      }
      trace(e.getMessage)
    }

    "register functions as routes" in {
      val r = RouteBuilder()
        .add[ServiceExample]

      trace(r.routes)
      r.routes.filter(_.path == "/user/:id").size shouldBe 3
      val post = r.routes.find(p => p.path == "/user" && p.method == HttpMethod.POST)
      post shouldBe defined
    }

    "support prefixed paths" in {
      val r = RouteBuilder()
        .add[PrefixExample]

      trace(r.routes)
      r.routes.head.path shouldBe "/v1/hello"
    }
  }
}
