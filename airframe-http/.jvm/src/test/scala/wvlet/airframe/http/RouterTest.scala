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

import wvlet.airframe.Session
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.example.ControllerExample.User
import wvlet.airframe.http.example._
import wvlet.airframe.http.router.{ControllerProvider, RouteMatcher}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  */
class RouterTest extends AirSpec {
  def `reject invalid path`: Unit = {
    val e = intercept[IllegalArgumentException] {
      Router.of[InvalidService]
    }
    trace(e.getMessage)
  }

  def `register functions as routes`: Unit = {
    val r = Router.of[ControllerExample]

    trace(r.routes)
    r.routes.filter(_.path == "/user/:id").size shouldBe 3
    r.routes.forall(_.isRPC == false) shouldBe true
    val post = r.routes.find(p => p.path == "/user" && p.method == HttpMethod.POST)
    post shouldBe defined
  }

  def `support prefixed paths`: Unit = {
    val r = Router.of[PrefixExample]

    trace(r.routes)
    r.routes.head.path shouldBe "/v1/hello"
  }

  def `combination of multiple controllers`: Unit = {
    val r = Router
      .add[ControllerExample]
      .add[PrefixExample]

    r.routes.find(_.path == "/user/:id") shouldBe defined
    r.routes.find(_.path == "/v1/hello") shouldBe defined
  }

  trait RouteA
  trait RouteB
  trait RouteC

  def `stack Routes`: Unit = {
    val r = Router
      .add[RouteA]
      .add[RouteB]
      .add[RouteC]

    r.children.size shouldBe 3
  }

  trait FilterA extends HttpFilterType
  trait RouteD

  def `filter Routers`: Unit = {
    val r = Router
      .add[FilterA]
      .andThen(
        Router
          .add[RouteA]
          .add[RouteB]
          .add[RouteC]
      )
      .add[RouteD]

    r.children.size shouldBe 2
    r.children(0).filterSurface shouldBe defined
    r.children(0).children(0).children.size shouldBe 3
    r.children(1).surface shouldBe defined
  }

  def `find target method`: Unit = {
    val router = Router.of[ControllerExample]

    val r = router.findRoute(Http.GET("/user/1"))
    debug(r)
    r shouldBe defined
    r.get.route.method shouldBe HttpMethod.GET

    val r2 = router.findRoute(Http.POST("/user"))
    debug(r2)
    r2 shouldBe defined
    r2.get.route.method shouldBe HttpMethod.POST

    val r3 = router.findRoute(Http.PUT("/user/2"))
    debug(r3)
    r3 shouldBe defined
    r3.get.route.method shouldBe HttpMethod.PUT

    val r4 = router.findRoute(Http.DELETE("/user/3"))
    debug(r4)
    r4 shouldBe defined
    r4.get.route.method shouldBe HttpMethod.DELETE

    val r5 = router.findRoute(Http.GET("/v1/config/info"))
    debug(r5)
    r5 shouldBe defined
    r5.get.route.method shouldBe HttpMethod.GET

    val r6 = router.findRoute(Http.GET("/v1/config/xxxx/info"))
    debug(r6)
    r6 shouldNotBe defined
  }

  def `call registered methods`(session: Session): Unit = {
    val router = Router.of[ControllerExample]

    val s = new ControllerExample {}

    val serviceProvider = new ControllerProvider {
      override def findController(session: Session, serviceSurface: Surface): Option[Any] = {
        serviceSurface match {
          case sf if sf == Surface.of[ControllerExample] => Some(s)
          case _                                         => None
        }
      }
    }

    def call[A](request: HttpMessage.Request, expected: A): Unit = {
      val ret =
        router
          .findRoute(request)
          .flatMap { m =>
            m.call(session, serviceProvider, request, HttpContext.mockContext, MessageCodecFactory.defaultFactory)
          }

      ret shouldBe defined
      ret.get shouldBe expected
    }

    call(Http.GET("/user/10"), ControllerExample.User("10", "leo"))
    call(Http.PUT("/user/2").withContent("hello"), "hello")
    call(
      Http.POST("/user").withContent("""{"name":"aina", "id":"xxxx"}"""),
      User("xxxx", "aina")
    )
    call(Http.GET("/scala/users"), ControllerExample.Group("scala", Seq(User("10", "leo"))))

    call(Http.GET("/scala/user/11"), ControllerExample.Group("scala", Seq(User("11", "leo"))))
    call(Http.GET("/conflict/users"), ControllerExample.Group("xxx", Seq(User("10", "leo"))))

    call(Http.GET("/v1/config/entry/long/path"), "long/path")
    call(Http.GET("/v1/config/info"), "hello")
  }

  def `find ambiguous path patterns`: Unit = {
    val r = Router.add[AmbiguousPathExample]
    warn("Ambiguous HTTP path pattern test")
    val ex = intercept[Throwable] {
      r.findRoute(Http.GET("/v1"))
    }
    warn(ex.getMessage)
  }

  def `find methods with the same prefix`: Unit = {
    val r  = Router.add[SharedPathPrefix]
    val m1 = r.findRoute(Http.GET("/v1/config"))
    m1 shouldBe defined
    m1.get.route.path shouldBe "/v1/config"

    val m2 = r.findRoute(Http.GET("/v1/config/app"))
    m2 shouldBe defined
    m2.get.route.path shouldBe "/v1/config/app"
  }

  def `build DFA`: Unit = {
    // Test DFA builder
    val r   = Router.add[ControllerExample]
    val dfa = RouteMatcher.buildPathDFA(r.routes)
    debug(dfa.toString)
  }
}
