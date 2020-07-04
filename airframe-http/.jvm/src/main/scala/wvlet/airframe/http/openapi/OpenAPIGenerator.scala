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
import java.util.Locale

import wvlet.airframe.http.{HttpStatus, Router}
import wvlet.airframe.http.codegen.RouteAnalyzer
import wvlet.airframe.surface.{ArraySurface, GenericSurface, Primitive, Surface, Union2}
import wvlet.log.LogSupport

/**
  */
object OpenAPIGenerator extends LogSupport {
  import OpenAPI._

  private def sanitizedSurfaceName(s: Surface): String = {
    s.fullName.replaceAll("\\$", ".")
  }

  def fromRouter(name: String, version: String, router: Router): OpenAPI = {

    val referencedSchemas = Map.newBuilder[String, SchemaOrRef]

    val paths = for (route <- router.routes) yield {
      val routeAnalysis = RouteAnalyzer.analyzeRoute(route)
      info(routeAnalysis)

      val path = "/" + route.pathComponents
        .map { p =>
          p match {
            case x if x.startsWith(":") =>
              s"{${x.substring(1, x.length - 1)}}"
            case x if x.startsWith("*") =>
              s"{${x.substring(1, x.length - 1)}}"
            case _ =>
              p
          }
        }.mkString("/")

      val requestBodyContent = Map(
        "application/json" -> MediaType(
          schema = Schema(
            `type` = "object",
            properties = Some(
              routeAnalysis.userInputParameters.map { p =>
                p.name -> getOpenAPISchema(p.surface, useRef = true)
              }.toMap
            )
          )
        )
      )

      routeAnalysis.httpClientCallInputs.foreach { p =>
        referencedSchemas += sanitizedSurfaceName(p.surface) -> getOpenAPISchema(p.surface, useRef = false)
      }
      val returnTypeName = sanitizedSurfaceName(route.returnTypeSurface)
      referencedSchemas += returnTypeName -> getOpenAPISchema(route.returnTypeSurface, useRef = false)

      val pathParameters: Seq[ParameterOrRef] = routeAnalysis.pathOnlyParameters.toSeq.map { p =>
        if (p.surface.isPrimitive) {
          Parameter(
            name = p.name,
            in = In.path,
            required = !p.surface.isOption,
            allowEmptyValue = if (p.getDefaultValue.nonEmpty) Some(true) else None
          )
        } else {
          ParameterRef(s"#/components/parameters/${sanitizedSurfaceName(p.surface)}")
        }
      }

      val httpMethod = route.method.toLowerCase(Locale.ENGLISH)
      val pathItem = PathItem(
        summary = route.methodSurface.name,
        // TODO Use @RPC(description = ???) or Scaladoc comment
        description = route.methodSurface.name,
        operationId = route.methodSurface.name,
        parameters = if (pathParameters.isEmpty) None else Some(pathParameters),
        requestBody =
          if (requestBodyContent.isEmpty) None
          else
            Some(
              RequestBody(
                content = requestBodyContent,
                required = true
              )
            ),
        responses = Map(
          // POST Created_201 responses
          "201" ->
            Response(
              description = s"RPC response",
              content = Map(
                "application/json" -> MediaType(
                  schema = SchemaRef(s"#/components/schemas/${returnTypeName}")
                ),
                "application/x-msgpack" -> MediaType(
                  schema = Schema(
                    `type` = "string",
                    format = Some("msgpack")
                  )
                )
              )
            ),
          "400" -> ResponseRef("#/components/responses/400"),
          "500" -> ResponseRef("#/components/responses/500"),
          "503" -> ResponseRef("#/components/responses/503")
        )
      )
      path -> Map(httpMethod -> pathItem)
    }

    val schemas = referencedSchemas.result()

    OpenAPI(
      info = Info(
        title = name,
        version = version
      ),
      paths = paths.toMap,
      components = Some(
        Components(
          schemas = if (schemas.isEmpty) None else Some(schemas),
          responses = Some(
            Map(
              "400" -> Response(
                description = HttpStatus.BadRequest_400.reason,
                content = Map(
                  "application/json" ->
                    MediaType(
                      schema = Schema(
                        `type` = "string"
                        //properties = ...
                      )
                    )
                )
              ),
              "500" -> Response(
                description = HttpStatus.InternalServerError_500.reason,
                content = Map(
                  "application/json" ->
                    MediaType(
                      schema = Schema(
                        `type` = "string"
                        //properties = ...
                      )
                    )
                )
              ),
              "503" -> Response(
                description = HttpStatus.ServiceUnavailable_503.reason,
                content = Map(
                  "application/json" ->
                    MediaType(
                      schema = Schema(
                        `type` = "string"
                        //properties = ...
                      )
                    )
                )
              )
            )
          )
        )
      )
    )
  }

  def getOpenAPISchema(s: Surface, useRef: Boolean): SchemaOrRef = {
    s match {
      case Primitive.Int =>
        Schema(
          `type` = "integer",
          format = Some("int32")
        )
      case Primitive.Long =>
        Schema(
          `type` = "integer",
          format = Some("int64")
        )
      case Primitive.Float =>
        Schema(
          `type` = "number",
          format = Some("float")
        )
      case Primitive.Double =>
        Schema(
          `type` = "number",
          format = Some("double")
        )
      case Primitive.Boolean =>
        Schema(`type` = "boolean")
      case Primitive.String =>
        Schema(`type` = "string")
      case a: ArraySurface =>
        Schema(
          `type` = "array",
          items = Some(
            Seq(getOpenAPISchema(a.elementSurface, useRef))
          )
        )
      case g: Surface if classOf[Map[_, _]].isAssignableFrom(g.rawType) && g.typeArgs(0) == Primitive.String =>
        Schema(
          `type` = "object",
          additionalProperties = Some(
            getOpenAPISchema(g.typeArgs(1), useRef)
          )
        )
      case s: Surface if s.isSeq =>
        Schema(
          `type` = "array",
          items = Some(
            Seq(getOpenAPISchema(s.typeArgs.head, useRef))
          )
        )
      case s: Surface if useRef =>
        SchemaRef(`$ref` = s"#/components/schemas/${sanitizedSurfaceName(s)}")
      case g: Surface if g.params.length > 0 =>
        val requiredParams = g.params
          .filter(p => p.isRequired || !p.surface.isOption)
          .map(_.name)

        val properties = g.params.map { p =>
          p.name -> getOpenAPISchema(p.surface, useRef)
        }.toMap

        Schema(
          `type` = "object",
          required = if (requiredParams.isEmpty) None else Some(requiredParams),
          properties = if (properties.isEmpty) None else Some(properties)
        )
    }
  }

}
