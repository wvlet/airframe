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
import example.generic.{GenericRequestService, GenericService}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airspec.AirSpec

import scala.concurrent.Future

/**
  */
class GenericServiceTest extends AirSpec {

  private val router = RouteScanner.buildRxRouter(Seq(classOf[GenericService[Future]].getPackageName))

  test("support F and Future return values in async clients") {
    debug(router)

    val code = HttpCodeGenerator.generate(router, HttpClientGeneratorConfig("example.generic:async"))
    code.contains(": F[String]") shouldBe true
    code.contains(": F[Int]") shouldBe true
    code.contains("import wvlet.airframe.http.HttpMessage.Response") shouldBe false
  }

  test("support F and Future return values in sync clients") {
    debug(router)

    val code = HttpCodeGenerator.generate(router, HttpClientGeneratorConfig("example.generic:sync"))
    code.contains(": String = {") shouldBe true
    code.contains(": Int = {") shouldBe true
    code.contains("import wvlet.airframe.http.HttpMessage.Response") shouldBe false
  }

  test("abstract request type") {
    pending("Not sure using backend specific request/response in IDL is a good idea")
    val r = RouteScanner.buildRxRouter(Seq(classOf[GenericRequestService[Future, Request, Response]].getPackageName))
    debug(r)
    val code = HttpCodeGenerator.generate(r, HttpClientGeneratorConfig("example.generic.GenericRequestService:async"))
  }
}
