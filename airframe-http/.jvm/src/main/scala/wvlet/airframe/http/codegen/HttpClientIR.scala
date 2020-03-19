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
import java.util.Locale

import wvlet.airframe.http.Router
import wvlet.airframe.http.codegen.RouteAnalyzer.RouteAnalysisResult
import wvlet.airframe.http.router.Route
import wvlet.airframe.surface.{HigherKindedTypeSurface, MethodParameter, Parameter, Surface}
import wvlet.log.LogSupport

/**
  * Generate an intermediate represenatation (IR) of Scala HTTP client code from a given airframe-http interface definition (Router).
  *
  * This IR abstracts away the differences between Scala (Sync/Async clients) and Scala.js (Async + AJAX).
  */
object HttpClientIR extends LogSupport {

  // Intermediate representation (IR) of HTTP client code
  sealed trait ClientCodeIR
  case class ClientSourceDef(packageName: String, classDef: ClientClassDef) extends ClientCodeIR {
    def imports: Seq[Surface] = {
      // Collect all Surfaces used in the generated code
      def loop(s: Any): Seq[Surface] = {
        s match {
          case s: Surface =>
            Seq(s) ++ s.typeArgs.flatMap(loop)
          case m: Parameter =>
            loop(m.surface)
          case c: ClientClassDef   => c.services.flatMap(loop)
          case x: ClientServiceDef => x.methods.flatMap(loop)
          case m: ClientMethodDef =>
            loop(m.returnType) ++ m.inputParameters.flatMap(loop)
          case _ =>
            Seq.empty
        }
      }

      def requireImports(surface: Surface): Boolean = {
        val fullName = surface.fullName
        // Primitive Scala collections can be found in scala.Predef. No need to include them
        !(surface.rawType.getPackageName == "scala.collection" ||
          fullName.startsWith("wvlet.airframe.http.") ||
          surface.isPrimitive ||
          // Within the same package
          surface.rawType.getPackageName == packageName)
      }

      loop(classDef).filter(requireImports).distinct.sortBy(_.name)
    }
  }
  case class ClientClassDef(clsName: String, services: Seq[ClientServiceDef])     extends ClientCodeIR
  case class ClientServiceDef(serviceName: String, methods: Seq[ClientMethodDef]) extends ClientCodeIR
  case class ClientMethodDef(
      httpMethod: String,
      isOpsRequest: Boolean,
      name: String,
      typeArgs: Seq[Surface],
      inputParameters: Seq[MethodParameter],
      clientCallParameters: Seq[MethodParameter],
      returnType: Surface,
      path: String
  ) extends ClientCodeIR {

    def resolveLeafReturnTypeName: String = {
      logger.info(returnType)
      returnType match {
        case f if f.name.startsWith("Future[") || f.name.startsWith("Object[") || f.name.startsWith("F[") =>
          returnType.typeArgs.mkString(", ")
        case _ =>
          returnType.name
      }
    }
  }

  private case class PathVariableParam(name: String, param: MethodParameter)

  /**
    * Building an intermediate representation of the client code
    */
  def buildIR(router: Router, config: HttpClientGeneratorConfig): ClientSourceDef = {

    // Build service clients for controllers
    def buildClassDef: ClientClassDef = {
      val services = for ((controllerSurface, routes) <- router.routes.groupBy(_.controllerSurface)) yield {
        buildService(controllerSurface, routes)
      }

      ClientClassDef(
        clsName = config.clientType.defaultClassName,
        services = services.toIndexedSeq
      )
    }

    def buildService(controllerSurface: Surface, routes: Seq[Route]): ClientServiceDef = {
      // Use a lowercase word for the accessor objects
      val controllerName =
        controllerSurface.name.substring(0, 1).toLowerCase(Locale.ENGLISH) + controllerSurface.name.substring(1)

      ClientServiceDef(serviceName = controllerName, routes.map(buildClientCall))
    }

    // Create a method definition for each endpoint (Route)
    def buildClientCall(route: Route): ClientMethodDef = {
      val analysis: RouteAnalysisResult = RouteAnalyzer.analyzeRoute(route)

      val httpClientCallInputs = analysis.httpClientCallInputs
      val name                 = route.methodSurface.name

      val typeArgBuilder = Seq.newBuilder[Surface]

      if (httpClientCallInputs.size >= 2) {
        throw new IllegalStateException(s"HttpClient doesn't support multiple object inputs: ${route}")
      }
      httpClientCallInputs.headOption.map { x => typeArgBuilder += x.surface }
      typeArgBuilder += route.returnTypeSurface

      ClientMethodDef(
        httpMethod = route.method,
        isOpsRequest = httpClientCallInputs.nonEmpty,
        name = name,
        typeArgs = typeArgBuilder.result(),
        inputParameters = analysis.userInputParameters,
        clientCallParameters = httpClientCallInputs,
        path = analysis.pathString,
        returnType = route.returnTypeSurface
      )
    }

    ClientSourceDef(
      packageName = config.targetPackageName,
      classDef = buildClassDef
    )
  }

}
