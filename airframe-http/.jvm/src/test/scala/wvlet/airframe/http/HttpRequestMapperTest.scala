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
    def f1(p1: String): String                    = p1
    def f2(p1: String, p2: Int): String           = s"${p1},${p2}"
    def f3(p1: NestedRequest)                     = s"${p1}"
    def f4(p1: String, p2: NestedRequest): String = s"${p1},${p2}"
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

  test("map a single primitive argument using JSON") {
    val r    = router.routes.find(_.methodSurface.name == "f1").get
    val args = mapArgs(r, _.withJson("""{"p1":"hello"}"""))
    args shouldBe Seq("hello")
  }

  test("map a single primitive argument with a string content") {
    val r    = router.routes.find(_.methodSurface.name == "f1").get
    val args = mapArgs(r, _.withContent("""hello"""))
    args shouldBe Seq("hello")
  }

  test("map multiple primitive arguments") {
    val r    = router.routes.find(_.methodSurface.name == "f2").get
    val args = mapArgs(r, _.withJson("""{"p1":"hello","p2":2020}"""))
    args shouldBe Seq("hello", 2020)
  }

  test("map a single request object") {
    val r    = router.routes.find(_.methodSurface.name == "f3").get
    val args = mapArgs(r, _.withJson("""{"name":"hello","msg":"world"}"""))
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("map a primitive value and a single request object") {
    val r    = router.routes.find(_.methodSurface.name == "f4").get
    val args = mapArgs(r, _.withJson("""{"p1":"Yes","p2":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq("Yes", NestedRequest("hello", "world"))
  }

}
