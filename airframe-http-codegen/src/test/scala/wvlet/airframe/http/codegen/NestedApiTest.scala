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

import wvlet.airspec.AirSpec

/**
  */
class NestedApiTest extends AirSpec {

  private def check(code: String): Unit = {
    debug(code)
    code.contains("Seq[example.nested.v1.MyApi.Message]]") shouldBe true
  }

  private val router = RouteScanner.buildRxRouter(Seq("example.nested"))

  test("generate gRPC client for nested packages") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.nested:grpc")
    )
    check(code)
  }

  test("generate http client for nested packages") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.nested:sync")
    )
    check(code)
  }

  test("generate http async client for nested packages") {
    val code = HttpCodeGenerator.generate(
      router,
      HttpClientGeneratorConfig("example.nested:async")
    )
    check(code)
  }
}
