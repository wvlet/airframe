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
package wvlet.airframe.codec

import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

/**
  *
  */
trait CodecSpec extends AirSpec {
  protected def roundtrip[A](surface: Surface, v: A, expectedType: DataType = DataType.ANY): MessageContext = {
    roundtrip[A](MessageCodec.ofSurface(surface).asInstanceOf[MessageCodec[A]], v, expectedType)
  }

  protected def roundtrip[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageContext = {
    val h = new MessageContext
    debug(s"Testing roundtrip of ${v} with ${codec}")
    val packer = MessagePack.newBufferPacker
    codec.pack(packer, v)
    val unpacker = MessagePack.newUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.getError.map { e =>
      warn(e)
    }
    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  protected def roundtripStr[A](codec: MessageCodec[A], v: A, expectedType: DataType): MessageContext = {
    val h = new MessageContext
    trace(s"Testing str based roundtrip of ${v} with ${codec}")
    val packer = MessagePack.newBufferPacker
    packer.packString(v.toString)
    val unpacker = MessagePack.newUnpacker(packer.toByteArray)
    codec.unpack(unpacker, h)

    h.isNull shouldBe false
    h.hasError shouldBe false
    h.getDataType shouldBe expectedType
    h.getLastValue shouldBe v
    h
  }

  protected def checkCodec[A](codec: MessageCodec[A], v: A): Unit = {
    val b = codec.toMsgPack(v)
    val r = codec.unpackBytes(b)
    r shouldBe defined
    v shouldBe r.get
  }
}
