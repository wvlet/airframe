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
  }

  object MyApi2 {
    def router: RxRouter = RxRouter.of[MyApi2]
  }

  trait AuthFilter extends RxFilter {
    override def apply(request: HttpMessage.Request, endpoint: RxEndpoint): Rx[HttpMessage.Response] = {
      endpoint(request.withHeader("X-Airframe-Test", "xxx"))
    }
  }

  test("create a single route RxRouter") {
    val r = RxRouter.of[MyApi]
    r.routes.size shouldBe 1
    r.filter shouldBe empty

    r.routes(0) shouldMatch { case RxRouter.EndpointNode(filter, controllerSurface, methodSurfaces) =>
      filter shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
    }
  }

  test("creat a new RxRouter") {
    val r = RxRouter
      .add(MyApi.router)
      .add(MyApi2.router)

    r.routes.size shouldBe 2
    r.filter shouldBe empty

    r.routes(0) shouldMatch { case RxRouter.EndpointNode(filter, controllerSurface, methodSurfaces) =>
      filter shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi]
      methodSurfaces.size shouldBe 1
    }

    r.routes(1) shouldMatch { case RxRouter.EndpointNode(filter, controllerSurface, methodSurfaces) =>
      filter shouldBe empty
      controllerSurface shouldBe Surface.of[MyApi2]
      methodSurfaces.size shouldBe 1
    }
  }

  test("Add filter") {
    val r = RxRouter
      .filter[AuthFilter]
      .andThen(
        RxRouter
          .add(MyApi.router)
          .add(MyApi2.router)
      )

    r.routes.size shouldBe 2
    r.filter shouldBe defined
    r.filter.get.filterSurface shouldBe Surface.of[AuthFilter]
  }
}
