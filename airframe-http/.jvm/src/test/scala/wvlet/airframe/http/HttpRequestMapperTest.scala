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
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.router.{HttpRequestMapper, Route}
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.airspec.AirSpec

import scala.concurrent.Future

/**
  *
 */
object HttpRequestMapperTest extends AirSpec {

  case class NestedRequest(name: String, msg: String)

  @RPC
  trait MyApi {
    def rpc1(p1: String): String                    = p1
    def rpc2(p1: String, p2: Int): String           = s"${p1},${p2}"
    def rpc3(p1: NestedRequest)                     = s"${p1}"
    def rpc4(p1: String, p2: NestedRequest): String = s"${p1},${p2}"
    def rpc5(p1: Option[String]): String            = s"${p1}"
    def rpc6(p1: Option[NestedRequest]): String     = s"${p1}"
  }

  private val api    = new MyApi {}
  private val router = Router.of[MyApi]

  private def mapArgs(route: Route, requestFilter: HttpMessage.Request => HttpMessage.Request): Seq[Any] = {
    val args = HttpRequestMapper.buildControllerMethodArgs[HttpMessage.Request, HttpMessage.Response, Future](
      controller = api,
      methodSurface = route.methodSurface.asInstanceOf[ReflectMethodSurface],
      request = requestFilter(Http.POST(route.path)),
      context = HttpContext.mockContext,
      params = Map.empty,
      codecFactory = MessageCodecFactory.defaultFactoryForJSON
    )
    args
  }

  private def findRoute(name: String): Route = {
    router.routes.find(_.methodSurface.name == name).get
  }

  test("map a single primitive argument using JSON") {
    val r    = findRoute("rpc1")
    val args = mapArgs(r, _.withJson("""{"p1":"hello"}"""))
    args shouldBe Seq("hello")
  }

  test("map a single primitive argument with a string content") {
    val r    = findRoute("rpc1")
    val args = mapArgs(r, _.withContent("""hello"""))
    args shouldBe Seq("hello")
  }

  test("map multiple primitive arguments") {
    val r    = findRoute("rpc2")
    val args = mapArgs(r, _.withJson("""{"p1":"hello","p2":2020}"""))
    args shouldBe Seq("hello", 2020)
  }

  test("map a single request object") {
    val r    = findRoute("rpc3")
    val args = mapArgs(r, _.withJson("""{"name":"hello","msg":"world"}"""))
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("map a single request object inside nested JSON") {
    val r    = findRoute("rpc3")
    val args = mapArgs(r, _.withJson("""{"p1":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("map a primitive value and a single request object") {
    val r    = findRoute("rpc4")
    val args = mapArgs(r, _.withJson("""{"p1":"Yes","p2":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq("Yes", NestedRequest("hello", "world"))
  }

  test("map an option of a primitive value") {
    val r    = findRoute("rpc5")
    val args = mapArgs(r, _.withJson("""{"p1":"hello"}"""))
    args shouldBe Seq(Some("hello"))
  }

  test("map an option (None) of a primitive value") {
    val r    = findRoute("rpc5")
    val args = mapArgs(r, _.withJson("""{}"""))
    args shouldBe Seq(None)
  }

  test("map an option (None) of a primitive value with empty content") {
    val r    = findRoute("rpc5")
    val args = mapArgs(r, identity)
    args shouldBe Seq(None)
  }

  test("map a single request object to Option[X]") {
    val r    = findRoute("rpc6")
    val args = mapArgs(r, _.withJson("""{"name":"hello","msg":"world"}"""))
    args shouldBe Seq(Some(NestedRequest("hello", "world")))
  }

  test("map a single request object (Empty) to Option[X]") {
    val r    = findRoute("rpc6")
    val args = mapArgs(r, identity)
    args shouldBe Seq(None)
  }

}
