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
import wvlet.airframe.http.example._
import wvlet.airframe.http.example.ControllerExample.User
import wvlet.airframe.surface.Surface

/**
  *
  */
class RouterTest extends AirframeSpec {
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
      .add[ControllerExample]
      .add[PrefixExample]

    r.routes.find(_.path == "/user/:id") shouldBe defined
    r.routes.find(_.path == "/v1/hello") shouldBe defined
  }

  "find target method" in {
    val router = Router.of[ControllerExample]

    val r = router.findRoute(SimpleHttpRequest(HttpMethod.GET, "/user/1"))
    debug(r)
    r shouldBe defined
    r.get.route.method shouldBe HttpMethod.GET

    val r2 = router.findRoute(SimpleHttpRequest(HttpMethod.POST, "/user"))
    debug(r2)
    r2 shouldBe defined
    r2.get.route.method shouldBe HttpMethod.POST

    val r3 = router.findRoute(SimpleHttpRequest(HttpMethod.PUT, "/user/2"))
    debug(r3)
    r3 shouldBe defined
    r3.get.route.method shouldBe HttpMethod.PUT

    val r4 = router.findRoute(SimpleHttpRequest(HttpMethod.DELETE, "/user/3"))
    debug(r4)
    r4 shouldBe defined
    r4.get.route.method shouldBe HttpMethod.DELETE
  }

  "call registered methods" in {
    val router = Router.of[ControllerExample]

    val s = new ControllerExample {}

    val serviceProvider = new ControllerProvider {
      override def findController(serviceSurface: Surface): Option[Any] = {
        serviceSurface match {
          case sf if sf == Surface.of[ControllerExample] => Some(s)
          case _                                         => None
        }
      }
    }

    def call[A](request: SimpleHttpRequest, exepected: A): Unit = {
      val ret =
        router
          .findRoute(request)
          .flatMap(_.call(serviceProvider, request))

      ret shouldBe defined
      ret.get shouldBe exepected
    }

    call(SimpleHttpRequest(HttpMethod.GET, "/user/10"), ControllerExample.User("10", "leo"))
    call(SimpleHttpRequest(HttpMethod.PUT, "/user/2", contentString = "hello"), "hello")
    call(SimpleHttpRequest(HttpMethod.POST, "/user", contentString = """{"name":"aina", "id":"xxxx"}"""),
         User("xxxx", "aina"))
    call(SimpleHttpRequest(HttpMethod.GET, "/scala/users"), ControllerExample.Group("scala", Seq(User("10", "leo"))))

    call(SimpleHttpRequest(HttpMethod.GET, "/scala/user/11"), ControllerExample.Group("scala", Seq(User("11", "leo"))))
    call(SimpleHttpRequest(HttpMethod.GET, "/conflict/users"), ControllerExample.Group("xxx", Seq(User("10", "leo"))))

    call(SimpleHttpRequest(HttpMethod.GET, "/v1/config/entry/long/path"), "long/path")
    call(SimpleHttpRequest(HttpMethod.GET, "/v1/config/info"), "hello")
  }

  "find ambiguous path patterns" in {
    val r = Router.add[AmbiguousPathExample]
    warn("Ambiguous HTTP path pattern test")
    val ex = intercept[Throwable] {
      r.findRoute(SimpleHttpRequest(HttpMethod.GET, "/v1"))
    }
    warn(ex.getMessage)
  }

  "find methods with the same prefix" in {
    val r  = Router.add[SharedPathPrefix]
    val m1 = r.findRoute(SimpleHttpRequest(HttpMethod.GET, "/v1/config"))
    m1 shouldBe defined
    m1.get.route.path shouldBe "/v1/config"

    val m2 = r.findRoute(SimpleHttpRequest(HttpMethod.GET, "/v1/config/app"))
    m2 shouldBe defined
    m2.get.route.path shouldBe "/v1/config/app"
  }

  "build DFA" in {
    // Test DFA builder
    val r   = Router.add[ControllerExample]
    val dfa = RouteMatcher.buildPathDFA(r.routes)
    info(dfa.toString)
  }
}
