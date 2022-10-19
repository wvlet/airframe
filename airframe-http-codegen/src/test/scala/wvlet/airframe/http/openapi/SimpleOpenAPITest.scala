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
import wvlet.airframe.http.{RPC, Router, description}
import wvlet.airframe.ulid.ULID
import wvlet.airspec.AirSpec

/**
  */
object SimpleOpenAPITest extends AirSpec {
  case class Person(id: Int, name: String)

  @RPC
  trait MyService {
    def hello(person: Person): String
  }

  private val openApiConfig =
    OpenAPIGeneratorConfig(basePackages = Seq("wvlet.airframe.http.openapi.SimpleOpenAPITest"))
  private def openApiGenerator(router: Router) = OpenAPI.ofRouter(router, openApiConfig)

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

    pending(s"This test has been unstable. Need more information")
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

  @RPC
  trait ULIDTestApi {
    def getULID: ULID
  }

  test("represent ulid type as string") {
    val r    = Router.of[ULIDTestApi]
    val yaml = openApiGenerator(r).toYAML
    debug(yaml)

    yaml.contains("""$ref: '#/components/schemas/wvlet.airframe.ulid.ULID'""".stripMargin) shouldBe true
    yaml.contains("""  schemas:
                    |    wvlet.airframe.ulid.ULID:
                    |      type: string""".stripMargin) shouldBe true
  }

  @RPC
  trait DescriptionTestApi {
    @description("sample method")
    def method1(
        @description("custom parameter 1")
        p1: String
    ): Method1Response

    @description("multi-line\ndescription")
    def method2(): Unit

    @description("'single-quoted' description")
    def method3(): Unit
  }

  @description("method1 response")
  case class Method1Response(@description("response code") ret: Int)

  test("annotation description") {
    val r    = Router.of[DescriptionTestApi]
    val yaml = openApiGenerator(r).toYAML

    info(yaml)
    yaml.contains("description: 'sample method'") shouldBe true
    yaml.contains("description: 'custom parameter 1'") shouldBe true
    yaml.contains("description: 'method1 response'") shouldBe true
    yaml.contains("description: 'response code'") shouldBe true

    test("Use literal format multiline descriptions") {
      yaml.contains("""description: |
          |  multi-line
          |  description""".stripMargin)
    }

    test("Use literal format for strings with single quote") {
      yaml.contains(
        """description: |
          |  'single-quoted' description""".stripMargin
      )
    }
  }
}
