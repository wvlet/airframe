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

import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.{HttpMethod, Router, RxEndpoint, RxFilter, RxRPC}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

class RxRouterConverterTest extends AirSpec {

  trait MyApi extends RxRPC {
    def hello: String = "hello"
  }

  object MyApi {
    def router: RxRouter = RxRouter.of[MyApi]
  }

  trait AuthFilter extends RxFilter {
    def apply(request: Request, endpoint: RxEndpoint) = {
      endpoint.apply(request)
    }
  }
  trait LogFilter extends RxFilter {
    def apply(request: Request, endpoint: RxEndpoint) = {
      endpoint.apply(request)
    }
  }

  trait MyApi2 extends RxRPC {
    def hello2: String = "hello2"
  }

  test("Convert RxRouter to Router") {
    val r = Router.fromRxRouter(MyApi.router)
    r.routes.size shouldBe 1
    val r0 = r.routes(0)
    r0.path shouldBe "/hello"
    r0.httpMethod shouldBe HttpMethod.POST
    r0.controllerSurface shouldBe Surface.of[MyApi]
  }

  test("Convert RxRouter with filter to Router") {
    val rxRouter = RxRouter.filter[AuthFilter].andThen(MyApi.router)
    val r        = Router.fromRxRouter(rxRouter)
    r.routes.size shouldBe 1
    r.filterSurface shouldBe defined
    r.filterSurface shouldBe Some(Surface.of[AuthFilter])

    val r0 = r.routes(0)
    r0.path shouldBe "/hello"
    r0.httpMethod shouldBe HttpMethod.POST
    r0.controllerSurface shouldBe Surface.of[MyApi]
  }

  test("with nested filter") {
    val rxRouter = RxRouter
      .filter[AuthFilter]
      .andThen[LogFilter]
      .andThen(MyApi.router)

    val r = Router.fromRxRouter(rxRouter)
    r.filterSurface shouldBe Some(Surface.of[AuthFilter])
    r.children.size shouldBe 1
    r.localRoutes shouldBe empty

    val rc0 = r.children(0)
    rc0.filterSurface shouldBe Some(Surface.of[LogFilter])
    rc0.children.size shouldBe 1

    val rc1 = rc0.children(0)
    rc1.localRoutes.size shouldBe 1
    val l0 = rc1.localRoutes(0)
    l0.path shouldBe "/hello"
    l0.httpMethod shouldBe HttpMethod.POST
    l0.controllerSurface shouldBe Surface.of[MyApi]
  }

  test("multiple APIs") {
    val rxRouter = RxRouter.of(
      RxRouter.of[MyApi],
      RxRouter.of[MyApi2]
    )

    val r = Router.fromRxRouter(rxRouter)
    r.routes.size shouldBe 2
    val r0 = r.routes(0)
    r0.path shouldBe "/hello"
    r0.controllerSurface shouldBe Surface.of[MyApi]
    val r1 = r.routes(1)
    r1.path shouldBe "/hello2"
    r1.controllerSurface shouldBe Surface.of[MyApi2]
  }

  test("multiple APIs with a filter") {
    val rxRouter = RxRouter
      .filter[AuthFilter].andThen(
        RxRouter.of[MyApi],
        RxRouter.of[MyApi2]
      )

    val r = Router.fromRxRouter(rxRouter)
    r.filterSurface shouldBe Some(Surface.of[AuthFilter])
    r.routes.size shouldBe 2
    val r0 = r.routes(0)
    r0.path shouldBe "/hello"
    r0.controllerSurface shouldBe Surface.of[MyApi]
    val r1 = r.routes(1)
    r1.path shouldBe "/hello2"
    r1.controllerSurface shouldBe Surface.of[MyApi2]
  }

}
