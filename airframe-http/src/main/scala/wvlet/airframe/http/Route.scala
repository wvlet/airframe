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
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.json.JSON
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.airframe.surface.{Surface, Zero}
import wvlet.log.LogSupport

import scala.util.Try

/**
  * A mapping from an HTTP route to a method with Endpoint annotation
  */
case class Route(controllerSurface: Surface, method: HttpMethod, path: String, methodSurface: ReflectMethodSurface)
    extends LogSupport {
  require(
    path.startsWith("/"),
    s"Invalid route path: ${path}. EndPoint path must start with a slash (/) in ${methodSurface.owner.name}:${methodSurface.name}")

  override def toString =
    s"${method} ${path} -> ${methodSurface.name}(${methodSurface.args
      .map(x => s"${x.name}:${x.surface}").mkString(", ")}): ${methodSurface.returnType}"

  val pathComponents: IndexedSeq[String] = {
    path
      .substring(1)
      .split("/")
      .toIndexedSeq
  }

  def returnTypeSurface: Surface = methodSurface.returnType

  /**
    * Find a corresponding controller and call the matching methods
    *
    * @param request
    * @return
    */
  def buildControllerMethodArgs[Req](controller: Any, request: Req, params: Map[String, String])(
      implicit adapter: HttpRequestAdapter[Req]): Seq[Any] = {
    // Collect URL query parameters and other parameters embedded inside URL.
    val requestParams: Map[String, String] = adapter.queryOf(request) ++ params
    lazy val queryParamMsgpack             = Route.stringMapCodec.toMsgPack(requestParams)

    // Build the function arguments
    val methodArgs: Seq[Any] =
      for (arg <- methodSurface.args) yield {
        arg.surface.rawType match {
          case cl if classOf[HttpRequest[_]].isAssignableFrom(cl) =>
            // Bind the current http request instance
            adapter.httpRequestOf(request)
          case cl if adapter.requestType.isAssignableFrom(cl) =>
            request
          case _ =>
            // Build from the string value in the request params
            val argCodec = MessageCodecFactory.defaultFactory.of(arg.surface)
            val v: Option[Any] = requestParams.get(arg.name) match {
              case Some(paramValue) =>
                // Pass the String parameter to the method argument
                argCodec.unpackMsgPack(StringCodec.toMsgPack(paramValue))
              case None =>
                if (adapter.methodOf(request) == HttpMethod.GET) {
                  // Build the method argument instance from the query strings for GET requests
                  argCodec.unpackMsgPack(queryParamMsgpack)
                } else {
                  // Build the method argument instance from the content body for non GET requests
                  val contentBytes = adapter.contentBytesOf(request)

                  if (contentBytes.nonEmpty) {
                    val msgpack =
                      adapter.contentTypeOf(request).map(_.split(";")(0)) match {
                        case Some("application/x-msgpack") =>
                          contentBytes
                        case Some("application/json") =>
                          // JSON -> msgpack
                          JSONCodec.toMsgPack(JSON.parse(contentBytes))
                        case _ =>
                          // Try parsing as JSON first
                          Try(JSON.parse(contentBytes))
                            .map { jsonValue =>
                              JSONCodec.toMsgPack(jsonValue)
                            }
                            .getOrElse {
                              // If parsing as JSON fails, treat the content body as a regular string
                              StringCodec.toMsgPack(adapter.contentStringOf(request))
                            }
                      }
                    argCodec.unpackMsgPack(msgpack)
                  } else {
                    // Return the method default argument if exists
                    arg.getMethodArgDefaultValue(controller)
                  }
                }
            }
            // If mapping fails, use the zero value
            v.getOrElse(Zero.zeroOf(arg.surface))
        }
      }
    trace(s"(${methodSurface.args.mkString(", ")}) <=  [${methodArgs.mkString(", ")}]")
    methodArgs
  }

  def call(controller: Any, methodArgs: Seq[Any]): Any = {
    methodSurface.call(controller, methodArgs: _*)
  }

  def call[Req: HttpRequestAdapter](controllerProvider: ControllerProvider,
                                    request: Req,
                                    params: Map[String, String]): Option[Any] = {
    controllerProvider.findController(controllerSurface).map { controller =>
      call(controller, buildControllerMethodArgs(controller, request, params))
    }
  }
}

object Route {
  private[Route] val stringMapCodec = MessageCodec.of[Map[String, String]]
}
