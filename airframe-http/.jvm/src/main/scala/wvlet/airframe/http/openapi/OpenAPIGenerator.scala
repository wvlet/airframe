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

import wvlet.airframe.http.{HttpMethod, HttpStatus, Router}
import wvlet.airframe.http.codegen.RouteAnalyzer
import wvlet.airframe.http.openapi.OpenAPI.Response
import wvlet.airframe.surface.{ArraySurface, GenericSurface, MethodParameter, OptionSurface, Primitive, Surface, Union2}
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap

case class OpenAPIGeneratorConfig(
    // status code -> Response
    commonErrorResponses: Map[String, OpenAPI.Response] = ListMap(
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

/**
  * OpenAPI schema generator
  */
private[openapi] object OpenAPIGenerator extends LogSupport {
  import OpenAPI._

  /**
    * Sanitize the given class name as Open API doesn't support names containing $
    */
  private def sanitizedSurfaceName(s: Surface): String = {
    s match {
      case o: OptionSurface =>
        sanitizedSurfaceName(o.elementSurface)
      case _ =>
        s.fullName.replaceAll("\\$", ".")
    }
  }

  /**
    * Check whether the type is a primitive (no need to use component reference) or not
    */
  private def isPrimitiveTypeFamily(s: Surface): Boolean = {
    s match {
      case s if s.isPrimitive => true
      case o: OptionSurface   => o.elementSurface.isPrimitive
      case other              => false
    }
  }

  private[openapi] def buildFromRouter(router: Router, config: OpenAPIGeneratorConfig): OpenAPI = {
    val referencedSchemas = Map.newBuilder[String, SchemaOrRef]

    val paths = for (route <- router.routes) yield {
      val routeAnalysis = RouteAnalyzer.analyzeRoute(route)
      trace(routeAnalysis)

      // Replace path parameters into
      val path = "/" + route.pathComponents
        .map { p =>
          p match {
            case x if x.startsWith(":") =>
              s"{${x.substring(1, x.length)}}"
            case x if x.startsWith("*") =>
              s"{${x.substring(1, x.length)}}"
            case _ =>
              p
          }
        }.mkString("/")

      // User HTTP request body
      val requestMediaType = MediaType(
        schema = Schema(
          `type` = "object",
          required = requiredParams(routeAnalysis.userInputParameters),
          properties = Some(
            routeAnalysis.httpClientCallInputs.map { p =>
              p.name -> getOpenAPISchema(p.surface, useRef = true)
            }.toMap
          )
        )
      )
      val requestBodyContent: Map[String, MediaType] = {
        if (route.method == HttpMethod.GET) {
          // GET should have no request body
          Map.empty
        } else {
          if (routeAnalysis.userInputParameters.isEmpty) {
            Map.empty
          } else {
            Map(
              "application/json"      -> requestMediaType,
              "application/x-msgpack" -> requestMediaType
            )
          }
        }
      }

      /**
        * Register a component for creating a reference link
        */
      def registerComponent(s: Surface): Unit = {
        s match {
          case s if isPrimitiveTypeFamily(s) =>
          // Do not register schema
          case _ =>
            referencedSchemas += sanitizedSurfaceName(s) -> getOpenAPISchema(s, useRef = false)
        }
      }

      // Http response type
      routeAnalysis.httpClientCallInputs.foreach { p =>
        registerComponent(p.surface)
      }
      val returnTypeName = sanitizedSurfaceName(route.returnTypeSurface)
      registerComponent(route.returnTypeSurface)

      def toParameter(p: MethodParameter, in: In): ParameterOrRef = {
        if (p.surface.isPrimitive) {
          Parameter(
            name = p.name,
            in = in,
            required = true,
            schema = if (isPrimitiveTypeFamily(p.surface)) {
              Some(getOpenAPISchema(p.surface, useRef = false))
            } else {
              registerComponent(p.surface)
              Some(SchemaRef(s"#/components/schemas/${sanitizedSurfaceName(p.surface)}"))
            },
            allowEmptyValue = if (p.getDefaultValue.nonEmpty) Some(true) else None
          )
        } else {
          ParameterRef(s"#/components/parameters/${sanitizedSurfaceName(p.surface)}")
        }
      }

      // URL path parameters (e.g., /:id/, /*path, etc.)
      val pathParameters: Seq[ParameterOrRef] = routeAnalysis.pathOnlyParameters.toSeq.map { p =>
        toParameter(p, In.path)
      }
      // URL query string parameters
      val queryParameters: Seq[ParameterOrRef] = if (route.method == HttpMethod.GET) {
        routeAnalysis.httpClientCallInputs.map { p =>
          toParameter(p, In.query)
        }
      } else {
        Seq.empty
      }
      val pathAndQueryParameters = pathParameters ++ queryParameters

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
        parameters = if (pathAndQueryParameters.isEmpty) None else Some(pathAndQueryParameters),
        requestBody = if (requestBodyContent.isEmpty) {
          None
        } else {
          Some(
            RequestBody(
              content = requestBodyContent,
              required = true
            )
          )
        },
        responses = Map(
          "200" ->
            Response(
              description = s"RPC response",
              content = content
            )
        ) ++ config.commonErrorResponses
          .map(_._1).map { statusCode =>
            statusCode -> ResponseRef(s"#/components/responses/${statusCode}")
          }.toMap,
        tags = if (route.isRPC) Some(Seq("rpc")) else None
      )
      path -> Map(httpMethod -> pathItem)
    }

    val schemas = referencedSchemas.result()

    OpenAPI(
      // Use ListMap for preserving the order
      paths = ListMap.newBuilder.++=(paths).result(),
      components = Some(
        Components(
          schemas = if (schemas.isEmpty) None else Some(schemas),
          responses = if (config.commonErrorResponses.isEmpty) None else Some(config.commonErrorResponses)
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
