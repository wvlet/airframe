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

import wvlet.airframe.http.{Http, HttpMessage, RxHttpEndpoint, RxRouter}
import wvlet.airframe.rx.Rx
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

class CustomEndpointTest extends AirSpec {

  test("build route with custom endpoint") {
    val endpoint = new RxHttpEndpoint {
      override def apply(request: HttpMessage.Request): Rx[HttpMessage.Response] = {
        Rx.single(Http.response())
      }
    }
    val router = RxRouter.of(endpoint)

    router shouldMatch { case RxRouter.EndpointNode(controllerSurface, methodSurfaces, Some(ep)) =>
      ep shouldBeTheSameInstanceAs endpoint
      controllerSurface shouldBe Surface.of[RedirectToRxEndpoint]
      methodSurfaces(0).name shouldBe "process"
    }
  }
}
