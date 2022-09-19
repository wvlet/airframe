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
import wvlet.airframe.codec.GenericException
import wvlet.airframe.http.codegen.RouteAnalyzer
import wvlet.airframe.http.openapi.OpenAPI.Response
import wvlet.airframe.http.{HttpMethod, HttpStatus, Router, description}
import wvlet.airframe.json.JSON.JSONValue
import wvlet.airframe.json.Json
import wvlet.airframe.metrics.{Count, DataSize, ElapsedTime}
import wvlet.airframe.msgpack.spi.{MsgPack, Value}
import wvlet.airframe.surface._
import wvlet.airframe.ulid.ULID
import wvlet.log.LogSupport
import wvlet.airframe.surface.reflect._

import java.time.Instant
import java.util.Locale
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import scala.collection.immutable.ListMap

case class OpenAPIGeneratorConfig(
    basePackages: Seq[String] = Seq.empty,
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
) {
  val packagePrefixes: Seq[String] = basePackages.map(prefix => if (prefix.endsWith(".")) prefix else s"${prefix}.")
}

/**
  * OpenAPI schema generator
  */
private[openapi] object OpenAPIGenerator extends LogSupport {

  import OpenAPI._

  private[openapi] def buildFromRouter(router: Router, config: OpenAPIGeneratorConfig): OpenAPI = {
    val g = new OpenAPIGenerator(config)
    g.buildFromRouter(router)
  }

  /**
    * Sanitize the given class name as Open API doesn't support names containing $
    */
  private def sanitizedSurfaceName(s: Surface): String = {
    s match {
      case o: OptionSurface =>
        sanitizedSurfaceName(o.elementSurface)
      case r: Surface if Router.isFinagleReader(r) =>
        sanitizedSurfaceName(r.typeArgs(0))
      case s: Surface if Router.isFuture(s) =>
        sanitizedSurfaceName(Router.unwrapFuture(s))
      case s: Surface if s == Surface.of[Throwable] =>
        sanitizedSurfaceName(Surface.of[GenericException])
      case other =>
        other.fullName.replaceAll("\\$", ".")
    }
  }

  /**
    * Check whether the type is a primitive (no need to use component reference) or not
    */
  private def isPrimitiveTypeFamily(s: Surface): Boolean = {
    s match {
      case s if s.isPrimitive => true
      case o: OptionSurface   => o.elementSurface.isPrimitive
      case f: Surface if Router.isFuture(f) =>
        isPrimitiveTypeFamily(Router.unwrapFuture(f))
      case r: Surface if Router.isHttpResponse(r) =>
        // HTTP raw response (without explicit type)
        true
      case s: Surface if s.isSeq =>
        isPrimitiveTypeFamily(s.typeArgs.head)
      case other => false
    }
  }

  private def extractNonPrimitiveSurfaces(s: Surface, seen: Set[Surface]): Seq[Surface] = {
    s match {
      case s if seen.contains(s) =>
        Seq(s)
      case s if s.isPrimitive || Router.isHttpResponse(s) =>
        Seq.empty
      case s if s.isSeq || s.isOption || Router.isFinagleReader(s) =>
        extractNonPrimitiveSurfaces(s.typeArgs(0), seen)
      case s: ArraySurface =>
        extractNonPrimitiveSurfaces(s.typeArgs(0), seen)
      case s if Router.isFuture(s) =>
        extractNonPrimitiveSurfaces(Router.unwrapFuture(s), seen)
      case s: Surface if s == Surface.of[Throwable] =>
        Seq(Surface.of[GenericException])
      // Generic collection types
      case s: Surface if s.typeArgs.length > 0 =>
        s.typeArgs.flatMap(t => extractNonPrimitiveSurfaces(t, seen))
      // Object type
      case s: Surface if s.params.length > 0 =>
        Seq(s) ++ s.params.flatMap(p => extractNonPrimitiveSurfaces(p.surface, seen + s))
      case other =>
        Seq(s)
    }
  }

  /**
    * Merge PathItems in the same path
    */
  private def mergePaths(paths: Seq[(String, Map[String, PathItem])]): Seq[(String, Map[String, PathItem])] = {
    val b = Seq.newBuilder[(String, Map[String, PathItem])]
    for ((path, lst) <- paths.groupBy(_._1)) {
      val pathItems = lst.map(_._2)
      if (pathItems.size == 0) {
        b += path -> pathItems.head
      } else {
        b += path -> pathItems.reduce(_ ++ _)
      }
    }
    b.result()
  }

  private def requiredParams(params: Seq[wvlet.airframe.surface.Parameter]): Option[Seq[String]] = {
    val required = params
      .filter(p => p.isRequired || !(p.getDefaultValue.nonEmpty || p.surface.isOption))
      .map(_.name)
    if (required.isEmpty) None
    else Some(required)
  }
}

