package wvlet.airframe.tablet.msgpack

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import wvlet.airframe.tablet.msgpack.MessageCodec.ErrorCode
import scala.reflect.runtime.{universe => ru}

trait MessageCodec[A] {
  def pack(p: MessagePacker, v: A)
  def unpack(u: MessageUnpacker, v: MessageHolder)
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
