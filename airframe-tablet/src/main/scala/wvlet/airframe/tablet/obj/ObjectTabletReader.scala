package wvlet.airframe.tablet.obj

import wvlet.airframe.codec.{Codec, MessageCodecFactory}
import wvlet.airframe.msgpack
import wvlet.airframe.surface.Surface
import wvlet.airframe.tablet.{MessagePackRecord, Record, TabletReader}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectTabletReader[A](elementCodec: Codec[A], input: Seq[A]) extends TabletReader with LogSupport {

  private val cursor = input.iterator

  def read: Option[Record] = {
    if (!cursor.hasNext) {
      None
    } else {
      val elem   = cursor.next()
      val packer = msgpack.newBufferPacker
      if (elem == null) {
        packer.packNil
      } else {
        elementCodec.pack(packer, elem)
      }
      Some(MessagePackRecord(packer.toByteArray))
    }
  }
}

object ObjectTabletReader {
  def newTabletReader[A](seq: Seq[A], surface: Surface, codec: Map[Surface, Codec[_]] = Map.empty) = {
    val elementCodec = MessageCodecFactory.defaultFactory.withCodecs(codec).of(surface)
    new ObjectTabletReader[A](elementCodec.asInstanceOf[Codec[A]], seq)
  }

  def newTabletReaderOf[A: ru.TypeTag](seq: Seq[A], codec: Map[Surface, Codec[_]] = Map.empty) = {
    val elementCodec = MessageCodecFactory.defaultFactory.withCodecs(codec).of[A]
    new ObjectTabletReader[A](elementCodec, seq)
  }
}
