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
import wvlet.airframe.codec.{JSONCodec, MessageCodecFactory, ObjectCodec, ParamListCodec}
import wvlet.airframe.http.HttpHeader.MediaType
import wvlet.airframe.http._
import wvlet.airframe.json.JSON
import wvlet.airframe.msgpack.spi.Value.{MapValue, StringValue}
import wvlet.airframe.msgpack.spi.{MessagePack, MsgPack, ValueFactory}
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.airframe.surface.{CName, MethodParameter, OptionSurface, Zero}
import wvlet.log.LogSupport

import scala.language.higherKinds
import scala.util.Try

/**
  * Mapping HTTP requests to RPC/Endpoint method call arguments
  */
object HttpRequestMapper extends LogSupport {
  def buildControllerMethodArgs[Req, Resp, F[_]](
      // This instance is necessary to retrieve the default method argument values
      controller: Any,
      // The target method surface to call
      methodSurface: ReflectMethodSurface,
      request: Req,
      context: HttpContext[Req, Resp, F],
      // Additional parameters
      params: Map[String, String],
      codecFactory: MessageCodecFactory
  )(implicit adapter: HttpRequestAdapter[Req]): Seq[Any] = {
    // Collect URL query parameters and other parameters embedded inside URL.
    val requestParams: HttpMultiMap = adapter.queryOf(request) ++ params
    lazy val queryParamMsgpack      = HttpMultiMapCodec.toMsgPack(requestParams)

    // Created a place holder for the function arguments
    val methodArgs: Array[Any]               = Array.fill[Any](methodSurface.args.size)(null)
    var remainingArgs: List[MethodParameter] = Nil

    // Populate http request context parameters first
    for (arg <- methodSurface.args) {
      val argSurface = arg.surface
      val value: Any = argSurface.rawType match {
        case cl if classOf[HttpMessage.Request].isAssignableFrom(cl) =>
          // Bind the current http request instance
          adapter.httpRequestOf(request)
        case cl if classOf[HttpRequest[_]].isAssignableFrom(cl) =>
          // Bind the current http request instance
          adapter.wrap(request)
        case cl if adapter.requestType.isAssignableFrom(cl) =>
          // Bind HttpRequestAdapter[_]
          request
        case cl if classOf[HttpContext[Req, Resp, F]].isAssignableFrom(cl) =>
          // Bind HttpContext
          context
        case _ =>
          // Build from the string value in the request params
          val v: Option[Any] = requestParams.get(arg.name) match {
            case Some(paramValue) =>
              // Pass the String parameter to the method argument
              val argCodec = codecFactory.of(argSurface)
              argCodec.unpackMsgPack(StringCodec.toMsgPack(paramValue))
            case _ =>
              None
          }
          v.getOrElse(null)
      }
      // Set the method argument
      if (value != null) {
        methodArgs(arg.index) = value
      } else {
        remainingArgs = arg :: remainingArgs
      }
    }

    // Populate the remaining function arguments

    // If GET requests should have no body content, so we need to populate method args using query strings
    if (adapter.methodOf(request) == HttpMethod.GET) {
      while (remainingArgs.nonEmpty) {
        val arg        = remainingArgs.head
        val argSurface = arg.surface
        // Build the method argument instance from the query strings for GET requests
        argSurface match {
          case _ if argSurface.isPrimitive =>
            arg.getDefaultValue
          case o: OptionSurface if o.elementSurface.isPrimitive =>
            arg.getDefaultValue
          case _ =>
            // If the
            val argCodec = codecFactory.of(argSurface)
            argCodec.unpackMsgPack(queryParamMsgpack).orElse(arg.getDefaultValue)
        }
        remainingArgs = remainingArgs.tail
      }
    }

    def readContentBodyAsMsgPack: Option[MsgPack] = {
      // Build the method argument instance from the content body for non GET requests
      val contentBytes = adapter.contentBytesOf(request)

      if (contentBytes.nonEmpty) {
        adapter.contentTypeOf(request).map(_.split(";")(0).toLowerCase()) match {
          case Some("application/x-msgpack") =>
            Some(contentBytes)
          case Some("application/json") =>
            // JSON -> msgpack
            Some(MessagePack.fromJSON(contentBytes))
          case Some("application/octet-stream") =>
            // Do not read binary contents
            None
          case _ =>
            // Try parsing the content body as as JSON
            Some {
              Try(JSON.parse(contentBytes))
                .map { jsonValue =>
                  JSONCodec.toMsgPack(jsonValue)
                }
                .getOrElse {
                  // If parsing as JSON fails, treat the content body as a regular string
                  StringCodec.toMsgPack(adapter.contentStringOf(request))
                }
            }
        }
      } else {
        None
      }
    }

    def setValue(arg: MethodParameter, v: Option[Any]): Unit = {
      methodArgs(arg.index) = v
      // Use the method default argument value if exists
        .orElse(arg.getMethodArgDefaultValue(controller))
        // If no mapping is available, use the zero value
        // TODO: Throw an error here when strict validation is enabled
        .getOrElse(Zero.zeroOf(arg.surface))
    }

    remainingArgs match {
      case Nil =>
      // Do nothing
      case arg :: Nil =>
        // For unary functions, we can omit the parameter name key in the request body
        val argSurface = arg.surface
        val argCodec   = codecFactory.of(argSurface)
        readContentBodyAsMsgPack match {
          case Some(msgpack) =>
            // Read the content body as a MessagePack Map value
            val v = MessagePack.newUnpacker(msgpack).unpackValue
            val opt: Option[Any] = v match {
              case m: MapValue =>
                m.get(ValueFactory.newString(arg.name)).map { paramValue =>
                    // {"(param name)":(value)}
                    argCodec.unpack(paramValue.toMsgpack)
                  }
                  .orElse {
                    // map content body as a parameter (no key is present)
                    argCodec.unpackMsgPack(msgpack)
                  }
              case _ =>
                argCodec.unpackMsgPack(msgpack)
            }
            setValue(arg, opt)
          case None =>
            setValue(arg, None)
        }
        remainingArgs = Nil
      case _ =>
        // Populate all of the remaining arguments using the content body
        readContentBodyAsMsgPack.foreach { msgpack =>
          MessagePack.newUnpacker(msgpack).unpackValue match {
            case m: MapValue =>
              val mapValue = m.entries.map { kv =>
                CName.toCanonicalName(kv._1.toString) -> kv._2
              }
              remainingArgs.foreach { arg =>
                val argValueOpt: Option[Any] = mapValue.get(CName.toCanonicalName(arg.name)).flatMap { x =>
                  val argCodec = codecFactory.of(arg.surface)
                  argCodec.unpackMsgPack(x.toMsgpack)
                }
                setValue(arg, argValueOpt)
              }
              remainingArgs = Nil
            case _ =>
          }
        }
    }

    // Set the default value for the remaining args
    remainingArgs.foreach { arg =>
      setValue(arg, None)
    }
//
//
//            case None =>
//                if (adapter.methodOf(request) == HttpMethod.GET) {
//                  // Build the method argument instance from the query strings for GET requests
//                  argSurface match {
//                    case _ if argSurface.isPrimitive =>
//                      arg.getDefaultValue
//                    case o: OptionSurface if o.elementSurface.isPrimitive =>
//                      arg.getDefaultValue
//                    case _ =>
//                      argCodec.unpackMsgPack(queryParamMsgpack).orElse(arg.getDefaultValue)
//                  }
//                } else if (!argSurface.isOption) {
//                  // Build the method argument instance from the content body for non GET requests
//                  val contentBytes = adapter.contentBytesOf(request)
//
//                  if (contentBytes.nonEmpty) {
//                    val msgpack =
//                      adapter.contentTypeOf(request).map(_.split(";")(0)) match {
//                        case Some("application/x-msgpack") =>
//                          contentBytes
//                        case Some("application/json") =>
//                          // JSON -> msgpack
//                          MessagePack.fromJSON(contentBytes)
//                        case _ =>
//                          // Try parsing as JSON first
//                          Try(JSON.parse(contentBytes))
//                            .map { jsonValue =>
//                              JSONCodec.toMsgPack(jsonValue)
//                            }
//                            .getOrElse {
//                              // If parsing as JSON fails, treat the content body as a regular string
//                              StringCodec.toMsgPack(adapter.contentStringOf(request))
//                            }
//                      }
//                    argCodec.unpackMsgPack(msgpack)
//                  } else {
//                    // Return the method default argument value if exists
//                    arg.getMethodArgDefaultValue(controller)
//                  }
//                } else {
//                  // Return the method default argument value if exists
//                  arg.getMethodArgDefaultValue(controller)
//                }
//            }
//            // If mapping fails, use the zero value
//            v.getOrElse(Zero.zeroOf(arg.surface))
//        }
//      }
    trace(
      s"Method binding for request ${adapter.pathOf(request)}: ${methodSurface.name}(${methodSurface.args
        .mkString(", ")}) <= [${methodArgs.mkString(", ")}]"
    )
    methodArgs.toSeq
  }
}
