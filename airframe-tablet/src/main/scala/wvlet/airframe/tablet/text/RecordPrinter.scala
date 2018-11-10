package wvlet.airframe.tablet.text

import wvlet.airframe.msgpack.spi.ValueType
import wvlet.airframe.tablet.{Record, TabletWriter}

/**
  *
  */
object RecordPrinter extends TabletWriter[Seq[String]] {
  def write(record: Record): Seq[String] = {
    val unpacker = record.unpacker
    val f        = unpacker.getNextFormat
    f.getValueType match {
      case ValueType.NIL =>
        unpacker.unpackNil
        Seq.empty
      case ValueType.ARRAY =>
        val len = unpacker.unpackArrayHeader
        val array = (0 until len).map { i =>
          val v = unpacker.unpackValue
          v.toString
        }
        array
      case other =>
        val v = unpacker.unpackValue
        Seq(v.toString)
    }
  }
}
