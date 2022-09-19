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
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.router.{HttpRequestMapper, Route}
import wvlet.airspec.AirSpec

import scala.concurrent.Future

/**
  */
object HttpRequestMapperTest extends AirSpec {
  case class NestedRequest(name: String, msg: String)
  case class NestedRequest2(id: Int)

  @RPC
  trait MyApi {
    def rpc1(p1: String): Unit                    = {}
    def rpc2(p1: String, p2: Int): Unit           = {}
    def rpc3(p1: NestedRequest): Unit             = {}
    def rpc4(p1: String, p2: NestedRequest): Unit = {}
    def rpc5(p1: Option[String]): Unit            = {}
    def rpc6(p1: Option[NestedRequest]): Unit     = {}
    def rpc7(
        request: HttpMessage.Request,
        context: HttpContext[Request, Response, Future],
        req: HttpRequest[Request]
    ): Unit = {}
    def rpc8(p1: Int): Unit         = {}
    def rpc9(p1: Option[Int]): Unit = {}
  }

  trait MyApi2 {
    @Endpoint(method = HttpMethod.GET, path = "/v1/endpoint1")
    def endpoint1(p1: NestedRequest): Unit = {}

    @Endpoint(method = HttpMethod.GET, path = "/v1/endpoint2")
    def endpoint2(p1: NestedRequest2): Unit = {}

    @Endpoint(method = HttpMethod.GET, path = "/v1/endpoint3")
    def endpoint3(p1: Seq[String]): Unit = {}

    @Endpoint(method = HttpMethod.GET, path = "/v1/endpoint4")
    def endpoint4(p1: Option[Seq[String]]): Unit = {}
  }

  private val api    = new MyApi {}
  private val router = Router.add[MyApi].add[MyApi2]

