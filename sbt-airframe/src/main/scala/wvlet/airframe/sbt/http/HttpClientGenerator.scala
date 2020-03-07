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
package wvlet.airframe.sbt.http
import java.util.Locale

import wvlet.airframe.http.router.Route
import wvlet.airframe.http.{HttpRequest, Router}
import wvlet.airframe.surface.{CName, MethodParameter, MethodSurface, Surface}
import wvlet.log.LogSupport

/**
  *
  */
object HttpClientGenerator extends LogSupport {

  private case class PathVariableParam(name: String, param: MethodParameter)

  private case class ClientCall(
      route: Route,
      pathString: String,
      inputParameters: Seq[MethodParameter],
      pathOnlyParameters: Set[MethodParameter]
  ) {

    def generateCode: Seq[String] = {
      val clientMethodArgBuilder = Seq.newBuilder[String]
      inputParameters.foreach { x => clientMethodArgBuilder += s"${x.name}: ${x.surface.name}" }
      clientMethodArgBuilder += "requestFilter: Req => Req = identity"

      val remainingParams = inputParameters.toSet -- pathOnlyParameters

      val clientCallBuilder = Seq.newBuilder[String]
      clientCallBuilder += s"""resourcePath = s"${pathString}""""
      remainingParams.foreach { x => clientCallBuilder += x.name }
      clientCallBuilder += s"requestFilter = requestFilter"

      val returnType = route.returnTypeSurface.name

      val methodName = {
        val name = route.method.name().toLowerCase(Locale.ENGLISH)
        if (remainingParams.isEmpty) {
          s"${name}[${returnType}]"
        } else {
          val requestObj = remainingParams.head.surface.name
          s"${name}Ops[${requestObj}, ${returnType}]"
        }
      }

      val lines = Seq.newBuilder[String]

      lines += s"def ${route.methodSurface.name}(${clientMethodArgBuilder.result().mkString(", ")}): F[${returnType}] = {"
      lines += s"  client.${methodName}(${clientCallBuilder.result().mkString(", ")})"
      lines += "}"

      lines.result()
    }
  }

  private def isClientSideArg(x: MethodParameter): Boolean = {
    !classOf[HttpRequest[_]].isAssignableFrom(x.surface.rawType) &&
    !x.surface.fullName.startsWith("wvlet.airframe.http.HttpContext") &&
    x.surface.fullName != "com.twitter.finagle.http.Request"
  }

  /**
    * Extract http client method arguments by removing server-side only arguments (e.g., HttpRequest, HttpContext, etc.)
    */
  private def buildClientCall(route: Route): ClientCall = {
    // Filter server-side only arguments
    val clientSideArgs = route.methodSurface.args.filter(isClientSideArg)

    var pathOnlyArgs = Set.empty[MethodParameter]

    /**
      * Resolve path variables (e.g., id in /v1/query/:id) using the client interface arguments.
      * For example, if the input resource (e.g., QueryRequest(id:String)) contains parameters matching path variables,
      * use it when building path strings.
      */
    val path = {
      val pathBuilder = Seq.newBuilder[String]
      route.pathComponents.map {
        case c if c.startsWith(":") || c.startsWith("*") =>
          val varName = CName(c.substring(1))
          // If the request argument contains path variables, use it.
          clientSideArgs.find(p => CName(p.name) == varName) match {
            case Some(p) =>
              // Find the path variable in the input arguments
              pathBuilder += s"$${${p.name}}"
              pathOnlyArgs += p
            case None =>
              // Find the path variable in the nested parameters
              clientSideArgs
                .map { arg => (arg, arg.surface.params.find(nestedParam => CName(nestedParam.name) == varName)) }
                .collectFirst {
                  case (arg, Some(nestedParam)) =>
                    pathBuilder += s"$${${arg.name}.${nestedParam.name}}"
                }
                .getOrElse {
                  // If the request argument has no path variable, add it to the function interface
                  throw new IllegalArgumentException(
                    s"Missing parameter ${varName} in the input ${clientSideArgs}"
                  )
                }
          }
        case other =>
          pathBuilder += other
      }
      "/" + pathBuilder.result().mkString("/")
    }

    ClientCall(
      route = route,
      pathString = path,
      inputParameters = clientSideArgs,
      pathOnlyArgs
    )
  }

  def findImportedClases(router: Router): Seq[Surface] = {
    val importedClasses = Set.newBuilder[Surface]

    def add(surface: Surface): Unit = {
      val fullName = surface.fullName
      if (!(fullName.startsWith("scala.") || fullName.startsWith("wvlet.airframe.http.") || surface.isPrimitive)) {
        importedClasses += surface
      }
    }

    def loop(x: Any): Unit = x match {
      case s: Surface =>
        add(s)
        s.typeArgs.foreach(loop)
      case m: MethodSurface =>
        m.args.foreach(loop)
      case p: MethodParameter =>
        loop(p.surface)
      case _ =>
    }

    router.routes.foreach { r =>
      loop(r.returnTypeSurface)
      loop(r.methodSurface)
    }

    importedClasses.result().toSeq.sortBy(_.fullName)
  }

  def generateHttpClient(router: Router, targetPackage: Option[String] = None): String = {

    val lines = Seq.newBuilder[String]

    val importedClasses = findImportedClases(router)
    debug(importedClasses.map(_.rawType.getName).mkString("\n"))

    for ((controllerSurface, routes) <- router.routes.groupBy(_.controllerSurface)) {

      // Use a lowercase word for the accessor objects
      val controllerName =
        controllerSurface.name.substring(0, 1).toLowerCase(Locale.ENGLISH) + controllerSurface.name.substring(1)

      lines += s"object ${controllerName} {"
      for (r <- routes) {
        val clientCall = buildClientCall(r)
        lines ++= clientCall.generateCode.map(x => s"  ${x}")
      }
      lines += "}"
    }
    val methods = lines.result().map(x => s"  ${x}").mkString("\n")

    val pkg = targetPackage.getOrElse("generated")
    val code = s"""
                  |package ${pkg}
                  |
                  |import wvlet.airframe.http._
                  |${importedClasses.map(x => s"import ${x.rawType.getName}").mkString("\n")}
                  |
                  |class ServiceClient[F[_], Req, Resp](private val client:HttpClient[F, Req, Resp]) {
                  |  def getClient: HttpClient[F, Req, Resp] = client
                  |${methods}
                  |}
                  |"""
    /**EndMarker*/ .stripMargin.stripMargin

    info(code)
    code
  }

}
