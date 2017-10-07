package wvlet.airframe.tablet.msgpack

import java.io.{FileInputStream, FileOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import org.msgpack.core.{MessagePack, MessagePacker, MessageUnpacker}
import wvlet.airframe.tablet._
import wvlet.log.LogSupport

/**
  *
  */
object MessagePackTablet {

  def msgpackGzReader(file: String): TabletReader = {
    new MessagePackTabletReader(MessagePack.newDefaultUnpacker(new GZIPInputStream(new FileInputStream(file))))
  }

  def msgpackGzWriter(file: String): TabletWriter[Unit] = {
    new MessagePackTabletWriter(MessagePack.newDefaultPacker(new GZIPOutputStream(new FileOutputStream(file))))
  }

}

/**
  *
  */
class MessagePackTabletReader(unpacker: MessageUnpacker) extends TabletReader with LogSupport {

  private var readRows = 0

  private def readNext: Option[Record] = {
    if (!unpacker.hasNext) {
      None
    } else {
      val f = unpacker.getNextFormat
      readRows += 1
      Some(ShallowMessagePackRecord(unpacker))
    }
  }

  override def read: Option[Record] = {
    readNext
  }
}

/**
  *
  */
class MessagePackTabletWriter(packer: MessagePacker) extends TabletWriter[Unit] {
  def write(r: Record) {
    r.pack(packer)
  }

  override def close(): Unit = {
    packer.close()
  }
}
