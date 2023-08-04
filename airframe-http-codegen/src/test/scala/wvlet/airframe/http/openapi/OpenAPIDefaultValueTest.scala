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
package wvlet.airframe.http.openapi

import wvlet.airframe.http.{RPC, Router, RxRouter}
import wvlet.airspec.AirSpec

object OpenAPIDefaultValueTest extends AirSpec {

  @RPC
  trait TestApi {
    def query(q: TestApi.QueryRequest): String
  }
  object TestApi {
    case class QueryRequest(
        p1: Double = 0.1,
        p2: Int,
        p3: Option[Int]
    )
  }

  test("generate default values") {
    val router = RxRouter.of[TestApi]
    val openapi = OpenAPIGenerator.buildFromRouter(
      Router.fromRxRouter(router),
      OpenAPIGeneratorConfig(basePackages = Seq("wvlet.airframe.http.openapi.OpenAPIDefaultValueTest"))
    )
    val yaml = openapi.toYAML
    info(yaml)

    yaml shouldContain "- p"            // as a required parameter
    yaml shouldContain "default: '0.1'" // default value for p1
    yaml shouldNotContain "- p1"
    yaml shouldNotContain "- p3"
    yaml shouldContain "- p2" // as a required parameter
  }

}
