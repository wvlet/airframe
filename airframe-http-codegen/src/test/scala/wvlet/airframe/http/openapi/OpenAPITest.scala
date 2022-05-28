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
import example.openapi.{OpenAPIEndpointExample, OpenAPIRPCExample, OpenAPISmallExample}
import io.swagger.v3.parser.OpenAPIV3Parser
import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.HttpCodeGenerator
import wvlet.airspec.AirSpec

/**
  */
class OpenAPITest extends AirSpec {
  private val rpcRouter      = Router.add[OpenAPIRPCExample]
  private val endpointRouter = Router.add[OpenAPIEndpointExample]

  private val config = OpenAPIGeneratorConfig(basePackages = Seq("example.openapi"))

  test("Generate OpenAPI from Router") {
    val openapi = OpenAPI
      .ofRouter(rpcRouter, config)
      .withInfo(OpenAPI.Info(title = "RPCTest", version = "1.0"))
    trace(openapi)

    openapi.info.title shouldBe "RPCTest"
    openapi.info.version shouldBe "1.0"

    val json = openapi.toJSON
    trace(s"Open API JSON:\n${json}\n")
    parseOpenAPI(json)

    val yaml = openapi.toYAML
    trace(s"Open API Yaml:\n${yaml}\n")
    parseOpenAPI(yaml)

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
        |                  $ref: '#/components/schemas/OpenAPIRPCExample.RPCRequest'""".stripMargin,
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
        |        - OpenAPIRPCExample""".stripMargin,
      """      responses:
        |        '200':
        |          description: 'RPC response'
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIRPCExample.RPCResponse'
        |            application/x-msgpack:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIRPCExample.RPCResponse'""".stripMargin,
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
        |      responses:
        |        '200':
        |          description: 'RPC response'""".stripMargin,
      """  /example.openapi.OpenAPIRPCExample/rpcWithFutureResponse:
        |    post:
        |      summary: rpcWithFutureResponse
        |      description: rpcWithFutureResponse
        |      operationId: rpcWithFutureResponse
        |      responses:
        |        '200':
        |          description: 'RPC response'
        |          content:
        |            application/json:
        |              schema:
        |                type: string
        |""".stripMargin,
      """components:
        |  schemas:
        |    OpenAPIRPCExample.RPCRequest:
        |      type: object
        |      required:
        |        - x1
        |        - x2
        |        - x3
        |        - x4
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
      """    OpenAPIRPCExample.RPCResponse:
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
      debug(s"checking\n${x}")
      yaml.contains(x) shouldBe true
    }
  }

  test(s"Generate OpenAPI spec from command line") {
    val yaml = HttpCodeGenerator.generateOpenAPI(
      rpcRouter,
      "yaml",
      title = "My API",
      version = "1.0",
      packageNames = Seq("example.openapi")
    )
    debug(yaml)
    parseOpenAPI(yaml)

    val json = HttpCodeGenerator.generateOpenAPI(
      rpcRouter,
      "json",
      title = "My API",
      version = "1.0",
      packageNames = Seq("example.openapi")
    )
    debug(json)
    parseOpenAPI(json)

    intercept[IllegalArgumentException] {
      HttpCodeGenerator.generateOpenAPI(
        rpcRouter,
        "invalid",
        title = "My API",
        version = "1.0",
        packageNames = Seq("example.openapi")
      )
    }
  }

  test("Get with param") {
    val openAPI = OpenAPI
      .ofRouter(Router.of[OpenAPISmallExample])
    val yaml = openAPI.toYAML
    yaml.contains("""  /v1/get/{id}:
                    |    get:
                    |      summary: getWithParam
                    |      description: getWithParam
                    |      operationId: getWithParam
                    |      parameters:
                    |        - name: id
                    |          in: path
                    |          required: true
                    |          schema:
                    |            type: integer
                    |            format: int32""".stripMargin) shouldBe true
  }

  test("Generate OpenAPI spec from @Endpoint") {
    val openapi = OpenAPI
      .ofRouter(endpointRouter, config)
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
      """  responses:
        |    '400':
        |      description: 'Bad Request'
        |    '500':
        |      description: 'Internal Server Error'
        |    '503':
        |      description: 'Service Unavailable'""".stripMargin,
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
        |            type: string""".stripMargin,
      """  /v1/post1:
        |    post:
        |      summary: post1
        |      description: post1
        |      operationId: post1
        |      responses:
        |        '200':
        |          description: 'RPC response'
        |        '400':
        |          $ref: '#/components/responses/400'
        |        '500':
        |          $ref: '#/components/responses/500'
        |        '503':
        |          $ref: '#/components/responses/503'""".stripMargin,
      """  /v1/post2/{id}:
        |    post:
        |      summary: post2
        |      description: post2
        |      operationId: post2
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              required:
        |                - id""".stripMargin,
      """  /v1/post4/{id}:
        |    post:
        |      summary: post4
        |      description: post4
        |      operationId: post4
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              required:
        |                - id
        |                - p1
        |              properties:
        |                p1:
        |                  type: string""".stripMargin,
      """  /v1/post5:
        |    post:
        |      summary: post5
        |      description: post5
        |      operationId: post5
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              required:
        |                - p1
        |              properties:
        |                p1:
        |                  $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointRequest'
        |          application/x-msgpack:
        |            schema:
        |              type: object
        |              required:
        |                - p1
        |              properties:
        |                p1:
        |                  $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointRequest'
        |        required: true
        |      responses:
        |        '200':
        |          description: 'RPC response'
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointResponse'
        |            application/x-msgpack:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointResponse'""".stripMargin,
      """  /v1/post6/{id}:
        |    post:
        |      summary: post6
        |      description: post6
        |      operationId: post6
        |      parameters:
        |        - name: id
        |          in: path
        |          required: true
        |          schema:
        |            type: integer
        |            format: int32
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              required:
        |                - id
        |                - p1
        |              properties:
        |                p1:
        |                  $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointRequest'
        |          application/x-msgpack:
        |            schema:
        |              type: object
        |              required:
        |                - id
        |                - p1
        |              properties:
        |                p1:
        |                  $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointRequest'
        |        required: true
        |      responses:
        |        '200':
        |          description: 'RPC response'
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointResponse'
        |            application/x-msgpack:
        |              schema:
        |                $ref: '#/components/schemas/OpenAPIEndpointExample.EndpointResponse'""".stripMargin,
      """    OpenAPIEndpointExample.EndpointRequest:
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
      """    OpenAPIEndpointExample.EndpointResponse:
        |      type: object
        |      required:
        |        - y1
        |        - y2
        |      properties:
        |        y1:
        |          type: string
        |        y2:
        |          type: boolean""".stripMargin,
      """  /v1/put1:
        |    put:
        |      summary: put1
        |      description: put1
        |      operationId: put1""".stripMargin,
      """  /v1/delete1:
        |    delete:
        |      summary: delete1
        |      description: delete1
        |      operationId: delete1""".stripMargin,
      """  /v1/patch1:
        |    patch:
        |      summary: patch1
        |      description: patch1
        |      operationId: patch1""".stripMargin,
      """  /v1/head1:
        |    head:
        |      summary: head1
        |      description: head1
        |      operationId: head1""".stripMargin,
      """  /v1/options1:
        |    options:
        |      summary: options1
        |      description: options1
        |      operationId: options1""".stripMargin,
      """  /v1/trace1:
        |    trace:
        |      summary: trace1
        |      description: trace1
        |      operationId: trace1""".stripMargin,
      """  /v1/multi_method:""".stripMargin,
      """    post:
        |      summary: multi1
        |      description: multi1
        |      operationId: multi1""".stripMargin,
      """    options:
        |      summary: multi2
        |      description: multi2
        |      operationId: multi2""".stripMargin
    )

    // Use this code snippet for ease of testing at https://editor.swagger.io/
    // java.awt.Toolkit.getDefaultToolkit.getSystemClipboard
    //      .setContents(new java.awt.datatransfer.StringSelection(yaml), null)

    fragments.foreach { x =>
      debug(x)
      yaml.contains(x) shouldBe true
    }

    // Parsing test
    OpenAPI.parseJson(json)
    val oa = parseOpenAPI(yaml)

    test("generate overloaded methods") {
      val mm = oa.getPaths.get("/v1/multi_method")
      mm.getOptions shouldNotBe null
      mm.getPost shouldNotBe null
    }
  }

  private def parseOpenAPI(yamlOrJson: String): io.swagger.v3.oas.models.OpenAPI = {
    new OpenAPIV3Parser().readContents(yamlOrJson).getOpenAPI
  }
}
