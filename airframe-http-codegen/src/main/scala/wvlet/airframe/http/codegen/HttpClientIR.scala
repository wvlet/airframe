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
import wvlet.airframe.http.Router.unwrapFuture
import wvlet.airframe.http.{HttpMethod, Router}
import wvlet.airframe.http.codegen.RouteAnalyzer.RouteAnalysisResult
import wvlet.airframe.http.codegen.client.HttpClientGenerator
import wvlet.airframe.http.router.Route
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.airframe.surface.{GenericSurface, HigherKindedTypeSurface, MethodParameter, Parameter, Surface}
import wvlet.log.LogSupport

/**
  * Generate an intermediate representation (IR) of Scala HTTP client code from a given airframe-http interface
  * definition (Router).
  *
  * This IR abstracts away the differences between Scala (Sync/Async clients) and Scala.js (Async + AJAX).
  */
object HttpClientIR extends LogSupport {

  // Intermediate representation (IR) of HTTP client code
  sealed trait ClientCodeIR

  case class ClientSourceDef(destPackageName: String, classDef: ClientClassDef) extends ClientCodeIR {
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
            loop(m.returnType) ++ m.typeArgs.flatMap(loop) ++ m.inputParameters
              .flatMap(loop)
          case _ =>
            Seq.empty
        }
      }

      def requireImports(surface: Surface): Boolean = {
        val importPackageName =
          resolveObjectName(surface.rawType.getName)
            .split("\\.")
            .dropRight(1)
            .mkString(".")
        // Primitive Scala collections can be found in scala.Predef. No need to include them
        !(importPackageName.isEmpty ||
          importPackageName == "java.lang" ||
          importPackageName == "scala.collection" ||
          importPackageName == "scala.collection.immutable" ||
          importPackageName == "wvlet.airframe.http" ||
          surface.isOption ||
          surface.isPrimitive ||
          // Within the same package
          importPackageName == destPackageName)
      }

      loop(classDef).filter(requireImports).distinct.sortBy(_.name)
    }

    private def resolveObjectName(fullName: String): String = {
      fullName.replaceAll("\\$", ".")
    }

    def importStatements: String = {
      imports
        .map(x => s"import ${resolveObjectName(x.rawType.getName)}")
        .mkString("\n")
    }

  }

  /**
    * Represents hierarchical API structures
    * @param packageLeafName
    * @param services
    * @param children
    */
  case class ClientServicePackages(
      packageLeafName: String,
      services: Seq[ClientServiceDef],
      children: Seq[ClientServicePackages]
  ) {
    def withChildren(newChildren: Seq[ClientServicePackages]): ClientServicePackages =
      ClientServicePackages(packageLeafName, services, newChildren)
  }

  case class ClientClassDef(clsName: String, services: Seq[ClientServiceDef]) extends ClientCodeIR {
    def toNestedPackages: ClientServicePackages = {
      def iter(
          packagePrefix: String,
          lst: Seq[(List[String], ClientServiceDef)]
      ): ClientServicePackages = {
        val (leafServices, remaining) = lst.partition(_._1.isEmpty)
        val node                      = ClientServicePackages(packagePrefix, leafServices.map(_._2), Seq.empty)
        val children =
          for ((prefix, lst) <- remaining.groupBy(_._1.head))
            yield {
              iter(prefix, lst.map(x => (x._1.tail, x._2)))
            }
        node.withChildren(children.toSeq)
      }

      iter("", services.map(x => (x.relativePackages, x)))
    }
  }

  case class ClientServiceDef(
      basePackageName: String,
      fullPackageName: String,
      serviceName: String,
      methods: Seq[ClientMethodDef]
  ) extends ClientCodeIR {
    def fullServiceName: String = s"${internalPackageName}.${serviceName}"
    def internalPackageName: String = {
      if (relativePackageName.isEmpty) {
        "internal"
      } else {
        s"internal.${relativePackageName}"
      }
    }

    def relativePackageName: String = {
      if (fullPackageName == basePackageName) {
        ""
      } else {
        fullPackageName.stripPrefix(s"${basePackageName}.")
      }
    }
    def relativePackages: List[String] = relativePackageName.split("\\.").toList
  }

  case class ClientRequestModelClassDef(name: String, parameter: Seq[Parameter]) {
    def code(isPrivate: Boolean = true) =
      s"${if (isPrivate) "private "
      else ""}case class ${name}(${parameter
        .map { p =>
          s"${p.name}: ${p.surface.fullName.replaceAll("\\$", ".")}"
        }
        .mkString(", ")})"
  }

  case class ClientMethodDef(
      httpMethod: String,
      isOpsRequest: Boolean,
      name: String,
      typeArgs: Seq[Surface],
      inputParameters: Seq[MethodParameter],
      clientCallParameters: Seq[String],
      returnType: Surface,
      path: String,
      // A case class definition for wrapping HTTP request parameters
      requestModelClassDef: Option[ClientRequestModelClassDef] = None
  ) extends ClientCodeIR {
    def typeArgString =
      typeArgs
        .map(arg => HttpClientGenerator.fullTypeNameOf(arg))
        .mkString(", ")
    def clientMethodName = {
      val methodName = httpMethod.toString.toLowerCase(Locale.ENGLISH)
      if (isOpsRequest) s"${methodName}Ops"
      else methodName
    }
    def requestModelClassType: String = {
      requestModelClassDef match {
        case Some(m) => m.name
        case None    => "Map[String, Any]"
      }
    }

    def grpcReturnType: Surface = {
      returnType.rawType match {
        case cl if classOf[Rx[_]].isAssignableFrom(cl) =>
          returnType.typeArgs(0)
        case _ => returnType
      }
    }

    def grpcClientStreamingArg: Option[MethodParameter] = {
      findGrpcClientStreamingArg(inputParameters)
    }

    def grpcClientStreamingRequestType: Surface = {
      grpcClientStreamingArg.get.surface.typeArgs(0)
    }

    def grpcMethodType: GrpcMethodType = {
      val isClientStreaming: Boolean = grpcClientStreamingArg.isDefined
      if (classOf[Rx[_]].isAssignableFrom(returnType.rawType)) {
        if (isClientStreaming)
          GrpcMethodType.BIDI_STREAMING
        else
          GrpcMethodType.SERVER_STREAMING
      } else {
        if (isClientStreaming)
          GrpcMethodType.CLIENT_STREAMING
        else
          GrpcMethodType.UNARY
      }
    }
  }

  private def findGrpcClientStreamingArg(inputParameters: Seq[MethodParameter]): Option[MethodParameter] = {
    inputParameters.find(x => classOf[Rx[_]].isAssignableFrom(x.surface.rawType))
  }

  sealed trait GrpcMethodType {
    def code: String
  }

  object GrpcMethodType {

    case object UNARY extends GrpcMethodType {
      override def code: String = s"io.grpc.MethodDescriptor.MethodType.UNARY"
    }

    case object SERVER_STREAMING extends GrpcMethodType {
      override def code: String =
        s"io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING"
    }

    case object CLIENT_STREAMING extends GrpcMethodType {
      override def code: String =
        s"io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING"
    }

    case object BIDI_STREAMING extends GrpcMethodType {
      override def code: String =
        s"io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING"
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
      // Use a API class name as is for the accessor objects
      val controllerName = controllerSurface.name
      ClientServiceDef(
        basePackageName = config.apiPackageName,
        fullPackageName = controllerSurface.rawType.getPackageName,
        serviceName = controllerName,
        routes.map(buildClientCall)
      )
    }

    // Create a method definition for each endpoint (Route)
    def buildClientCall(route: Route): ClientMethodDef = {
      val analysis: RouteAnalysisResult = RouteAnalyzer.analyzeRoute(route)

      val httpClientCallInputs = analysis.httpClientCallInputs
      val name                 = route.methodSurface.name

      val typeArgBuilder = Seq.newBuilder[Surface]

      def isPrimitive(s: Surface): Boolean =
        s.isPrimitive || (s.isOption && s.typeArgs.forall(_.isPrimitive))

      val primitiveOnlyInputs =
        httpClientCallInputs.nonEmpty && httpClientCallInputs.forall(x => isPrimitive(x.surface))

      val clientCallParams                                         = Seq.newBuilder[String]
      var requestModelClassDef: Option[ClientRequestModelClassDef] = None

      if (httpClientCallInputs.isEmpty) {
        if (route.method == HttpMethod.POST) {
          // For RPC calls without any input, embed an empty json
          clientCallParams += "Map.empty[String, Any]"
          typeArgBuilder += Surface.of[Map[String, Any]]
        } else {
          // Do not add any parameters for empty requests
        }
      } else if (httpClientCallInputs.size == 1 && !primitiveOnlyInputs && !route.isRPC) {
        // Unary Endpoint call
        httpClientCallInputs.headOption.map { x =>
          clientCallParams += x.name
          typeArgBuilder += x.surface
        }
      } else if (primitiveOnlyInputs) {
        // Primitive values (or its Option) cannot be represented in JSON, so we need to wrap them with a map
        val params = Seq.newBuilder[String]
        httpClientCallInputs.foreach { x =>
          params += s""""${x.name}" -> ${x.name}"""
        }
        clientCallParams += s"Map(${params.result().mkString(", ")})"
        typeArgBuilder += Surface.of[Map[String, Any]]
      } else {
        // For complex request arguments, create a type-safe model class to wrap the request parameters.
        // This is because if we use Map[String, Any] for request parameters, we cannot resolve the actual Surface of
        // the complex object at compile-time in Scala.js.
        val requestModelClassName = s"__${name}_request"
        // Create Parameter objects for the model class surface
        val requestModelClassParamSurfaces: Seq[Parameter] =
          for ((p, i) <- httpClientCallInputs.zipWithIndex) yield {
            new Parameter {
              override def index: Int                   = p.index
              override def name: String                 = p.name
              override def surface: Surface             = p.surface
              override def isRequired: Boolean          = p.isRequired
              override def isSecret: Boolean            = p.isSecret
              override def get(x: Any): Any             = p.get(x)
              override def getDefaultValue: Option[Any] = p.getDefaultValue
            }
          }

        if (findGrpcClientStreamingArg(route.methodSurface.args).isEmpty) {
          requestModelClassDef = Some(ClientRequestModelClassDef(requestModelClassName, requestModelClassParamSurfaces))
        }

        clientCallParams += s"${requestModelClassName}(${requestModelClassParamSurfaces
          .map { p =>
            s"${p.name} = ${p.name}"
          }
          .mkString(", ")})"

        // Create a model class surface for defining http request object parameter
        val requestModelClassSurface =
          new Surface {
            override def rawType: Class[_]      = classOf[Any]
            override def typeArgs: Seq[Surface] = Seq.empty
            override def params: Seq[Parameter] = requestModelClassParamSurfaces
            override def name: String           = requestModelClassName
            override def fullName: String       = requestModelClassName
            override def isOption: Boolean      = false
            override def isAlias: Boolean       = false
            override def isPrimitive: Boolean   = false
          }
        typeArgBuilder += requestModelClassSurface
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
        returnType = unwrapFuture(route.returnTypeSurface),
        requestModelClassDef = requestModelClassDef
      )
    }

    ClientSourceDef(
      destPackageName = config.targetPackageName,
      classDef = buildClassDef
    )
  }
}
