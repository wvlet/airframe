package wvlet.airframe.tablet.obj

import org.msgpack.core.MessagePack
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.tablet.{MessagePackRecord, Record, TabletReader}
import wvlet.log.LogSupport
import wvlet.surface.Surface

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectTabletReader[A](elementCodec: MessageCodec[A], input: Seq[A]) extends TabletReader with LogSupport {

  private val cursor = input.iterator

  def read: Option[Record] = {
    if (!cursor.hasNext) {
      None
    } else {
      val elem   = cursor.next()
      val packer = MessagePack.newDefaultBufferPacker()
      if (elem == null) {
        packer.packNil()
      } else {
        elementCodec.pack(packer, elem)
      }
      Some(MessagePackRecord(packer.toByteArray))
    }
  }
}

object ObjectTabletReader {
  def newTabletReader[A](seq: Seq[A], surface: Surface, codec: Map[Surface, MessageCodec[_]] = Map.empty) = {
    val elementCodec = MessageCodec.defaultFactory.withCodecs(codec).of(surface)
    new ObjectTabletReader[A](elementCodec.asInstanceOf[MessageCodec[A]], seq)
  }

  def newTabletReaderOf[A: ru.TypeTag](seq: Seq[A], codec: Map[Surface, MessageCodec[_]] = Map.empty) = {
    val elementCodec = MessageCodec.defaultFactory.withCodecs(codec).of[A]
    new ObjectTabletReader[A](elementCodec, seq)
  }
}
