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

import io.grpc.MethodDescriptor.Marshaller
import io.grpc.{Status, StatusException, StatusRuntimeException}
import wvlet.airframe.codec.{MessageCodec, MessageCodecException, ParamListCodec}
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.control.IO
import wvlet.airframe.http.grpc.internal.GrpcException
import wvlet.airframe.msgpack.spi.{MsgPack, ValueFactory}
import wvlet.airframe.msgpack.spi.Value.MapValue
import wvlet.log.LogSupport

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

/**
  */
object GrpcRequestMarshaller extends Marshaller[MsgPack] with LogSupport {
  override def stream(value: MsgPack): InputStream = {
    new ByteArrayInputStream(value)
  }
  override def parse(stream: InputStream): MsgPack = {
    val bytes = IO.readFully(stream)
    bytes
  }
}

/**
  * This class wraps the response value with encoding method information
  */
case class GrpcResponse(value: Any, encoding: GrpcEncoding)

class GrpcResponseMarshaller[A](codec: MessageCodec[A]) extends Marshaller[Any] with LogSupport {
  override def stream(response: Any): InputStream = {
    try {
      response match {
        case GrpcResponse(v, encoding) =>
          encoding match {
            case GrpcEncoding.JSON =>
              // Wrap JSON with a response object for the ease of parsing
              val json = s"""{"response":${codec.toJson(v.asInstanceOf[A])}}"""
              new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
            case _ =>
              new ByteArrayInputStream(codec.toMsgPack(v.asInstanceOf[A]))
          }
        case _ =>
          new ByteArrayInputStream(codec.toMsgPack(response.asInstanceOf[A]))
      }
    } catch {
      case e: Throwable =>
        throw GrpcException.wrap(e)
    }
  }

  override def parse(stream: InputStream): Any = {
    val bytes = IO.readFully(stream)

    try {
      if (GrpcEncoding.isJsonObjectMessage(bytes)) {
        // Parse {"response": ....}
        ValueCodec.fromJson(bytes) match {
          case m: MapValue =>
            m.get(ValueFactory.newString("response")) match {
              case Some(v) =>
                codec.fromMsgPack(v.toMsgpack)
              case other =>
                throw Status.INTERNAL
                  .withDescription(s"Missing response value: ${other}")
                  .asRuntimeException()
            }
          case other =>
            throw Status.INTERNAL
              .withDescription(s"Invalid response: ${other}")
              .asRuntimeException()
        }
      } else {
        codec.fromMsgPack(bytes)
      }
    } catch {
      case e: Throwable =>
        throw GrpcException.wrap(e)
    }
  }

}
