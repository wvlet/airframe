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

import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.json.JSON.{JSONObject, JSONValue}
import wvlet.airframe.json.UnexpectedEOF
import wvlet.airframe.msgpack.spi._
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.experimental.macros
import scala.reflect.runtime.universe._
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
    val v        = new MessageContext
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
    new IllegalArgumentException(s"Failed to read the input msgpack data with codec: ${this}", e)
  }

  def pack(p: Packer, v: A): Unit
  def unpack(u: Unpacker, v: MessageContext): Unit

  // TODO add specialized methods for primitive values
  // def unpackInt(u:MessageUnpacker) : Int

  def toMsgPack(v: A): Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    this match {
      case c: PackAsMapSupport[_] =>
        c.asInstanceOf[PackAsMapSupport[A]].packAsMap(packer, v)
      case _ =>
        pack(packer, v)
    }
    packer.toByteArray
  }

  private def toMsgpackMap(v: A): MsgPack = {
    val packer = MessagePack.newBufferPacker
    this match {
      case c: PackAsMapSupport[_] =>
        c.asInstanceOf[PackAsMapSupport[A]].packAsMap(packer, v)
      case _ =>
        pack(packer, v)
    }
    packer.toByteArray
  }

  def toJson(v: A): String = {
    JSONCodec.toJson(toMsgpackMap(v))
  }

  def toJSONObject(v: A): JSONObject = {
    JSONValueCodec.unpackMsgPack(toMsgpackMap(v)) match {
      case Some(j @ JSONObject(o)) => j
      case _ =>
        throw new IllegalArgumentException(s"Failed to read as JSONObject: ${v}")
    }
  }

  def unpackBytes(msgpack: Array[Byte]): Option[A]                        = unpackMsgPack(msgpack)
  def unpackBytes(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = unpackMsgPack(msgpack, offset, len)

  def fromMsgPack(msgpack: Array[Byte]): A = unpack(msgpack)

  def unpackMsgPack(msgpack: Array[Byte]): Option[A] = unpackMsgPack(msgpack, 0, msgpack.length)
  def unpackMsgPack(msgpack: Array[Byte], offset: Int, len: Int): Option[A] = {
    val unpacker = MessagePack.newUnpacker(msgpack, offset, len)
    val v        = new MessageContext
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
        trace(e)
        None
    }
  }

  def unpackJson(json: String): Option[A] = {
    try {
      Some(fromJson(json))
    } catch {
      case e: UnexpectedEOF =>
        warn(s"${e.getMessage} in json: ${json}")
        None
    }
  }

  def fromJson(json: String): A = {
    val msgpack  = MessagePack.fromJSON(json)
    val unpacker = MessagePack.newUnpacker(msgpack)
    val v        = new MessageContext
    unpack(unpacker, v)
    if (v.hasError) {
      throw v.getError.get
    } else if (v.isNull) {
      throw new MessageCodecException[A](INVALID_DATA, this, s"Invalid JSON data for ${this}:\n${json}")
    } else {
      v.getLastValue.asInstanceOf[A]
    }
  }

  def fromMap(m: Map[String, Any]): A = {
    // We cannot call MessageCodec.of[Map[String, Any]] as this macro is defined in this project, so using
    // MessageCodec.ofSurface instead.
    val mapCodec = MessageCodec.ofSurface(Surface.of[Map[String, Any]]).asInstanceOf[MessageCodec[Map[String, Any]]]
    val msgpack  = mapCodec.toMsgPack(m)
    fromMsgPack(msgpack)
  }

  /**
    * A shortcut for fromMsgPack(StringCodec.toMsgPack(s))
    */
  def fromString(s: String): A = {
    fromMsgPack(StringCodec.toMsgPack(s))
  }
}

trait MessageValueCodec[A] extends MessageCodec[A] {
  def packValue(v: A): Value
  def unpackValue(v: Value): A

  override def pack(p: Packer, v: A): Unit = {
    p.packValue(packValue(v))
  }

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
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

  def fromJson[A: TypeTag](json: String): A = {
    MessageCodecFactory.defaultFactory.fromJson[A](json)
  }

  def toJson[A: TypeTag](obj: A): String = {
    MessageCodecFactory.defaultFactory.toJson[A](obj)
  }
}
