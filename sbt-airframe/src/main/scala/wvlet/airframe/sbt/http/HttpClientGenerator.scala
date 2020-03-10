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
import wvlet.airframe.http.{HttpMethod, HttpRequest, Router}
import wvlet.airframe.surface.{CName, MethodParameter, Surface}
import wvlet.log.LogSupport

/**
  *
  */
object HttpClientGenerator extends LogSupport {

  // Intermediate representation (IR) of HTTP client code
  sealed trait ClientCodeIR
  case class ClientSourceDef(packageName: String, classDef: ClientClassDef) extends ClientCodeIR {
    def imports: Seq[Surface] = {
      // Collect all Surfaces used in the generated code
      def loop(s: Any): Seq[Surface] = {
        s match {
          case c: ClientClassDef   => c.services.flatMap(loop)
          case x: ClientServiceDef => x.methods.flatMap(loop)
          case m: ClientMethodDef  => Seq(m.returnType) ++ m.inputParameters.map(_.surface)
        }
      }

      def requireImports(surface: Surface): Boolean = {
        val fullName = surface.fullName
        !(fullName.startsWith("scala.") || fullName.startsWith("wvlet.airframe.http.") || surface.isPrimitive)
      }

      loop(classDef).filter(requireImports).distinct
    }
  }
  case class ClientClassDef(clsName: String, services: Seq[ClientServiceDef])     extends ClientCodeIR
  case class ClientServiceDef(serviceName: String, methods: Seq[ClientMethodDef]) extends ClientCodeIR
  case class ClientMethodDef(
      httpMethod: HttpMethod,
      isOpsRequest: Boolean,
      name: String,
      typeArgs: Seq[Surface],
      inputParameters: Seq[MethodParameter],
      clientCallParameters: Seq[MethodParameter],
      returnType: Surface,
      path: String
  ) extends ClientCodeIR

  private case class PathVariableParam(name: String, param: MethodParameter)

  case class ClientBuilderConfig(
      packageName: String = "generated",
      className: String = "ServiceClient"
  )

  def generateHttpClient(router: Router, config: ClientBuilderConfig = ClientBuilderConfig()): String = {
    val ir   = buildIR(router, config)
    val code = generateScalaCode(ir)
    debug(code)
    code
  }

  /**
    * Building an intermediate representation of the client code
    */
  def buildIR(router: Router, config: ClientBuilderConfig): ClientSourceDef = {

    // Build service clients for controllers
    def buildClassDef: ClientClassDef = {
      val services = for ((controllerSurface, routes) <- router.routes.groupBy(_.controllerSurface)) yield {
        buildService(controllerSurface, routes)
      }

      ClientClassDef(
        clsName = config.className,
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

      val analysis: RouteAnalysis = analyzeRoute(route)

      val remainingParams = analysis.inputParameters.toSet -- analysis.pathOnlyParameters
      val name            = route.methodSurface.name

      val typeArgBuilder = Seq.newBuilder[Surface]

      remainingParams.headOption.map { x => typeArgBuilder += x.surface }
      typeArgBuilder += route.returnTypeSurface

      ClientMethodDef(
        httpMethod = route.method,
        isOpsRequest = remainingParams.nonEmpty,
        name = name,
        typeArgs = typeArgBuilder.result(),
        inputParameters = analysis.inputParameters,
        clientCallParameters = remainingParams.toSeq,
        path = analysis.pathString,
        returnType = route.returnTypeSurface
      )
    }

    ClientSourceDef(
      packageName = config.packageName,
      classDef = buildClassDef
    )
  }

  private case class RouteAnalysis(
      pathString: String,
      inputParameters: Seq[MethodParameter],
      pathOnlyParameters: Set[MethodParameter]
  )

  /**
    * airframe-http interface may contain HTTP specific parameters  (e.g., HttpRequest, HttpContext, etc.).
    * We need to remove these server-side only arguments to generate client methods.
    */
  private def isClientSideArg(x: MethodParameter): Boolean = {
    !classOf[HttpRequest[_]].isAssignableFrom(x.surface.rawType) &&
    !x.surface.fullName.startsWith("wvlet.airframe.http.HttpContext") &&
    x.surface.fullName != "com.twitter.finagle.http.Request"
  }

  private def analyzeRoute(route: Route): RouteAnalysis = {
    // Filter server-side only arguments
    val clientSideArgs = route.methodSurface.args.filter(isClientSideArg)

    var pathOnlyArgs = Set.empty[MethodParameter]

    /**
      * Resolve path variables (e.g., id in /v1/query/:id) using the client interface arguments.
      * For example, if the input resource (e.g., QueryRequest(id:String)) contains parameters matching the path variables,
      * use them when building path strings.
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

    RouteAnalysis(
      pathString = path,
      inputParameters = clientSideArgs,
      pathOnlyArgs
    )
  }

  private def header(packageName: String): String = {
    s"""/**
        | * === DO NOT EDIT THIS FILE ===
        | * This code is generated by sbt-airframe plugin.
        | */
        |package ${packageName}""".stripMargin
  }

  private def indent(body: String, level: Int = 1): String = {
    body
      .split("\n").map { x => s"${"  " * level}${x}" }
      .mkString("\n")
  }

  def generateScalaCode(src: ClientSourceDef): String = {
    def code =
      s"""${header(src.packageName)}
         |
         |import wvlet.airframe.http._
         |${src.imports.map(x => s"import ${x.rawType.getName}").mkString("\n")}
         |
         |${cls}""".stripMargin

    def cls: String =
      s"""class ${src.classDef.clsName}[F[_], Req, Resp](private val client: HttpClient[F, Req, Resp]) extends AutoCloseable {
         |  override def close(): Unit = { client.close() }
         |  def getClient: HttpClient[F, Req, Resp] = client
         |${indent(clsBody)}
         |}
         |""".stripMargin

    def clsBody: String = {
      src.classDef.services
        .map { svc =>
          s"""object ${svc.serviceName} {
             |${indent(serviceBody(svc))}
             |}""".stripMargin
        }.mkString("\n")
    }

    def serviceBody(svc: ClientServiceDef): String = {
      svc.methods
        .map { m =>
          val httpMethodName       = m.httpMethod.toString.toLowerCase(Locale.ENGLISH)
          val httpClientMethodName = if (m.isOpsRequest) s"${httpMethodName}Ops" else httpMethodName

          val inputArgs =
            m.inputParameters.map(x => s"${x.name}: ${x.surface.name}") ++ Seq("requestFilter: Req => Req = identity")

          val sendRequestArgs = Seq.newBuilder[String]
          sendRequestArgs += s"""resourcePath = s"${m.path}""""
          sendRequestArgs ++= m.clientCallParameters.map(x => s"${x.name}")
          sendRequestArgs += "requestFilter = requestFilter"

          s"""def ${m.name}(${inputArgs.mkString(", ")}): F[${m.returnType.name}] = {
             |  client.${httpClientMethodName}[${m.typeArgs.map(_.name).mkString(", ")}](${sendRequestArgs.result
               .mkString(", ")})
             |}""".stripMargin
        }.mkString("\n")
    }

    code
  }

  def generateScalaJSCode(src: ClientSourceDef): String = {
    def code: String =
      s"""${header(src.packageName)}
         |
         |import wvlet.airframe.http.js.HttpClient
         |
         |""".stripMargin

    def cls: String = {
      s"""object ${src.classDef.clsName} {
         |
         |}
         |""".stripMargin
    }

    def clsBody: String = {
      ""
    }

    code
  }
}
