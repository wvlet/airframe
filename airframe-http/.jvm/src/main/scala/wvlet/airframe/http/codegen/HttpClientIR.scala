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

import wvlet.airframe.http.{HttpMethod, Router}
import wvlet.airframe.http.codegen.RouteAnalyzer.RouteAnalysisResult
import wvlet.airframe.http.router.Route
import wvlet.airframe.surface.{GenericSurface, HigherKindedTypeSurface, MethodParameter, Parameter, Surface}
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
    private def imports: Seq[Surface] = {
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
            loop(m.returnType) ++ m.typeArgs.flatMap(loop) ++ m.inputParameters.flatMap(loop)
          case _ =>
            Seq.empty
        }
      }

      def requireImports(surface: Surface): Boolean = {
        val importPackageName =
          resolveObjectName(surface.rawType.getName).split("\\.").dropRight(1).mkString(".")
        // Primitive Scala collections can be found in scala.Predef. No need to include them
        !(importPackageName.isEmpty ||
          importPackageName == "java.lang" ||
          importPackageName == "scala.collection" ||
          importPackageName == "wvlet.airframe.http" ||
          surface.isPrimitive ||
          // Within the same package
          importPackageName == packageName)
      }

      loop(classDef).filter(requireImports).distinct.sortBy(_.name)
    }

    private def resolveObjectName(fullName: String): String = {
      fullName.replaceAll("\\$", ".")
    }

    def importStatements: String = {
      imports.map(x => s"import ${resolveObjectName(x.rawType.getName)}").mkString("\n")
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
      clientCallParameters: Seq[String],
      returnType: Surface,
      path: String
  ) extends ClientCodeIR {
    def typeArgString = typeArgs.map(_.name).mkString(", ")
    def clientMethodName = {
      val methodName = httpMethod.toString.toLowerCase(Locale.ENGLISH)
      if (isOpsRequest) s"${methodName}Ops" else methodName
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

      def isPrimitive(s: Surface): Boolean = s.isPrimitive || (s.isOption && s.typeArgs.forall(_.isPrimitive))

      val hasPrimitiveInputs =
        if (httpClientCallInputs.nonEmpty && httpClientCallInputs.forall(x => isPrimitive(x.surface))) {
          true
        } else if (httpClientCallInputs.size >= 2) {
          throw new IllegalStateException(s"HttpClient doesn't support multiple non-primitive object inputs: ${route}")
        } else {
          false
        }

      val clientCallParams = Seq.newBuilder[String]

      if (hasPrimitiveInputs) {
        // Primitive values (or its Option) cannot be represented in JSON, so we need to wrap them with a map
        val params = Seq.newBuilder[String]
        httpClientCallInputs.foreach { x => params += s""""${x.name}" -> ${x.name}""" }
        clientCallParams += s"Map(${params.result.mkString(", ")})"
        typeArgBuilder += Surface.of[Map[String, Any]]
      } else {
        if (httpClientCallInputs.isEmpty && route.method == HttpMethod.POST) {
          // For RPC calls without any input, embed an empty json
          clientCallParams += "Map.empty"
          typeArgBuilder += Surface.of[Map[String, Any]]
        } else {
          httpClientCallInputs.headOption.map { x =>
            clientCallParams += x.name
            typeArgBuilder += x.surface
          }
        }
      }
      typeArgBuilder += unwrapFuture(route.returnTypeSurface)
      val typeArgs = typeArgBuilder.result()

      ClientMethodDef(
        httpMethod = route.method,
        isOpsRequest = typeArgs.size > 1,
        name = name,
        typeArgs = typeArgs,
        inputParameters = analysis.userInputParameters,
        clientCallParameters = clientCallParams.result(),
        path = analysis.pathString,
        returnType = unwrapFuture(route.returnTypeSurface)
      )
    }

    ClientSourceDef(
      packageName = config.targetPackageName,
      classDef = buildClassDef
    )
  }

  private def unwrapFuture(s: Surface): Surface = {
    s match {
      case h: HigherKindedTypeSurface
          if h.typeArgs.size == 1 && h.name == "F" => // Only support 'F' for tagless-final pattern
        h.typeArgs.head
      case s: Surface
          if s.rawType == classOf[scala.concurrent.Future[_]] || s.rawType.getName == "com.twitter.util.Future" =>
        s.typeArgs.head
      case _ =>
        s
    }
  }

}
