package wvlet.airframe.tablet

import org.msgpack.value.{ArrayValue, MapValue, Value}
import wvlet.airframe
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker}
import wvlet.airframe.tablet.obj.ObjectTabletReader
import wvlet.airframe.tablet.text.{CSVTabletPrinter, JSONTabletPrinter, TSVTabletPrinter}

import scala.reflect.runtime.{universe => ru}

trait Record {
  def pack(packer: Packer): Unit
  def unpacker: Unpacker
}

case class ShallowMessagePackRecord(unpacker: Unpacker) extends Record {
  override def pack(packer: Packer): Unit = {
    throw new UnsupportedOperationException("pack is not supported")
  }
}

case class MessagePackRecord(arr: Array[Byte]) extends Record {
  override def unpacker = {
    MessagePack.newUnpacker(arr)
  }
  override def pack(packer: Packer): Unit = {
    packer.addPayload(arr)
  }
}
case class StringArrayRecord(arr: Seq[String]) extends Record {
  override def unpacker = {
    val packer = MessagePack.newBufferPacker
    pack(packer)
    MessagePack.newUnpacker(packer.toByteArray)
  }
  override def pack(packer: Packer): Unit = {
    packer.packArrayHeader(arr.length)
    arr.foreach(v => packer.packString(v))
  }
}

/**
  *
  */
trait TabletReader {
  def close: Unit = {}

  def read: Option[Record]

  def pipe[Out](out: TabletWriter[Out]): Seq[Out] = {
    Tablet.pipe(this, out)
  }

  def |[Out](out: TabletWriter[Out]): Seq[Out] = {
    Tablet.pipe(this, out)
  }
}

trait TabletWriter[A] extends AutoCloseable {
  self =>

  def write(record: Record): A

  override def close(): Unit = {}
}

object Tablet {

  def pipe[A](in: TabletReader, out: TabletWriter[A]): Seq[A] = {
    val result = Iterator
      .continually(in.read)
      .takeWhile(_.isDefined)
      .map(record => out.write(record.get))
      .toIndexedSeq
    out.close()
    result
  }

  val nullOutput = NullTabletWriter

  implicit class SeqTablet[A: ru.TypeTag](seq: Seq[A]) {
    def |[B](out: TabletWriter[B]) = {
      ObjectTabletReader.newTabletReaderOf(seq) | out
    }

    def toJson: Seq[String] = ObjectTabletReader.newTabletReaderOf(seq).pipe(JSONTabletPrinter)
    def toCSV: Seq[String]  = ObjectTabletReader.newTabletReaderOf(seq).pipe(CSVTabletPrinter)
    def toTSV: Seq[String]  = ObjectTabletReader.newTabletReaderOf(seq).pipe(TSVTabletPrinter)
  }

  implicit class RichTabletReader(in: TabletReader) {
    def toJson: Seq[String] = in.pipe(JSONTabletPrinter)
    def toCSV: Seq[String]  = in.pipe(CSVTabletPrinter)
    def toTSV: Seq[String]  = in.pipe(TSVTabletPrinter)
  }

}

object NullTabletWriter extends TabletWriter[Unit] {
  override def write(record: Record): Unit = {
    val u = record.unpacker
    u.skipValue
  }

  override def close(): Unit = {}
}

//  private implicit class RichColumnIndex(columnIndex: Int) {
//    def toType: Column = schema.columnType(columnIndex)
//  }

//  // Primitive types
//  def writeInteger(c: Column, v: Int): self.type
//  def writeFloat(c: Column, v: Float): self.type
//  def writeBoolean(c: Column, v: Boolean): self.type
//  def writeString(c: Column, v: String): self.type
//  def writeTimeStamp(c: Column, v: TimeStamp): self.type
//  def writeBinary(c: Column, v: Array[Byte]): self.type
//  def writeBinary(c: Column, v: ByteBuffer): self.type
//
//  // Complex types
//  def writeJSON(c: Column, v: Value): self.type
//  def writeArray(c: Column, v: ArrayValue): self.type
//  def writeMap(c: Column, v: MapValue): self.type
//
//  // Helper methods for column index (0-origin) based accesses
//  def writeInteger(columIndex: Int, v: Int): self.type = writeInteger(columIndex.toType, v)
//  def writeFloat(columnIndex: Int, v: Float): self.type = writeFloat(columnIndex.toType, v)
//  def writeBoolean(columnIndex: Int, v: Boolean): self.type = writeBoolean(columnIndex.toType, v)
//  def writeString(columnIndex: Int, v: String): self.type = writeString(columnIndex.toType, v)
//  def writeTimeStamp(columnIndex: Int, v: TimeStamp): self.type = writeTimeStamp(columnIndex.toType, v)
//  def writeBinary(columnIndex: Int, v: Array[Byte]): self.type = writeBinary(columnIndex.toType, v)
//  def writeBinary(columnIndex: Int, v: ByteBuffer): self.type = writeBinary(columnIndex.toType, v)
//  def writeJSON(columnIndex: Int, v: Value): self.type = writeJSON(columnIndex.toType, v)
//  def writeArray(columnIndex: Int, v: ArrayValue): self.type = writeArray(columnIndex.toType, v)
//  def writeMap(columnIndex: Int, v: MapValue): self.type = writeMap(columnIndex.toType, v)

trait RecordReader {
  def isNull: Boolean
  def readNull: Unit
  def readLong: Long
  def readDouble: Double
  def readString: String
  def readBinary: Array[Byte]
  def readTimestamp: java.time.temporal.Temporal
  def readJson: Value

  def readArray: ArrayValue
  def readMap: MapValue
}
