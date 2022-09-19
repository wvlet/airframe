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

import wvlet.airframe.codec.PrimitiveCodec.{StringCodec, ValueCodec}
import wvlet.airframe.codec.{JSONCodec, MessageCodecFactory}
import wvlet.airframe.http._
import wvlet.airframe.json.JSON
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.msgpack.spi.{MessagePack, MsgPack, Value}
import wvlet.airframe.surface._
import wvlet.log.LogSupport

import scala.language.higherKinds
import scala.util.Try

/**
  * Mapping HTTP requests to RPC/Endpoint method call arguments.
  *
  * http request (path parameters, query parameters, request body (json or msgpack data))
  * -> rpc function call arguments (p1:t1, p2:t2, ...)
  */
object HttpRequestMapper extends LogSupport {

  def buildControllerMethodArgs[Req, Resp, F[_]](
      // This instance is necessary to retrieve the default method argument values
      controller: Any,
      // The target method surface to call
      methodSurface: MethodSurface,
      request: Req,
      context: HttpContext[Req, Resp, F],
      // Additional parameters
      params: Map[String, String],
      codecFactory: MessageCodecFactory,
      isRPC: Boolean
  )(implicit adapter: HttpRequestAdapter[Req]): Seq[Any] = {
    // Collect URL path and query parameters
    val requestParamsInUrl: HttpMultiMap = adapter.queryOf(request) ++ params
    // A MessagePack representation of request parameters will be necessary
    // to construct non-primitive objects
    lazy val queryParamMsgPack = HttpMultiMapCodec.toMsgPack(requestParamsInUrl)
    lazy val queryParamMap: Map[String, Value] = toCanonicalKeyNameMap(
      ValueCodec.fromMsgPack(queryParamMsgPack).asInstanceOf[MapValue]
    )

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
          // Bind HttpRequest[_]
          Some(request)
        case cl if classOf[HttpContext[Req, Resp, F]].isAssignableFrom(cl) =>
          // Bind HttpContext
          Some(context)
        case _ =>
          // Pass the String parameter to the method argument
          val argCodec = codecFactory.of(argSurface)
          // Build from the string value in the request params
          queryParamMap.get(CName.toCanonicalName(arg.name)) match {
            case Some(paramValue) =>
              // Single parameter (e.g.,p1=v) or Multiple parameter values to the same key (e.g., p1=v1&p1=v2)
              Some(argCodec.fromMsgPack(paramValue.toMsgpack))
            case None =>
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
      while (remainingArgs.nonEmpty) {
        val arg        = remainingArgs.head
        val argSurface = arg.surface

        def isPrimitiveSeq(s: Surface): Boolean = s.isSeq && s.typeArgs.headOption.forall(_.isPrimitive)

        argSurface match {
          // For primitive type, Seq[Primitive], and Option[Primitive], Option[Seq[Primitive]] values,
          // it should already be found in the query strings, so bind None here
          case _ if argSurface.isPrimitive || isPrimitiveSeq(argSurface) =>
            setValue(arg, None)
          case o: OptionSurface if o.elementSurface.isPrimitive || isPrimitiveSeq(o.elementSurface) =>
            setValue(arg, None)
          case _ =>
            // Build the method argument object instance using MessagePack representation of the query strings of GET
            val argCodec = codecFactory.of(argSurface)
            setValue(arg, Some(argCodec.fromMsgPack(queryParamMsgPack)))
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
          case Some("application/msgpack") | Some("application/x-msgpack") =>
            Some(contentBytes)
          case Some("application/json") =>
            // JSON -> msgpack
            try {
              Some(MessagePack.fromJSON(contentBytes))
            } catch {
              case e: Throwable =>
                val invalidJson = new String(contentBytes)
                warn(s"Failed to parse the request body as JSON: ${invalidJson}")
                throw new HttpServerException(HttpStatus.BadRequest_400)
                  .withContent(s"Invalid json body: ${invalidJson}")
            }
          case Some("application/octet-stream") =>
            // Do not read binary contents (e.g., uploaded file)
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
                val mapValue = toCanonicalKeyNameMap(m)
                // Extract the target parameter from the MapValue representation of the request content body
                mapValue.get(CName.toCanonicalName(arg.name)) match {
                  case Some(paramValue) =>
                    // {"(param name)":(value)}
                    Option(argCodec.fromMsgPack(paramValue.toMsgpack)).orElse {
                      throw new IllegalArgumentException(s"Failed to parse ${paramValue} for ${arg}")
                    }
                  case None =>
                    if (isRPC) {
                      // For RPC calls, we do not support empty keys
                      throw new IllegalArgumentException(s"No key for ${arg.name} is found: ${v}")
                    } else {
                      // When the target parameter is not found in the MapValue, try mapping the content body as a whole
                      argCodec.unpackMsgPack(msgpack)
                    }
                }
              case _ =>
                if (isRPC) {
                  throw new IllegalArgumentException(s"No key for ${arg.name} is found: ${v}")
                } else {
                  Some(argCodec.fromMsgPack(msgpack))
                }
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
              val mapValue = toCanonicalKeyNameMap(m)
              while (remainingArgs.nonEmpty) {
                val arg = remainingArgs.head
                val argValueOpt: Option[Any] = mapValue
                  .get(CName.toCanonicalName(arg.name))
                  .map { x =>
                    val argCodec = codecFactory.of(arg.surface)
                    argCodec.fromMsgPack(x.toMsgpack)
                  }
                setValue(arg, argValueOpt)
                remainingArgs = remainingArgs.tail
              }
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

  /**
    * Convert MapValue to use CName as keys to support case-insensitive match
    */
  private[http] def toCanonicalKeyNameMap(m: MapValue): Map[String, Value] = {
    m.entries.map { kv =>
      CName.toCanonicalName(kv._1.toString) -> kv._2
    }.toMap
  }

}
