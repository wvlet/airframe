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
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.IO
import wvlet.airframe.msgpack.spi.MsgPack
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
    val contentType =
      GrpcContext.current.map(_.contentType).getOrElse(GrpcEncoding.ContentTypeGrpcMsgPack)

    info(contentType)
    contentType match {
      case GrpcEncoding.ContentTypeGrpcJson =>
        val json = codec.toJson(value)
        info(json)
        new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
      case _ =>
        new ByteArrayInputStream(codec.toMsgPack(value))
    }
  }

  override def parse(stream: InputStream): A = {
    val bytes = IO.readFully(stream)
    if (bytes.length > 0 && bytes.head == '{' && bytes.last == '}') {
      codec.fromJson(bytes)
    } else {
      codec.fromMsgPack(bytes)
    }
  }
}
