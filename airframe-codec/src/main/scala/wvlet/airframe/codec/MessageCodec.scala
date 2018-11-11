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

import wvlet.airframe.msgpack.spi.{Packer, Unpacker, Value}
import wvlet.airframe.surface.Surface

import scala.util.{Failure, Success, Try}

trait MessageCodec[A] {
  def pack(p: Packer, v: A): Unit
  def unpack(u: Unpacker, v: MessageHolder): Unit

  // TODO add specialized methods for primitive values
  // def unpackInt(u:MessageUnpacker) : Int

  def toMsgPack(v: A): Array[Byte] = {
    val packer = wvlet.airframe.msgpack.newBufferPacker
    pack(packer, v)
    packer.toByteArray
  }

  def unpackBytes(msgpack: Array[Byte]): Option[A]                        = unpackMsgPack(msgpack)
  def unpackBytes(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = unpackMsgPack(msgpack, offset, len)

  def unpackMsgPack(msgpack: Array[Byte]): Option[A] = unpackMsgPack(msgpack, 0, msgpack.length)
  def unpackMsgPack(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = {
    val unpacker = wvlet.airframe.msgpack.newUnpacker(msgpack, offset, len)
    val v        = new MessageHolder
    unpack(unpacker, v)
    if (v.isNull) {
      None
    } else {
      Some(v.getLastValue.asInstanceOf[A])
    }
  }
}

trait MessageValueCodec[A] extends MessageCodec[A] {
  def pack(v: A): Value
  def unpack(v: Value): A

  override def pack(p: Packer, v: A): Unit = {
    p.packValue(pack(v))
  }

  override def unpack(u: Unpacker, v: MessageHolder): Unit = {
    val vl = u.unpackValue
    Try(unpack(vl)) match {
      case Success(x) =>
        // TODO tell the value type of the object to MessageHolder
        // We cannot use MessageValueCodec for primitive types, which uses v.getInt, v.getString, etc.
        v.setObject(x)
      case Failure(e) =>
        v.setError(e)
    }
  }
}
