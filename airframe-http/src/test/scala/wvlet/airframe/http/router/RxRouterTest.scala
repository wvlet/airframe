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

import wvlet.airframe.http.router.RxRouter
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

object RxRouterTest extends AirSpec {

  @RPC
  trait MyApi {
    def hello: String = "hello"
  }

  object MyApi {
    def router: RxRouter = RxRouter.of[MyApi]
  }

  @RPC
  trait MyApi2 {
    def hello2: String = "hello2"
  }

  object MyApi2 {
    def router: RxRouter = RxRouter.of[MyApi2]
  }

  trait AuthFilter extends RxFilter {
    override def apply(request: HttpMessage.Request, nextService: RxService): Rx[HttpMessage.Response] = {
      nextService(request.withHeader("X-Airframe-Test", "xxx"))
    }
  }

  test("creat a new RxRouter") {
    val r = RxRouter
      .add(MyApi.router)
      .add(MyApi2.router)
    info(r)
  }

  test("Add filter") {
    RxRouter
      .filter[AuthFilter]
      .andThen(
        MyApi.router + MyApi2.router
      )
  }

}
