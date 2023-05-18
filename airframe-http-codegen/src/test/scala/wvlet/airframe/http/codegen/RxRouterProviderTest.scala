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
package wvlet.airframe.http.codegen

import wvlet.airframe.http.RxRouter
import wvlet.airspec.AirSpec

class RxRouterProviderTest extends AirSpec {

  private val router: RxRouter = {
    RouteScanner.buildRxRouter(Seq("example.rpc"))
  }

  test("Build router from RxRouterProvider") {
    router.children.size shouldBe 2
    router.routes.size > 0 shouldBe true
  }
}
