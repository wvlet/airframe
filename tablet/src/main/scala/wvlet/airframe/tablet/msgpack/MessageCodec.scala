package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import wvlet.airframe.tablet.msgpack.MessageCodec.{ErrorCode, INVALID_DATA}

import scala.reflect.runtime.{universe => ru}
import scala.util.{Failure, Try}

trait MessageCodec[A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)

  def pack(v: A): Array[Byte] = {
    val packer = MessagePack.newDefaultBufferPacker()
    pack(packer, v)
    packer.toByteArray
  }

  def unpack(data: Array[Byte]): Option[A] = unpack(data, 0, data.length)
  def unpack(data: Array[Byte], offset: Int, len: Int): Option[A] = {
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

class MessageCodecException(errorCode: ErrorCode, message: String) extends Exception(message) {
  override def getMessage = s"[${errorCode}] ${message}"
}

object MessageCodec {
  trait ErrorCode
  case object INVALID_DATA extends ErrorCode

  def default                            = new MessageCodecFactory(StandardCodec.standardCodec)
  def of[A: ru.TypeTag]: MessageCodec[A] = default.of[A]
}
