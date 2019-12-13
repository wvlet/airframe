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
package wvlet.airframe.http.router

import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.{HttpContext, HttpMethod, HttpRequest, HttpRequestAdapter}
import wvlet.airframe.json.JSON
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.airframe.surface.{OptionSurface, Zero}
import wvlet.log.LogSupport
import scala.language.higherKinds

import scala.util.Try

/**
  * Mapping HTTP requests to method call arguments
  */
object HttpRequestMapper extends LogSupport {
  private val stringMapCodec = MessageCodec.of[Map[String, String]]

  def buildControllerMethodArgs[Req, Resp, F[_]](
      // This instance is necessary to retrieve the default method argument values
      controller: Any,
      // The target method surface to call
      methodSurface: ReflectMethodSurface,
      request: Req,
      context: HttpContext[Req, Resp, F],
      // Additional parameters
      params: Map[String, String]
  )(
      implicit adapter: HttpRequestAdapter[Req]
  ): Seq[Any] = {
    // Collect URL query parameters and other parameters embedded inside URL.
    val requestParams: Map[String, String] = adapter.queryOf(request) ++ params
    lazy val queryParamMsgpack             = stringMapCodec.toMsgPack(requestParams)

    // Build the function arguments
    val methodArgs: Seq[Any] =
      for (arg <- methodSurface.args) yield {
        val argSurface = arg.surface
        argSurface.rawType match {
          case cl if classOf[HttpRequest[_]].isAssignableFrom(cl) =>
            // Bind the current http request instance
            adapter.httpRequestOf(request)
          case cl if adapter.requestType.isAssignableFrom(cl) =>
            request
          case cl if classOf[HttpContext[Req, Resp, F]].isAssignableFrom(cl) =>
            context
          case _ =>
            // Build from the string value in the request params
            val argCodec = MessageCodecFactory.defaultFactory.of(argSurface)
            val v: Option[Any] = requestParams.get(arg.name) match {
              case Some(paramValue) =>
                // Pass the String parameter to the method argument
                argCodec.unpackMsgPack(StringCodec.toMsgPack(paramValue))
              case None =>
                if (adapter.methodOf(request) == HttpMethod.GET) {
                  // Build the method argument instance from the query strings for GET requests
                  requestParams.get(arg.name) match {
                    case Some(x) =>
                      argCodec.unpackMsgPack(StringCodec.toMsgPack(x)).orElse(arg.getDefaultValue)
                    case None =>
                      argSurface match {
                        case _ if argSurface.isPrimitive =>
                          arg.getDefaultValue
                        case o: OptionSurface if o.elementSurface.isPrimitive =>
                          arg.getDefaultValue
                        case _ =>
                          argCodec.unpackMsgPack(queryParamMsgpack).orElse(arg.getDefaultValue)
                      }
                  }
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
                          MessagePack.fromJSON(contentBytes)
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
    trace(
      s"Method binding for request ${adapter.pathOf(request)}: ${methodSurface.name}(${methodSurface.args
        .mkString(", ")}) <= [${methodArgs.mkString(", ")}]"
    )
    methodArgs
  }
}
