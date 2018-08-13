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
class RouterTest extends AirframeSpec {
  "Router" should {

    "find target method" in {
      val router = RouteBuilder()
        .add[ServiceExample]
        .build

      val r = router.findRoute(SimpleHttpRequest(HttpMethod.GET, "/user/1"))
      debug(r)
      r shouldBe defined
      r.get.method shouldBe HttpMethod.GET

      val r2 = router.findRoute(SimpleHttpRequest(HttpMethod.POST, "/user"))
      debug(r2)
      r2 shouldBe defined
      r2.get.method shouldBe HttpMethod.POST

      val r3 = router.findRoute(SimpleHttpRequest(HttpMethod.PUT, "/user/2"))
      debug(r3)
      r3 shouldBe defined
      r3.get.method shouldBe HttpMethod.PUT

      val r4 = router.findRoute(SimpleHttpRequest(HttpMethod.DELETE, "/user/3"))
      debug(r4)
      r4 shouldBe defined
      r4.get.method shouldBe HttpMethod.DELETE
    }

  }
}
