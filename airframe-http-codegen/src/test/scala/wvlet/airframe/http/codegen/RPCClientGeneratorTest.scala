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

import example.rpc.RPCExample
import wvlet.airspec.AirSpec

class RPCClientGeneratorTest extends AirSpec {
  private val router = RouteScanner.buildRouter(Seq(classOf[RPCExample]))

  test("generate RPC client") {
    val config = HttpClientGeneratorConfig("example.rpc:rpc")
    val code   = HttpCodeGenerator.generate(router, config)
    debug(code)
  }
}
