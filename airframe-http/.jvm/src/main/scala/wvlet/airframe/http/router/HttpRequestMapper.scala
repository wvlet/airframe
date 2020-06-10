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
import wvlet.airframe.codec.{JSONCodec, MessageCodecFactory}
import wvlet.airframe.http._
import wvlet.airframe.json.JSON
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.msgpack.spi.{MessagePack, MsgPack, ValueFactory}
import wvlet.airframe.surface.reflect.ReflectMethodSurface
import wvlet.airframe.surface.{CName, MethodParameter, OptionSurface, Zero}
import wvlet.log.LogSupport

import scala.language.higherKinds
import scala.util.Try

/**
  * Mapping HTTP requests to RPC/Endpoint method call arguments.
  *
  * http request (path parameters, query parameters, request body (json or msgpack data))
  * -> rpc function call arguments (p1:t1, p2:t2, ...)
  *
  *
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
    // Collect URL path and query parameters
    val requestParamsInUrl: HttpMultiMap = adapter.queryOf(request) ++ params

    // Created a place holder for the function arguments
    val methodArgs: Array[Any]               = Array.fill[Any](methodSurface.args.size)(null)
    var remainingArgs: List[MethodParameter] = Nil

    // Populate http request context parameters first (e.g., HttpMessage.Request, HttpContext, etc.)
    for (arg <- methodSurface.args) {
      val argSurface = arg.surface
      val value: Option[Any] = argSurface.rawType match {
        case cl if classOf[HttpMessage.Request].isAssignableFrom(cl) =>
          // Bind the current http request instance
          Some(adapter.httpRequestOf(request))
        case cl if classOf[HttpRequest[_]].isAssignableFrom(cl) =>
          // Bind the current http request instance
          Some(adapter.wrap(request))
        case cl if adapter.requestType.isAssignableFrom(cl) =>
          // Bind HttpRequestAdapter[_]
          Some(request)
        case cl if classOf[HttpContext[Req, Resp, F]].isAssignableFrom(cl) =>
          // Bind HttpContext
          Some(context)
        case _ =>
          // Build from the string value in the request params
          requestParamsInUrl.get(arg.name) match {
            case Some(paramValue) =>
              // Pass the String parameter to the method argument
              val argCodec = codecFactory.of(argSurface)
              argCodec.unpackMsgPack(StringCodec.toMsgPack(paramValue))
            case _ =>
              None
          }
      }
      // Set the method argument
      value match {
        case Some(x) =>
          methodArgs(arg.index) = x
        case None =>
          remainingArgs = arg :: remainingArgs
      }
    }

    // GET requests should have no body content, so we need to construct method arg objects using query strings
    if (adapter.methodOf(request) == HttpMethod.GET) {
      // A MessagePack representation of request parameters is necessary to construct non-primitive objects
      lazy val queryParamMsgPack = HttpMultiMapCodec.toMsgPack(requestParamsInUrl)
      while (remainingArgs.nonEmpty) {
        val arg        = remainingArgs.head
        val argSurface = arg.surface
        // Build the method argument instance from the query strings for GET requests
        argSurface match {
          case _ if argSurface.isPrimitive =>
            setValue(arg, None)
          case o: OptionSurface if o.elementSurface.isPrimitive =>
            setValue(arg, None)
          case _ =>
            val argCodec = codecFactory.of(argSurface)
            setValue(arg, argCodec.unpackMsgPack(queryParamMsgPack))
        }
        remainingArgs = remainingArgs.tail
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

    def readContentBodyAsMsgPack: Option[MsgPack] = {
      // Build the method argument instance from the content body for non GET requests
      val contentBytes = adapter.contentBytesOf(request)

      if (contentBytes.isEmpty) {
        None
      } else {
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
      }
    }

    // Populate the remaining function arguments
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
              case m: MapValue if m.isEmpty =>
                None
              case m: MapValue =>
                // Extract the target parameter from the MapValue representation of the request content body
                m.get(ValueFactory.newString(arg.name)).map { paramValue =>
                    // {"(param name)":(value)}
                    argCodec.unpack(paramValue.toMsgpack)
                  }
                  .orElse {
                    // When the target paramter is not found in the MapValue
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

    trace(
      s"Method binding for request ${adapter.pathOf(request)}: ${methodSurface.name}(${methodSurface.args
        .mkString(", ")}) <= [${methodArgs.mkString(", ")}]"
    )
    methodArgs.toSeq
  }
}
