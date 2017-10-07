package wvlet.airframe.tablet.obj

import wvlet.airframe.tablet.msgpack.MessageCodec
import wvlet.airframe.tablet.{Record, TabletReader}
import wvlet.log.LogSupport
import wvlet.surface.Surface
import wvlet.surface.reflect.SurfaceFactory

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectTabletReader[A: ru.TypeTag](input: Seq[A], codec: Map[Surface, MessageCodec[_]] = Map.empty) extends TabletReader with LogSupport {

  private val cursor  = input.iterator
  private val reader  = new ObjectReader(codec = codec)
  private val surface = SurfaceFactory.of[A]

  def read: Option[Record] = {
    if (!cursor.hasNext) {
      None
    } else {
      val record = cursor.next()
      Some(reader.read[A](record))
    }
  }
}
