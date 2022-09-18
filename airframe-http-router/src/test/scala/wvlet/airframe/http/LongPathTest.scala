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

import wvlet.airframe.http.example.LongPathExample
import wvlet.airspec.AirSpec

/**
  */
class LongPathTest extends AirSpec {
  // ...
  test("match long paths") {
    val r = Router.add[LongPathExample]

    {
      val m = r.findRoute(Http.GET("/v1/config/entry"))
      m shouldBe defined
    }

    {
      val m = r.findRoute(Http.GET("/v1/config/entry/myapp"))
      m shouldBe defined
      m.get.params("scope") shouldBe "myapp"
    }

    {
      val m = r.findRoute(Http.GET("/v1/config/entry/myapp/long/path/entry"))
      m shouldBe defined
      val p = m.get.params
      p("scope") shouldBe "myapp"
      p("key") shouldBe "long/path/entry"
    }

    {
      val m =
        r.findRoute(
          Http.GET("/v1/config/entry/config/autoscaling/clusters/default/maxCapacity")
        )

      m shouldBe defined
      val p = m.get.params
      p("scope") shouldBe "config"
      p("key") shouldBe "autoscaling/clusters/default/maxCapacity"
    }
  }
}
