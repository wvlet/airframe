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
import wvlet.airframe.surface.{ArraySurface, GenericSurface, OptionSurface, Primitive, Surface, Union2}
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap

/**
  * OpenAPI schema generator
  */
private[openapi] object OpenAPIGenerator extends LogSupport {
  import OpenAPI._

  private def sanitizedSurfaceName(s: Surface): String = {
    s match {
      case o: OptionSurface =>
        sanitizedSurfaceName(o.elementSurface)
      case _ =>
        s.fullName.replaceAll("\\$", ".")
    }
  }

  private[openapi] def generateFromRouter(router: Router): OpenAPI = {
    val referencedSchemas = Map.newBuilder[String, SchemaOrRef]

    val paths = for (route <- router.routes) yield {
      val routeAnalysis = RouteAnalyzer.analyzeRoute(route)
      trace(routeAnalysis)

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

      val requestMediaType = MediaType(
        schema = Schema(
          `type` = "object",
          required = requiredParams(routeAnalysis.userInputParameters),
          properties = Some(
            routeAnalysis.userInputParameters.map { p =>
              p.name -> getOpenAPISchema(p.surface, useRef = true)
            }.toMap
          )
        )
      )
      val requestBodyContent = Map(
        "application/json"      -> requestMediaType,
        "application/x-msgpack" -> requestMediaType
      )

      def isPrimitiveTypeFamily(s: Surface): Boolean = {
        s match {
          case s if s.isPrimitive => true
          case o: OptionSurface   => o.elementSurface.isPrimitive
          case other              => false
        }
      }

      def registerComponent(s: Surface): Unit = {
        s match {
          case s if isPrimitiveTypeFamily(s) =>
          // Do not register schema
          case _ =>
            referencedSchemas += sanitizedSurfaceName(s) -> getOpenAPISchema(s, useRef = false)
        }
      }

      routeAnalysis.httpClientCallInputs.foreach { p =>
        registerComponent(p.surface)
      }
      val returnTypeName = sanitizedSurfaceName(route.returnTypeSurface)
      registerComponent(route.returnTypeSurface)

      val pathParameters: Seq[ParameterOrRef] = routeAnalysis.pathOnlyParameters.toSeq.map { p =>
        if (p.surface.isPrimitive) {
          Parameter(
            name = p.name,
            in = In.path,
            required = p.isRequired,
            allowEmptyValue = if (p.getDefaultValue.nonEmpty) Some(true) else None
          )
        } else {
          ParameterRef(s"#/components/parameters/${sanitizedSurfaceName(p.surface)}")
        }
      }

      val httpMethod = route.method.toLowerCase(Locale.ENGLISH)

      val content: Map[String, MediaType] = if (route.returnTypeSurface == Primitive.Unit) {
        Map.empty
      } else {
        val responseSchema = if (isPrimitiveTypeFamily(route.returnTypeSurface)) {
          getOpenAPISchema(route.returnTypeSurface, useRef = false)
        } else {
          SchemaRef(s"#/components/schemas/${returnTypeName}")
        }

        Map(
          "application/json" -> MediaType(
            schema = responseSchema
          ),
          "application/x-msgpack" -> MediaType(
            schema = responseSchema
          )
        )
      }

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
          "200" ->
            Response(
              description = s"RPC response",
              content = content
            ),
          "400" -> ResponseRef("#/components/responses/400"),
          "500" -> ResponseRef("#/components/responses/500"),
          "503" -> ResponseRef("#/components/responses/503")
        ),
        tags = if (route.isRPC) Some(Seq("rpc")) else None
      )
      path -> Map(httpMethod -> pathItem)
    }

    val schemas = referencedSchemas.result()

    OpenAPI(
      paths = paths.toMap,
      components = Some(
        Components(
          schemas = if (schemas.isEmpty) None else Some(schemas),
          responses = Some(
            Map(
              "400" -> Response(
                description = HttpStatus.BadRequest_400.reason
              ),
              "500" -> Response(
                description = HttpStatus.InternalServerError_500.reason
              ),
              "503" -> Response(
                description = HttpStatus.ServiceUnavailable_503.reason
              )
            )
          )
        )
      )
    )
  }

  def getOpenAPISchema(s: Surface, useRef: Boolean): SchemaOrRef = {
    s match {
      case Primitive.Unit =>
        Schema(
          `type` = "string"
        )
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
      case a if a == Surface.of[Any] =>
        // We should use anyOf here, but it will complicate the handling of Map[String, Any] (additionalParameter: {}), so
        // just use string type:
        Schema(`type` = "string")
      case o: OptionSurface =>
        getOpenAPISchema(o.elementSurface, useRef)
      case g: Surface if classOf[Map[_, _]].isAssignableFrom(g.rawType) && g.typeArgs(0) == Primitive.String =>
        Schema(
          `type` = "object",
          additionalProperties = Some(
            getOpenAPISchema(g.typeArgs(1), useRef)
          )
        )
      case a: ArraySurface =>
        Schema(
          `type` = "array",
          items = Some(getOpenAPISchema(a.elementSurface, useRef))
        )
      case s: Surface if s.isSeq =>
        Schema(
          `type` = "array",
          items = Some(
            getOpenAPISchema(s.typeArgs.head, useRef)
          )
        )
      case s: Surface if useRef =>
        SchemaRef(`$ref` = s"#/components/schemas/${sanitizedSurfaceName(s)}")
      case g: Surface if g.params.length > 0 =>
        // Use ListMap for preserving parameter orders
        val b = ListMap.newBuilder[String, SchemaOrRef]
        g.params.foreach { p =>
          b += p.name -> getOpenAPISchema(p.surface, useRef)
        }
        val properties = b.result()

        Schema(
          `type` = "object",
          required = requiredParams(g.params),
          properties = if (properties.isEmpty) None else Some(properties)
        )
    }
  }

  private def requiredParams(params: Seq[wvlet.airframe.surface.Parameter]): Option[Seq[String]] = {
    val required = params
      .filter(p => p.isRequired || !p.surface.isOption)
      .map(_.name)
    if (required.isEmpty) None else Some(required)
  }

}
