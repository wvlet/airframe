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

import wvlet.airframe.http.RxRouter.StemNode
import wvlet.airframe.http.router.RxRoute
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object RxRouterTest extends AirSpec {

  trait MyApi {
    def hello: String = "hello"
  }

  object MyApi {
    def router: RxRouter = RxRouter.of[MyApi]
  }

  trait MyApi2 {
    def hello2: String = "hello2"
    def hello3: String = "hello3"
  }

  object MyApi2 {
    def router: RxRouter = RxRouter.of[MyApi2]
  }

  trait AuthFilter extends RxHttpFilter {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      next(request.withHeader("X-Airframe-Test", "xxx"))
    }
  }

  trait LogFilter extends RxHttpFilter {
    override def apply(request: HttpMessage.Request, next: RxHttpEndpoint): Rx[HttpMessage.Response] = {
      // do some logging
      next.apply(request)
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
    val r = RxRouter.of(
      MyApi.router,
      MyApi2.router
    )

    r.children.size shouldBe 2
    r.filter shouldBe empty

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.children(1) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }

    r.routes(0) shouldMatch { case RxRoute(None, s1, m1) =>
      s1 shouldBe Surface.of[MyApi]
      m1(0).name shouldBe "hello"
    }
    r.routes(1) shouldMatch { case RxRoute(None, s2, m2) =>
      s2 shouldBe Surface.of[MyApi2]
      m2(0).name shouldBe "hello2"
      m2(1).name shouldBe "hello3"
    }
  }

  test("Create a filter") {
    val f = RxRouter.filter[AuthFilter]
    f.filterSurface shouldBe Surface.of[AuthFilter]
  }

  test("Add a filter") {
    val r = RxRouter
      .filter[AuthFilter]
      .andThen(
        MyApi.router,
        MyApi2.router
      )

    r.children.size shouldBe 2
    r.filter shouldBe defined
    r.filter.get.filterSurface shouldBe Surface.of[AuthFilter]

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }

    r.children(1) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
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

    r.children(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
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
    val r = RxRouter.of(
      RxRouter
        .filter[AuthFilter]
        .andThen(MyApi.router),
      RxRouter
        .filter[LogFilter]
        .andThen(MyApi2.router)
    )

    r.children.size shouldBe 2
    r.filter shouldBe empty

    r.children(0) shouldMatch { case StemNode(filter, child) =>
      filter shouldBe defined
      filter.get shouldMatch { case RxRouter.FilterNode(parent, filterSurface) =>
        parent shouldBe empty
        filterSurface shouldBe Surface.of[AuthFilter]
      }
      child(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
        controllerSurface shouldBe Surface.of[MyApi]
        methodSurfaces.size shouldBe 1
        methodSurfaces(0).name shouldBe "hello"
      }
    }

    r.children(1) shouldMatch { case StemNode(filter, child) =>
      filter shouldBe defined
      filter.get shouldMatch { case RxRouter.FilterNode(parent, filterSurface) =>
        parent shouldBe empty
        filterSurface shouldBe Surface.of[LogFilter]
      }
      child(0) shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, None) =>
        controllerSurface shouldBe Surface.of[MyApi2]
        methodSurfaces.size shouldBe 2
        methodSurfaces(0).name shouldBe "hello2"
        methodSurfaces(1).name shouldBe "hello3"
      }
    }

    r.routes(0) shouldMatch { case RxRoute(Some(filter), controllerSurface, methodSurfaces) =>
      filter.filterSurface shouldBe Surface.of[AuthFilter]
      filter.parent shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
      methodSurfaces(0).name shouldBe "hello"
    }
    r.routes(1) shouldMatch { case RxRoute(Some(filter), controllerSurface, methodSurfaces) =>
      filter.filterSurface shouldBe Surface.of[LogFilter]
      filter.parent shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 2
      methodSurfaces(0).name shouldBe "hello2"
      methodSurfaces(1).name shouldBe "hello3"
    }
  }
}
