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
import java.io.{ByteArrayInputStream, InputStream}

import io.grpc.MethodDescriptor.Marshaller
import wvlet.airframe.codec.{INVALID_DATA, MessageCodecException, MessageContext}
import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.log.LogSupport

/**
  * Marshalling String as MessagePack
  */
private[grpc] object StringMarshaller extends Marshaller[String] with LogSupport {
  override def stream(value: String): InputStream = {
    new ByteArrayInputStream(StringCodec.toMsgPack(value))
  }
  override def parse(stream: InputStream): String = {
    val unpacker = MessagePack.newUnpacker(stream)
    val v        = MessageContext()

    StringCodec.unpack(unpacker, v)
    if (!v.isNull) {
      val s = v.getString
      s
    } else {
      v.getError match {
        case Some(e) => throw new RuntimeException(e)
        case None    => throw new MessageCodecException(INVALID_DATA, StringCodec, "invalid input")
      }
    }
  }
}
