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

class GrpcResponseMarshaller[A](codec: MessageCodec[A]) extends Marshaller[A] with LogSupport {
  override def stream(value: A): InputStream = {
    val accept =
      GrpcContext.current.map(_.accept).getOrElse(GrpcEncoding.ContentTypeMsgPack)

    try {
      accept match {
        case GrpcEncoding.ContentTypeJson =>
          // Wrap JSON with a response object for the ease of parsing
          val json = s"""{"response":${codec.toJson(value)}}"""
          new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
        case _ =>
          new ByteArrayInputStream(codec.toMsgPack(value))
      }
    } catch {
      case e: MessageCodecException =>
        throw Status.INTERNAL
          .withDescription(s"Failed to encode the response: ${value}")
          .withCause(e)
          .asRuntimeException()
      case e: StatusRuntimeException =>
        throw e
      case e: StatusException =>
        throw e
      case e: Throwable =>
        throw Status.INTERNAL
          .withCause(e)
          .asRuntimeException()
    }
  }

  override def parse(stream: InputStream): A = {
    val bytes = IO.readFully(stream)

    try {
      if (isJsonBytes(bytes)) {
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
      case e: MessageCodecException =>
        throw Status.INTERNAL
          .withDescription("Failed to decode the response")
          .withCause(e)
          .asRuntimeException()
      case e: StatusRuntimeException =>
        throw e
      case e: StatusException =>
        throw e
      case e: Throwable =>
        throw Status.INTERNAL
          .withCause(e)
          .asRuntimeException()
    }
  }

  private def isJsonBytes(bytes: Array[Byte]): Boolean = {
    bytes.length >= 2 && bytes.head == '{' && bytes.last == '}'
  }
}
