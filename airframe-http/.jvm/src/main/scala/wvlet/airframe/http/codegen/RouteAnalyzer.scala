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

import wvlet.airframe.http.{HttpContext, HttpMessage, HttpRequest}
import wvlet.airframe.http.router.Route
import wvlet.airframe.surface.{CName, MethodParameter}
import scala.language.higherKinds

/**
  * Analyze a given HTTP Route, and build URL path strings, user-input arguments, and http client call arguments.
  */
object RouteAnalyzer {

  case class RouteAnalysisResult(
      // A path string generator code using string interpolation
      pathString: String,
      // User-input parameters for the client method
      userInputParameters: Seq[MethodParameter],
      pathOnlyParameters: Set[MethodParameter]
  ) {
    // http client call parameters, except parameters used for generating path strings
    val httpClientCallInputs: Seq[MethodParameter] =
      (userInputParameters.toSet -- pathOnlyParameters).toIndexedSeq
  }

  /**
    * airframe-http interface may contain HTTP specific parameters  (e.g., HttpRequest, HttpContext, etc.).
    * We need to remove these server-side only arguments to generate client methods.
    */
  private def isClientSideArg(x: MethodParameter): Boolean = {
    !classOf[HttpMessage.Request].isAssignableFrom(x.surface.rawType) &&
    !classOf[HttpRequest[_]].isAssignableFrom(x.surface.rawType) &&
    !classOf[HttpContext[_, _, F] forSome { type F[_] }]
      .isAssignableFrom(x.surface.rawType) &&
    x.surface.fullName != "com.twitter.finagle.http.Request"
  }

  def analyzeRoute(route: Route): RouteAnalysisResult = {
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
                .map { arg =>
                  (arg, arg.surface.params.find(nestedParam => CName(nestedParam.name) == varName))
                }
                .collectFirst { case (arg, Some(nestedParam)) =>
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

    RouteAnalysisResult(
      pathString = path,
      userInputParameters = clientSideArgs,
      pathOnlyArgs
    )
  }

}
