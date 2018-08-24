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
import wvlet.surface
import wvlet.surface.Surface

/**
  *
  */
class RouterTest extends AirframeSpec {
  "Router" should {

    "reject invalid path" in {
      val e = intercept[IllegalArgumentException] {
        Router.of[InvalidService]
      }
      trace(e.getMessage)
    }

    "register functions as routes" in {
      val r = Router.of[ControllerExample]

      trace(r.routes)
      r.routes.filter(_.path == "/user/:id").size shouldBe 3
      val post = r.routes.find(p => p.path == "/user" && p.method == HttpMethod.POST)
      post shouldBe defined
    }

    "support prefixed paths" in {
      val r = Router.of[PrefixExample]

      trace(r.routes)
      r.routes.head.path shouldBe "/v1/hello"
    }

    "combination of multiple controllers" in {
      val r = Router
        .of[ControllerExample]
        .add[PrefixExample]

      r.routes.find(_.path == "/user/:id") shouldBe defined
      r.routes.find(_.path == "/v1/hello") shouldBe defined
    }

    "find target method" in {
      val router = Router.of[ControllerExample]

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

    "call registered methods" in {
      val router = Router.of[ControllerExample]

      val s = new ControllerExample {}

      val serviceProvider = new ControllerProvider {
        override def findController(serviceSurface: Surface): Option[Any] = {
          serviceSurface match {
            case sf if sf == surface.of[ControllerExample] => Some(s)
            case _                                         => None
          }
        }
      }

      {
        val req = SimpleHttpRequest(HttpMethod.GET, "/user/10")
        val ret =
          router
            .findRoute(req)
            .flatMap(_.call(serviceProvider, req))

        ret.get shouldBe ControllerExample.User("10", "leo")
      }

      {
        val req2 = SimpleHttpRequest(HttpMethod.PUT, "/user/2", contentString = "hello")
        val ret2 =
          router
            .findRoute(req2)
            .flatMap(_.call(serviceProvider, req2))

        ret2.get shouldBe "hello"
      }

      {
        val req = SimpleHttpRequest(HttpMethod.POST, "/user", contentString = """{"name":"aina"}""")
        val ret =
          router
            .findRoute(req)
            .flatMap(_.call(serviceProvider, req))

        ret.get.asInstanceOf[ControllerExample.User].name shouldBe "aina"
      }
    }
  }
}
