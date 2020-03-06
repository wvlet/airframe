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
package wvlet.airframe.sbt.http
import example.{QueryApi, ResourceApi}
import wvlet.airframe.http.HttpMethod
import wvlet.airspec.AirSpec

/**
  *
  */
class AirframeHttpPluginTest extends AirSpec {

  val router =
    AirframeHttpPlugin.buildRouter(Seq(classOf[ResourceApi], classOf[QueryApi]))

  test("build router") {
    debug(router)

    val r = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/resources/:id")
    r shouldBe defined

    val q = router.routes.find(x => x.method == HttpMethod.GET && x.path == "/v1/query")
    q shouldBe defined
  }

  test("generate client") {
    info(router)
    AirframeHttpPlugin.generateHttpClient(router)
  }
}
