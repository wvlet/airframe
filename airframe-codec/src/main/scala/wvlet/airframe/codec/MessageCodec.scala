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

import wvlet.airframe.json.UnexpectedEOF
import wvlet.airframe.msgpack.spi._
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.experimental.macros
import scala.util.{Failure, Success, Try}

trait MessageCodec[A] extends LogSupport {

  /**
    * Converting the object into MessagePack (= Array[Byte])
    */
  def pack(v: A): MsgPack = toMsgPack(v)

  /**
    * Converting the input MessagePack into an object. If the conversion fails,
    * throw an IllegalArgumentException
    */
  def unpack(msgpack: MsgPack): A = {
    val unpacker = MessagePack.newUnpacker(msgpack)
    val v        = new MessageHolder
    try {
      unpack(unpacker, v)
    } catch {
      case e: Throwable => throw unpackError(e)
    }
    v.getError match {
      case Some(err) =>
        throw unpackError(err)
      case None =>
        v.getLastValue.asInstanceOf[A]
    }

  }

  private def unpackError(e: Throwable): Throwable = {
    new IllegalArgumentException(s"Failed to read the input msgpack data as ${this}", e)
  }

  def pack(p: Packer, v: A): Unit
  def unpack(u: Unpacker, v: MessageHolder): Unit

  // TODO add specialized methods for primitive values
  // def unpackInt(u:MessageUnpacker) : Int

  def toMsgPack(v: A): Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    pack(packer, v)
    packer.toByteArray
  }

  def toJson(v: A): String = {
    val packer = MessagePack.newBufferPacker
    this match {
      case c: PackAsMapSupport[_] =>
        c.asInstanceOf[PackAsMapSupport[A]].packAsMap(packer, v)
      case _ =>
        pack(packer, v)
    }
    val msgpack = packer.toByteArray
    JSONCodec.toJson(msgpack)
  }

  def unpackBytes(msgpack: Array[Byte]): Option[A]                        = unpackMsgPack(msgpack)
  def unpackBytes(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = unpackMsgPack(msgpack, offset, len)

  def unpackMsgPack(msgpack: Array[Byte]): Option[A] = unpackMsgPack(msgpack, 0, msgpack.length)
  def unpackMsgPack(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = {
    val unpacker = MessagePack.newUnpacker(msgpack, offset, len)
    val v        = new MessageHolder
    try {
      unpack(unpacker, v)
      if (v.isNull) {
        None
      } else {
        Some(v.getLastValue.asInstanceOf[A])
      }
    } catch {
      case e: InsufficientBufferException =>
        warn(e.getMessage)
        None
    }
  }

  def unpackJson(json: String): Option[A] = {
    try {
      val msgpack = JSONCodec.toMsgPack(json)
      unpackBytes(msgpack)
    } catch {
      case e: UnexpectedEOF =>
        warn(s"${e.getMessage} in json: ${json}")
        None
    }
  }
}

trait MessageValueCodec[A] extends MessageCodec[A] {
  def packValue(v: A): Value
  def unpackValue(v: Value): A

  override def pack(p: Packer, v: A): Unit = {
    p.packValue(packValue(v))
  }

  override def unpack(u: Unpacker, v: MessageHolder): Unit = {
    val vl = u.unpackValue
    Try(unpackValue(vl)) match {
      case Success(x) =>
        // TODO tell the value type of the object to MessageHolder
        // We cannot use MessageValueCodec for primitive types, which uses v.getInt, v.getString, etc.
        v.setObject(x)
      case Failure(e) =>
        v.setError(e)
    }
  }
}

object MessageCodec {
  def of[A]: MessageCodec[A] = macro CodecMacros.codecOf[A]
  def ofSurface(s: Surface): MessageCodec[_] = MessageCodecFactory.defaultFactory.ofSurface(s)
}
