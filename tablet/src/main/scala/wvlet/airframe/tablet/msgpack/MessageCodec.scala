package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import org.msgpack.value.Value
import wvlet.airframe.tablet.msgpack.MessageCodec.ErrorCode

import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Success, Try}

trait MessageCodec[A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)

  // TODO add specialized methods for primitive values
  // def unpackInt(u:MessageUnpacker) : Int

  def packToBytes(v: A): Array[Byte] = {
    val packer = MessagePack.newDefaultBufferPacker()
    pack(packer, v)
    packer.toByteArray
  }

  def unpackBytes(data: Array[Byte]): Option[A] = unpackBytes(data, 0, data.length)
  def unpackBytes(data: Array[Byte], offset: Int, len: Int): Option[A] = {
    val unpacker = MessagePack.newDefaultUnpacker(data, offset, len)
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

  override def pack(p: MessagePacker, v: A) {
    p.packValue(pack(v))
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder) {
    val vl = u.unpackValue()
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

class MessageCodecException(errorCode: ErrorCode, message: String) extends Exception(message) {
  override def getMessage = s"[${errorCode}] ${message}"
}

object MessageCodec {
  trait ErrorCode
  case object INVALID_DATA extends ErrorCode

  def default                            = new MessageCodecFactory(StandardCodec.standardCodec)
  def of[A: ru.TypeTag]: MessageCodec[A] = default.of[A]
}
