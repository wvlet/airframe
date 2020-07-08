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
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.Router
import wvlet.airspec.AirSpec

/**
  */
class OpenAPITest extends AirSpec {
  test("Generate OpenAPI from Router") {
    val router = Router.add[OpenAPIRPCExample]

    val openapi = OpenAPIGenerator.fromRouter(name = "RPCTest", version = "1.0", router)
    info(openapi)

    val codec = MessageCodecFactory.defaultFactoryForJSON.of[OpenAPI]
    val json  = codec.toJson(openapi)
    info(s"Open API JSON:\n${json}\n")
  }
}
