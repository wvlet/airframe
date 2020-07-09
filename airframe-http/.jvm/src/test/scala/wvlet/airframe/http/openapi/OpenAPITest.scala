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
import example.openapi.OpenAPIRPCExample
import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.HttpClientGenerator
import wvlet.airspec.AirSpec

/**
  */
class OpenAPITest extends AirSpec {
  private val router = Router.add[OpenAPIRPCExample]

  test("Generate OpenAPI from Router") {
    val openapi = OpenAPI
      .ofRouter(router)
      .withInfo(OpenAPI.Info(title = "RPCTest", version = "1.0"))
    debug(openapi)

    openapi.info.title shouldBe "RPCTest"
    openapi.info.version shouldBe "1.0"

    val json = openapi.toJSON
    debug(s"Open API JSON:\n${json}\n")

    val yaml = openapi.toYAML
    debug(s"Open API Yaml:\n${yaml}\n")
  }

  test(s"Generate OpenAPI spec from command line") {
    val yaml = HttpClientGenerator.generateOpenAPI(router, "yaml")
    debug(yaml)

    val json = HttpClientGenerator.generateOpenAPI(router, "json")
    debug(json)

    intercept[IllegalArgumentException] {
      HttpClientGenerator.generateOpenAPI(router, "invalid")
    }
  }
}
