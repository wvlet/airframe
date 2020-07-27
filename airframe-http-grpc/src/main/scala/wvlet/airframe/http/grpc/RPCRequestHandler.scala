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
package wvlet.airframe.http.grpc
import io.grpc.stub.ServerCalls.UnaryMethod
import io.grpc.stub.StreamObserver
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.http.router.HttpRequestMapper
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.airframe.surface.{CName, MethodSurface}
import wvlet.log.LogSupport

import scala.util.{Failure, Success, Try}

/**
  * Receives MessagePack Map value for the RPC request, and call the controller method
  */
class RPCRequestHandler[A](controller: Any, methodSurface: MethodSurface, codecFactory: MessageCodecFactory)
    extends UnaryMethod[MsgPack, A]
    with LogSupport {

  private val argCodecs = methodSurface.args.map(a => codecFactory.of(a.surface))

  override def invoke(request: MsgPack, responseObserver: StreamObserver[A]): Unit = {
    // Build method arguments from MsgPack
    val requestValue = ValueCodec.unpack(request)
    trace(requestValue)

    val result = Try {
      requestValue match {
        case m: MapValue =>
          val mapValue = HttpRequestMapper.toCanonicalKeyNameMap(m)
          val args = for ((arg, i) <- methodSurface.args.zipWithIndex) yield {
            val argOpt = mapValue.get(CName.toCanonicalName(arg.name)) match {
              case Some(paramValue) =>
                Option(argCodecs(i).fromMsgPack(paramValue.toMsgpack)).orElse {
                  throw new IllegalArgumentException(s"Failed to parse ${paramValue} for ${arg}")
                }
              case None =>
                // If no value is found, use the method parameter's default argument
                arg.getMethodArgDefaultValue(controller)
            }
            argOpt.getOrElse {
              throw new IllegalArgumentException(s"No key for ${arg.name} is found in ${m}")
            }
          }

          trace(s"RPC call ${methodSurface.name}(${args.mkString(", ")})")
          methodSurface.call(controller, args: _*)
        case _ =>
          throw new IllegalArgumentException(s"Invalid argument: ${requestValue}")
      }
    }
    result match {
      case Success(v) =>
        responseObserver.onNext(v.asInstanceOf[A])
      case Failure(e) =>
        responseObserver.onError(e)
    }
    responseObserver.onCompleted()
  }
}
