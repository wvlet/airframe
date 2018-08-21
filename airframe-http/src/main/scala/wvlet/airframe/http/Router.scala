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
package wvlet.airframe.http

import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.codec.{JSONCodec, MessageCodec, ParamListCodec}
import wvlet.log.LogSupport
import wvlet.surface
import wvlet.surface.{Surface, Zero}
import wvlet.surface.reflect._

import scala.reflect.runtime.{universe => ru}

/**
  * Provides mapping from HTTP requests to controller methods (= Route)
  * @param routes
  */
class Router(val routes: Seq[Route]) {

  def findRoute(request: HttpRequest): Option[Route] = {
    routes
      .find { r =>
        r.method == request.method &&
        r.pathComponents.length == request.pathComponents.length &&
        request.path.startsWith(r.pathPrefix)
      }
  }

  /**
    * Add methods annotated with @Endpoint to the routing table
    */
  def add[Controller: ru.TypeTag]: Router = {
    // Import ReflectSurface to find method annotations (Endpoint)
    import wvlet.surface.reflect._

    // Check prefix
    val serviceSurface = surface.of[Controller]
    val prefixPath =
      serviceSurface
        .findAnnotationOf[Endpoint]
        .map(_.path())
        .getOrElse("")

    val newRoutes =
      surface
        .methodsOf[Controller]
        .map(m => (m, m.findAnnotationOf[Endpoint]))
        .collect {
          case (m: ReflectMethodSurface, Some(endPoint)) =>
            Route(serviceSurface, endPoint.method(), prefixPath + endPoint.path(), m)
        }

    new Router(routes ++ newRoutes)
  }
}

object Router {
  def of[Controller: ru.TypeTag]: Router = apply().add[Controller]
  def apply(): Router                    = new Router(Seq.empty)
}

case class Route(serviceSurface: Surface, method: HttpMethod, path: String, methodSurface: ReflectMethodSurface)
    extends LogSupport {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. EndPoint path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}")

  val pathComponents: IndexedSeq[String] = {
    path
      .substring(1)
      .split("/")
      .toIndexedSeq
  }

  lazy val pathPrefix: String = {
    "/" +
      pathComponents
        .takeWhile(!_.startsWith(":"))
        .mkString("/")
  }

  /**
    * Extracting path parameter values. For example, /user/:id with /user/1 gives { id -> 1 }
    */
  private def extractPathParams(request: HttpRequest): Map[String, String] = {
    val pathParams = (for ((elem, actual) <- pathComponents.zip(request.pathComponents) if elem.startsWith(":")) yield {
      elem.substring(1) -> actual
    }).toMap[String, String]
    pathParams
  }

  /**
    * Find a corresponding controller and call the matching methods
    * @param controllerProvider
    * @param request
    * @return
    */
  def call(controllerProvider: ControllerProvider, request: HttpRequest): Option[Any] = {
    // Collect URL query parameters and other parameteres embedded inside URL.
    val requestParams = request.query ++ extractPathParams(request)

    // Build the function arguments
    val methodArgs: Seq[Any] =
      for (arg <- methodSurface.args) yield {
        arg.surface.rawType match {
          case cl if classOf[HttpRequest].isAssignableFrom(cl) =>
            // Bind the current http request instance
            request
          case _ =>
            // Build from the string value in the request params
            val argCodec = MessageCodec.default.of(arg.surface)
            val v: Option[Any] = requestParams.get(arg.name) match {
              case Some(paramValue) =>
                // String parameter to the method argument
                argCodec.unpackBytes(StringCodec.packToBytes(paramValue))
              case None =>
                // Build from the content body
                val content = request.contentString
                if (content.nonEmpty) {
                  // JSON -> msgpack -> argument
                  val msgpack = JSONCodec.packToBytes(content)
                  argCodec.unpackBytes(msgpack)
                } else {
                  // Rerturn the method default argument if exists
                  arg.getDefaultValue
                }
            }
            // If mapping fails, use the zero value
            v.getOrElse(Zero.zeroOf(arg.surface))
        }
      }
    trace(s"(${methodSurface.args.mkString(", ")}) <=  [${methodArgs.mkString(", ")}]")

    controllerProvider.find(serviceSurface).map { serviceObj =>
      methodSurface.call(serviceObj, methodArgs: _*)
    }
  }
}
