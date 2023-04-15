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
package wvlet.airframe.http.router

import wvlet.airframe.http.{HttpMessage, RPC, RxEndpoint, RxFilter, RxRPC}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object RxRouterTest extends AirSpec {

  trait MyApi extends RxRPC {
    def hello: String = "hello"
  }

  object MyApi {
    def router: RxRouter = RxRouter.of[MyApi]
  }

  trait MyApi2 extends RxRPC {
    def hello2: String = "hello2"
    def hello3: String = "hello3"
  }

  object MyApi2 {
    def router: RxRouter = RxRouter.of[MyApi2]
  }

  trait AuthFilter extends RxFilter {
    override def apply(request: HttpMessage.Request, endpoint: RxEndpoint): Rx[HttpMessage.Response] = {
      endpoint(request.withHeader("X-Airframe-Test", "xxx"))
    }
  }

  trait LogFilter extends RxFilter {
    override def apply(request: HttpMessage.Request, endpoint: RxEndpoint): Rx[HttpMessage.Response] = {
      // do some logging
      endpoint.apply(request)
    }
  }

  test("create a single route RxRouter") {
    val r = RxRouter.of[MyApi]
    r.children shouldBe empty
    r.routes.size shouldBe 1
    r.filter shouldBe empty

    r.routes(0) shouldMatch { case RxRoute(None, controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }
  }

  test("combine multiple routers") {
    val r = RxRouter
      .add(MyApi.router)
      .add(MyApi2.router)

    r.children.size shouldBe 2
    r.filter shouldBe empty

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.children(1) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }

    val s1 = Surface.of[MyApi]
    val s2 = Surface.of[MyApi2]
    val m1 = Surface.methodsOf[MyApi]
    val m2 = Surface.methodsOf[MyApi2]
    r.routes(0) shouldMatch { case RxRoute(None, `s1`, `m1`) => }
    r.routes(1) shouldMatch { case RxRoute(None, `s2`, `m2`) => }
  }

  test("Add a filter") {
    val r = RxRouter
      .filter[AuthFilter]
      .andThen(
        RxRouter
          .add(MyApi.router)
          .add(MyApi2.router)
      )

    r.children.size shouldBe 2
    r.filter shouldBe defined
    r.filter.get.filterSurface shouldBe Surface.of[AuthFilter]

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.children(1) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }

    r.routes.size shouldBe 2
    r.routes(0) shouldMatch { case RxRoute(Some(filter), controllerSurface, methodSurfaces) =>
      filter.filterSurface shouldBe Surface.of[AuthFilter]
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.routes(1) shouldMatch { case RxRoute(Some(filter), controllerSurface, methodSurfaces) =>
      filter.filterSurface shouldBe Surface.of[AuthFilter]
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }
  }

  test("Add multiple filters") {
    val r = RxRouter
      .filter[AuthFilter]
      .andThen[LogFilter]
      .andThen(MyApi.router)

    r.children.size shouldBe 1
    r.filter shouldBe defined
    r.filter.get.filterSurface shouldBe Surface.of[LogFilter]
    r.filter.get.parent shouldBe defined
    r.filter.get.parent.get.filterSurface shouldBe Surface.of[AuthFilter]

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.routes.size shouldBe 1
    r.routes(0) shouldMatch { case RxRoute(Some(filter), controllerSurface, methodSurfaces) =>
      filter.filterSurface shouldBe Surface.of[LogFilter]
      filter.parent shouldBe defined
      filter.parent.get.filterSurface shouldBe Surface.of[AuthFilter]
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }
  }

  test("Use different filters to different routes") {
    val r = RxRouter
      .add(
        RxRouter
          .filter[AuthFilter]
          .andThen(MyApi.router)
      )
      .add(
        RxRouter
          .filter[LogFilter]
          .andThen(MyApi2.router)
      )

    r.children.size shouldBe 2
    r.filter shouldBe empty

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      // filter.get.filterSurface shouldBe Surface.of[AuthFilter]
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.children(1) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces) =>
      // filter shouldBe defined
      // filter.get.filterSurface shouldBe Surface.of[LogFilter]
      // filter.get.parent shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }
  }
}
