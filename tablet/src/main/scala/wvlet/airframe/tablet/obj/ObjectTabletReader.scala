package wvlet.airframe.tablet.obj

import org.msgpack.core.MessagePack
import wvlet.airframe.tablet.msgpack.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.tablet.{MessagePackRecord, Record, TabletReader}
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectTabletReader[A: ru.TypeTag](input: Seq[A], codec: Map[Surface, MessageCodec[_]] = Map.empty) extends TabletReader with LogSupport {

  private val cursor       = input.iterator
  private val elementCodec = new MessageCodecFactory(codec).of[A]

  def read: Option[Record] = {
    if (!cursor.hasNext) {
      None
    } else {
      val elem   = cursor.next()
      val packer = MessagePack.newDefaultBufferPacker()
      if (elem == null) {
        packer.packArrayHeader(0)
      } else {
        elementCodec.pack(packer, elem)
      }
      Some(MessagePackRecord(packer.toByteArray))
    }
  }
}
