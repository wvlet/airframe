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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.Value

import java.nio.charset.StandardCharsets

/**
  */
sealed trait GrpcEncoding {
  def contentType: String
  def encodeWithCodec[A](v: A, codec: MessageCodec[A]): Array[Byte]
  def unpackValue(bytes: Array[Byte]): Value
}

object GrpcEncoding {
  val ContentTypeDefault     = "application/grpc"
  val ContentTypeGrpcMsgPack = "application/grpc+msgpack"
  val ContentTypeGrpcJson    = "application/grpc+json"

  case object MsgPack extends GrpcEncoding {
    override def contentType: String = ContentTypeGrpcMsgPack
    override def encodeWithCodec[A](v: A, codec: MessageCodec[A]): Array[Byte] = {
      codec.toMsgPack(v)
    }
    override def unpackValue(bytes: Array[Byte]): Value = {
      ValueCodec.unpack(bytes)
    }
  }

  case object JSON extends GrpcEncoding {
    override def contentType: String = ContentTypeGrpcJson
    override def encodeWithCodec[A](v: A, codec: MessageCodec[A]): Array[Byte] = {
      codec.toJson(v).getBytes(StandardCharsets.UTF_8)
    }
    override def unpackValue(bytes: Array[Byte]): Value = {
      ValueCodec.fromJson(new String(bytes, StandardCharsets.UTF_8))
    }
  }
}
