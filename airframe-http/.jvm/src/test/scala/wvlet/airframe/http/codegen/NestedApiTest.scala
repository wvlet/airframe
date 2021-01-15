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
  test("generate gRPC client for nested packages") {
    val code = HttpCodeGenerator.generate(
      RouteScanner.buildRouter(Seq(classOf[example.nested.v1.MyApi], classOf[example.nested.v2.MyApi])),
      HttpClientGeneratorConfig("example.nested:grpc")
    )
    debug(code)
  }

  test("generate http client for nested packages") {
    val code = HttpCodeGenerator.generate(
      RouteScanner.buildRouter(Seq(classOf[example.nested.v1.MyApi], classOf[example.nested.v2.MyApi])),
      HttpClientGeneratorConfig("example.nested:sync")
    )
    debug(code)
  }

  test("generate http async client for nested packages") {
    val code = HttpCodeGenerator.generate(
      RouteScanner.buildRouter(Seq(classOf[example.nested.v1.MyApi], classOf[example.nested.v2.MyApi])),
      HttpClientGeneratorConfig("example.nested:async")
    )
    debug(code)
  }

  test("generate http js client for nested packages") {
    val code = HttpCodeGenerator.generate(
      RouteScanner.buildRouter(Seq(classOf[example.nested.v1.MyApi], classOf[example.nested.v2.MyApi])),
      HttpClientGeneratorConfig("example.nested:scalajs")
    )
    debug(code)
  }

}