class OpenAPIGenerator(config: OpenAPIGeneratorConfig) extends LogSupport {
  import OpenAPI._
  import OpenAPIGenerator._

  import scala.jdk.CollectionConverters._
  private val schemaCache = {
    new ConcurrentHashMap[Surface, SchemaOrRef]().asScala
  }

  private def schemaName(surface: Surface): String = {
    val s = sanitizedSurfaceName(surface)
    suppressPackagePrefix(s)
  }

  private def suppressPackagePrefix(name: String): String = {
    val candidates = config.packagePrefixes.map(name.stripPrefix(_))
    if (candidates.isEmpty) {
      name
    } else {
      // Take the smallest name
      candidates.minBy(_.length)
    }
  }

  def buildFromRouter(router: Router): OpenAPI = {
    val referencedSchemas = Map.newBuilder[String, SchemaOrRef]

    val paths: Seq[(String, Map[String, PathItem])] = for (route <- router.routes) yield {
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
        }
        .mkString("/")

      // Collect all component types
      val componentTypes = (routeAnalysis.userInputParameters.map(_.surface) ++
        routeAnalysis.httpClientCallInputs.map(_.surface) ++
        Seq(route.returnTypeSurface))
        .flatMap(p => extractNonPrimitiveSurfaces(p, Set.empty))
        .distinct
        .toSet

      // Register a component for creating a reference link
      def registerComponent(s: Surface): Unit = {
        s match {
          case s if isPrimitiveTypeFamily(s) =>
          // Do not register schema
          case _ =>
            trace(s"Register a component: ${s}")
            referencedSchemas += schemaName(s) -> getOpenAPISchemaOfSurface(s, Set.empty)
        }
      }
      componentTypes.map(s => registerComponent(s))

      // Generate schema for the user HTTP request body
      val requestMediaType = MediaType(
        schema = Schema(
          `type` = "object",
          required = requiredParams(routeAnalysis.userInputParameters),
          properties = Some(
            routeAnalysis.httpClientCallInputs.map { p =>
              p.name -> getOpenAPISchemaOfParameter(p, componentTypes)
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

      // Http response type
      def toParameter(p: MethodParameter, in: In): ParameterOrRef = {
        if (p.surface.isPrimitive) {
          Parameter(
            name = p.name,
            in = in,
            description = p.findAnnotationOf[description].map(_.value()),
            required = true,
            schema = if (isPrimitiveTypeFamily(p.surface)) {
              Some(getOpenAPISchemaOfParameter(p, componentTypes))
            } else {
              registerComponent(p.surface)
              Some(SchemaRef(s"#/components/schemas/${schemaName(p.surface)}"))
            },
            allowEmptyValue = if (p.getDefaultValue.nonEmpty) Some(true) else None
          )
        } else {
          ParameterRef(s"#/components/parameters/${schemaName(p.surface)}")
        }
      }

      // URL path parameters (e.g., /:id/, /*path, etc.)
      val pathParameters: Seq[ParameterOrRef] =
        routeAnalysis.pathOnlyParameters.toSeq.map { p =>
          toParameter(p, In.path)
        }
      // URL query string parameters
      val queryParameters: Seq[ParameterOrRef] =
        if (route.method == HttpMethod.GET) {
          routeAnalysis.httpClientCallInputs.map { p =>
            toParameter(p, In.query)
          }
        } else {
          Seq.empty
        }
      val pathAndQueryParameters = pathParameters ++ queryParameters

      val httpMethod = route.method.toLowerCase(Locale.ENGLISH)

      val content: Map[String, MediaType] =
        if (route.returnTypeSurface == Primitive.Unit) {
          Map.empty
        } else {
          val responseSchema =
            getOpenAPISchemaOfSurface(route.returnTypeSurface, componentTypes)
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
        description = route.methodSurface
          // Extract the description from @description annotation if exists
          .findAnnotationOf[description].map(_.value()).getOrElse(
            route.methodSurface.name
          ),
        operationId = route.methodSurface.name,
        parameters =
          if (pathAndQueryParameters.isEmpty) None
          else Some(pathAndQueryParameters),
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
          .map(_._1)
          .map { statusCode =>
            statusCode -> ResponseRef(s"#/components/responses/${statusCode}")
          }
          .toMap,
        tags = {
          Some(Seq(suppressPackagePrefix(route.serviceName)))
        }
      )
      path -> Map(httpMethod -> pathItem)
    }

    val schemas = referencedSchemas.result()

    val mergedPaths = mergePaths(paths)

    OpenAPI(
      // Use ListMap for preserving the order
      paths = ListMap.newBuilder.++=(mergedPaths).result(),
      components = Some(
        Components(
          schemas = if (schemas.isEmpty) None else Some(schemas),
          responses =
            if (config.commonErrorResponses.isEmpty) None
            else Some(config.commonErrorResponses)
        )
      )
    )
  }

  def getOpenAPISchemaOfParameter(p: wvlet.airframe.surface.Parameter, seen: Set[Surface]): SchemaOrRef = {
    val schema = getOpenAPISchemaOfSurface(p.surface, seen)
    schema match {
      case s: Schema =>
        s.withDescription(p.findAnnotationOf[description].map(_.value))
      case _ =>
        schema
    }
  }

  private def getOrUpdateSchema(surface: Surface, factory: => SchemaOrRef): SchemaOrRef = {
    synchronized {
      schemaCache.get(surface) match {
        case Some(x) => x
        case None =>
          val v = factory
          schemaCache += surface -> v
          v
      }
    }
  }

  def getOpenAPISchemaOfSurface(s: Surface, seen: Set[Surface]): SchemaOrRef = {
    if (seen.contains(s)) {
      SchemaRef(`$ref` = s"#/components/schemas/${schemaName(s)}")
    } else {
      getOrUpdateSchema(
        s, {
          debug(s"Find Open API Schema of ${s}")
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
            case s if s == Surface.of[Instant] =>
              Schema(`type` = "string")
            case s if s == Surface.of[TimeUnit] =>
              Schema(`type` = "string")
            // Airframe specific types
            case s if s == Surface.of[Json] =>
              Schema(`type` = "object")
            case s if s == Surface.of[JSONValue] =>
              Schema(`type` = "object")
            case s if s == Surface.of[MsgPack] =>
              Schema(`type` = "string", format = Some("binary"))
            case s if s == Surface.of[Value] =>
              Schema(`type` = "object", format = Some("binary"))
            case s if s == Surface.of[DataSize] =>
              Schema(`type` = "string")
            case s if s == Surface.of[ElapsedTime] =>
              Schema(`type` = "string")
            case s if s == Surface.of[Count] =>
              Schema(`type` = "string")
            case s if s == Surface.of[ULID] =>
              Schema(`type` = "string")
            case o: OptionSurface =>
              getOpenAPISchemaOfSurface(o.elementSurface, seen + s)
            case s: Surface if Router.isFuture(s) =>
              getOpenAPISchemaOfSurface(Router.unwrapFuture(s), seen + s)
            case s: Surface if Router.isFinagleReader(s) =>
              getOpenAPISchemaOfSurface(s.typeArgs(0), seen + s)
            case r: Surface if Router.isHttpResponse(r) =>
              // Use just string if the response type is not given
              Schema(`type` = "string")
            case g: Surface
                if classOf[Map[_, _]]
                  .isAssignableFrom(g.rawType) && g
                  .typeArgs(0) == Primitive.String =>
              Schema(
                `type` = "object",
                additionalProperties = Some(
                  getOpenAPISchemaOfSurface(g.typeArgs(1), seen)
                )
              )
            case a: ArraySurface =>
              Schema(
                `type` = "array",
                items = Some(getOpenAPISchemaOfSurface(a.elementSurface, seen))
              )
            case s: Surface if s.isSeq =>
              Schema(
                `type` = "array",
                items = Some(
                  getOpenAPISchemaOfSurface(s.typeArgs.head, seen)
                )
              )
            case s: Surface if s.rawType == classOf[Either[_, _]] =>
              Schema(
                `type` = "array",
                items = Some(
                  OneOf(
                    oneOf = s.typeArgs.map(t => getOpenAPISchemaOfSurface(t, seen))
                  )
                )
              )
            case g: Surface if g.params.length > 0 =>
              // Use ListMap for preserving parameter orders
              val b = ListMap.newBuilder[String, SchemaOrRef]
              g.params.foreach { p =>
                b += p.name -> getOpenAPISchemaOfParameter(p, seen + g)
              }
              val properties = b.result()

              Schema(
                `type` = "object",
                description = g.findAnnotationOf[description].map(_.value()),
                required = requiredParams(g.params),
                properties = if (properties.isEmpty) None else Some(properties)
              )
            case s if s.isAlias =>
              getOpenAPISchemaOfSurface(s.dealias, seen + s)
            case other =>
              warn(s"Unknown type ${other.fullName}. Use string instead")
              Schema(
                `type` = "string",
                description = other.findAnnotationOf[description].map(_.value())
              )
          }
        }
      )
    }
  }

}
