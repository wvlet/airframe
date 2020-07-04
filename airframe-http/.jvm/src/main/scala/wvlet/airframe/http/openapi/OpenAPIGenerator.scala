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
import wvlet.log.LogSupport

/**
  */
object OpenAPIGenerator extends LogSupport {
  import OpenAPI._

  def fromRouter(name: String, version: String, router: Router): OpenAPI = {
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

      val method = route.method.toLowerCase(Locale.ENGLISH)
      val pathItem = PathItem(
        summary = route.methodSurface.name,
        // TOODO Use @RPC(description = ???) or Scaladoc comment
        description = route.methodSurface.name,
        operationId = route.methodSurface.name,
        responses = Map(
          "200" ->
            Response(
              description = s"RPC response",
              content = Map(
                "application/json" -> MediaType(
                  schema = SchemaRef(s"#/components/schemas/${route.methodSurface.returnType.fullName}")
                ),
                "application/x-msgpack" -> MediaType(
                  schema = SchemaRef(s"#/components/schemas/${route.methodSurface.returnType.fullName}")
                )
              )
            ),
          "400" -> ResponseRef("#/components/responses/400"),
          "500" -> ResponseRef("#/components/responses/500"),
          "503" -> ResponseRef("#/components/responses/503")
        )
      )
      path -> Map(method -> pathItem)
    }

    OpenAPI(
      info = Info(
        title = name,
        version = version
      ),
      paths = paths.toMap
    )
  }
}