  private val mockContext = HttpContext.mockContext
  private def mapArgs(
      route: Route,
      requestFilter: HttpMessage.Request => HttpMessage.Request,
      method: String = HttpMethod.POST
  ): Seq[Any] = {
    val args = HttpRequestMapper.buildControllerMethodArgs[HttpMessage.Request, HttpMessage.Response, Future](
      controller = api,
      methodSurface = route.methodSurface,
      request = requestFilter(Http.request(method, route.path)),
      context = mockContext,
      params = Map.empty,
      codecFactory = MessageCodecFactory.defaultFactoryForJSON,
      isRPC = route.isRPC
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

  test("detect wrong parameter mapping") {
    val r = findRoute("rpc1")
    intercept[IllegalArgumentException] {
      val args = mapArgs(r, _.withJson("""{"p0":"hello"}"""))
      warn(args)
    }
  }

  test("forbid mapping a single primitive argument as a body") {
    val r = findRoute("rpc1")
    intercept[IllegalArgumentException] {
      // Note: This should work for Endpoint calls
      val args = mapArgs(r, _.withContent("""hello"""))
      warn(args)
    }
  }

  test("map multiple primitive arguments") {
    val r    = findRoute("rpc2")
    val args = mapArgs(r, _.withJson("""{"p1":"hello","p2":2020}"""))
    args shouldBe Seq("hello", 2020)
  }

  test("throw an exception when reading incompatible primitive arguments") {
    val r = findRoute("rpc2")
    intercept[IllegalArgumentException] {
      mapArgs(r, _.withJson("""{"p1":"hello","p2":"abc"}"""))
    }
  }

  test("map a single request object") {
    val r    = findRoute("rpc3")
    val args = mapArgs(r, _.withJson("""{"p1":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("map a single request object inside nested JSON") {
    val r    = findRoute("rpc3")
    val args = mapArgs(r, _.withJson("""{"p1":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("construct request object using both query parameters and body") {
    skip("not supported for now")
    val r    = findRoute("rpc3")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?name=hello").withJson("""{"msg":"world"}""") })
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("map a primitive value and a single request object") {
    val r    = findRoute("rpc4")
    val args = mapArgs(r, _.withJson("""{"p1":"Yes","p2":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq("Yes", NestedRequest("hello", "world"))
  }

  test("extract a primitive value parameter from a query string") {
    val r    = findRoute("rpc4")
    val args = mapArgs(r, { r => r.withJson("""{"p2":{"name":"hello","msg":"world"}}""").withUri(s"${r.uri}?p1=Yes") })
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
    val args = mapArgs(r, _.withJson("""{"p1":{"name":"hello","msg":"world"}}"""))
    args shouldBe Seq(Some(NestedRequest("hello", "world")))
  }

  test("map a single request object (Empty) to Option[X]") {
    val r    = findRoute("rpc6")
    val args = mapArgs(r, identity)
    args shouldBe Seq(None)
  }

  test("map http request contexts") {
    val r    = findRoute("rpc7")
    val args = mapArgs(r, identity)
    args.length shouldBe 3
    args(0).getClass shouldBe classOf[Request]
    args(1) shouldBeTheSameInstanceAs mockContext
    classOf[HttpRequest[Request]].isAssignableFrom(args(2).getClass) shouldBe true
  }

  test("throw an error on incompatible type") {
    val r = findRoute("rpc8")
    intercept[IllegalArgumentException] {
      mapArgs(r, _.withJson("""{"p1":"abc"}"""))
    }
  }

  test("throw an error on incompatible type in query parameters") {
    val r = findRoute("rpc8")
    intercept[IllegalArgumentException] {
      mapArgs(r, { r => r.withUri(s"${r.uri}?p1=abc") })
    }
  }

  test("throw an error on incompatible type in request body") {
    val r = findRoute("rpc8")
    intercept[IllegalArgumentException] {
      mapArgs(r, { r => r.withContent("abc") })
    }
  }

  test("throw an error when mapping JSON [1] to Int") {
    val r = findRoute("rpc8")
    intercept[IllegalArgumentException] {
      val args = mapArgs(r, { r => r.withJson("""{"p1":[1]}""") })
      warn(args)
    }
  }

  test("throw an error on incompatible JSON [1] to Option[X]") {
    val r = findRoute("rpc9")
    intercept[IllegalArgumentException] {
      val args = mapArgs(r, { r => r.withJson("""{"p1":[1]}""") })
      warn(args)
    }
  }

  test("construct objects using query parameters for GET") {
    val r    = findRoute("endpoint1")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?name=hello&msg=world") }, method = HttpMethod.GET)
    args shouldBe Seq(NestedRequest("hello", "world"))
  }

  test("construct nested objects using query parameters for GET") {
    val r    = findRoute("endpoint2")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?id=1") }, method = HttpMethod.GET)
    args shouldBe Seq(NestedRequest2(1))
  }

  test("throw an error when incompatible input is found when constructing nested objects with GET") {
    val r = findRoute("endpoint2")
    intercept[IllegalArgumentException] {
      mapArgs(r, { r => r.withUri(s"${r.uri}?id=abc") }, method = HttpMethod.GET)
    }
  }

  test("map query parameters to Seq[X]") {
    val r    = findRoute("endpoint3")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?p1=apple") }, method = HttpMethod.GET)
    args shouldBe Seq(Seq("apple"))
  }

  test("map multiple query parameters to Seq[X]") {
    val r    = findRoute("endpoint3")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?p1=apple&p1=banana") }, method = HttpMethod.GET)
    args shouldBe Seq(Seq("apple", "banana"))
  }

  test("map empty parameters to Seq[X]") {
    val r    = findRoute("endpoint3")
    val args = mapArgs(r, identity, method = HttpMethod.GET)
    args shouldBe Seq(Seq.empty)
  }

  test("map empty parameters to Option[Seq[X]]") {
    val r    = findRoute("endpoint4")
    val args = mapArgs(r, identity, method = HttpMethod.GET)
    args shouldBe Seq(None)
  }

  test("map multiple query parameters to Option[Seq[X]]") {
    val r    = findRoute("endpoint4")
    val args = mapArgs(r, { r => r.withUri(s"${r.uri}?p1=apple&p1=banana") }, method = HttpMethod.GET)
    args shouldBe Seq(Some(Seq("apple", "banana")))
  }
}
