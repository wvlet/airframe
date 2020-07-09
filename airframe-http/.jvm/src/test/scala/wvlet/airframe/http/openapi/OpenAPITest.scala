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
import example.openapi.{OpenAPIEndpointExample, OpenAPIRPCExample}
import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.HttpCodeGenerator
import wvlet.airspec.AirSpec

/**
  */
class OpenAPITest extends AirSpec {
  private val rpcRouter      = Router.add[OpenAPIRPCExample]
  private val endpointRouter = Router.add[OpenAPIEndpointExample]

  test("Generate OpenAPI from Router") {
    val openapi = OpenAPI
      .ofRouter(rpcRouter)
      .withInfo(OpenAPI.Info(title = "RPCTest", version = "1.0"))
    debug(openapi)

    openapi.info.title shouldBe "RPCTest"
    openapi.info.version shouldBe "1.0"

    val json = openapi.toJSON
    debug(s"Open API JSON:\n${json}\n")

    val yaml = openapi.toYAML
    debug(s"Open API Yaml:\n${yaml}\n")

    // Naive tests for checking YAML fragments.
    // We need to refine these fragments if we change OpenAPI format and model classes
    val fragments = Seq(
      """openapi: '3.0.3'
        |info:
        |  title: RPCTest
        |  version: '1.0'""".stripMargin,
      """paths:""",
      """      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              required:
        |                - p1
        |              properties:
        |                p1:
        |                  $ref: '#/components/schemas/example.openapi.OpenAPIRPCExample.RPCRequest'""".stripMargin,
      """      responses:
        |        '200':
        |          description: 'RPC response'
        |        '400':
        |          $ref: '#/components/responses/400'
        |        '500':
        |          $ref: '#/components/responses/500'
        |        '503':
        |          $ref: '#/components/responses/503'
        |      tags:
        |        - rpc""".stripMargin,
      """      responses:
        |        '200':
        |          description: 'RPC response'
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/example.openapi.OpenAPIRPCExample.RPCResponse'
        |            application/x-msgpack:
        |              schema:
        |                $ref: '#/components/schemas/example.openapi.OpenAPIRPCExample.RPCResponse'""".stripMargin,
      """      operationId: rpcWithOption
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              properties:
        |                p1:
        |                  type: string
        |          application/x-msgpack:
        |            schema:
        |              type: object
        |              properties:
        |                p1:
        |                  type: string
        |        required: true""".stripMargin,
      """  /example.openapi.OpenAPIRPCExample/zeroAryRPC:
        |    post:
        |      summary: zeroAryRPC
        |      description: zeroAryRPC
        |      operationId: zeroAryRPC
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |          application/x-msgpack:
        |            schema:
        |              type: object
        |        required: true""".stripMargin,
      """components:
        |  schemas:
        |    example.openapi.OpenAPIRPCExample.RPCRequest:
        |      type: object
        |      required:
        |        - x1
        |        - x2
        |        - x3
        |        - x4
        |        - x5
        |        - x6
        |        - x7
        |        - x8
        |      properties:
        |        x1:
        |          type: integer
        |          format: int32
        |        x2:
        |          type: integer
        |          format: int64
        |        x3:
        |          type: boolean
        |        x4:
        |          type: number
        |          format: float
        |        x5:
        |          type: number
        |          format: double
        |        x6:
        |          type: array
        |          items:
        |            type: string
        |        x7:
        |          type: array
        |          items:
        |            type: string
        |        x8:
        |          type: object
        |          additionalProperties:
        |            type: string
        |        x9:
        |          type: integer
        |          format: int32""".stripMargin,
      """    example.openapi.OpenAPIRPCExample.RPCResponse:
        |      type: object
        |      required:
        |        - y1
        |        - y2
        |      properties:
        |        y1:
        |          type: string
        |        y2:
        |          type: boolean""".stripMargin
    )

    fragments.foreach { x =>
      debug(s"checking ${x}")
      yaml.contains(x) shouldBe true
    }
  }

  test(s"Generate OpenAPI spec from command line") {
    val yaml = HttpCodeGenerator.generateOpenAPI(rpcRouter, "yaml", title = "My API", version = "1.0")
    debug(yaml)

    val json = HttpCodeGenerator.generateOpenAPI(rpcRouter, "json", title = "My API", version = "1.0")
    debug(json)

    intercept[IllegalArgumentException] {
      HttpCodeGenerator.generateOpenAPI(rpcRouter, "invalid", title = "My API", version = "1.0")
    }
  }

  test("Generate OpenAPI spec from @Endpoint") {
    val openapi = OpenAPI
      .ofRouter(endpointRouter)
      .withInfo(OpenAPI.Info(title = "EndpointTest", version = "1.0"))
    debug(openapi)

    val json = openapi.toJSON
    debug(json)
    val yaml = openapi.toYAML
    debug(yaml)

    val fragments = Seq(
      """info:
        |  title: EndpointTest
        |  version: '1.0'""".stripMargin,
      """  /v1/get0:
        |    get:
        |      summary: get0
        |      description: get0
        |      operationId: get0
        |""".stripMargin,
      """  /v1/get1/{id}:
        |    get:
        |      summary: get1
        |      description: get1
        |      operationId: get1
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32""".stripMargin,
      """  /v1/get2/{id}/{name}:
        |    get:
        |      summary: get2
        |      description: get2
        |      operationId: get2
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32
        |        - name: name
        |          in: path
        |          required: true
        |          schema:
        |            type: string""".stripMargin,
      """  /v1/get3/{id}:
        |    get:
        |      summary: get3
        |      description: get3
        |      operationId: get3
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32
        |        - name: p1
        |          in: query
        |          required: true
        |          schema:
        |            type: string""".stripMargin
    )

    fragments.foreach { x =>
      debug(x)
      yaml.contains(x) shouldBe true
    }
  }
}
