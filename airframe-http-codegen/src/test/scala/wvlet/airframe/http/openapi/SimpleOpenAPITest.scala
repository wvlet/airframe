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
import wvlet.airframe.http.{RPC, Router}
import wvlet.airspec.AirSpec

/**
  */
object SimpleOpenAPITest extends AirSpec {
  case class Person(id: Int, name: String)

  @RPC
  trait MyService {
    def hello(person: Person): String
  }

  private val openApiConfig = OpenAPIGeneratorConfig(basePackage = "wvlet.airframe.http.openapi.SimpleOpenAPITest")
  private def openApiGenerator(router: Router) =  OpenAPI.ofRouter(router, openApiConfig)

  test("yaml") {
    val r    = Router.of[MyService]
    val yaml = openApiGenerator(r).toYAML
    debug(yaml)
  }

  case class Stat(childStats: Seq[Stat], prop: Map[String, Stat])

  @RPC
  trait RecursiveTypeApi {
    def getStat: Stat
  }

  test("recursive type") {
    val r    = Router.of[RecursiveTypeApi]
    val yaml = openApiGenerator(r).toYAML
    debug(yaml)

    yaml.contains(
      s"""Stat:
         |      type: object
         |      required:
         |        - childStats
         |        - prop
         |      properties:
         |        childStats:
         |          type: array
         |          items:
         |            $$ref: '#/components/schemas/Stat'
         |        prop:
         |          type: object
         |          additionalProperties:
         |            $$ref: '#/components/schemas/Stat'""".stripMargin
    ) shouldBe true
  }

  case class MyObj(p1: String)

  @RPC
  trait CollectionApi {
    def getSeq: Seq[MyObj]
    def getEither: Either[Throwable, MyObj]
  }

  test("collection parameter") {
    val r    = Router.of[CollectionApi]
    val yaml = openApiGenerator(r).toYAML
    debug(yaml)
  }

}
