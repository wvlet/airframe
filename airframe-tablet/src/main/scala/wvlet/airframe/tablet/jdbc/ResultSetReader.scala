package wvlet.airframe.tablet.jdbc

import java.sql.ResultSet

import wvlet.airframe.codec.JDBCCodec
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.tablet.{MessagePackRecord, Record, Schema, TabletReader}
import wvlet.log.LogSupport

/**
  *
  */
class ResultSetReader(rs: ResultSet) extends TabletReader with LogSupport {
  private lazy val codec = new JDBCCodec.ResultSetCodec(rs)

  override def read: Option[Record] = {
    if (!rs.next()) {
      None
    } else {
      val p = MessagePack.newBufferPacker
      codec.pack(p, rs)
      val arr = p.toByteArray
      Some(MessagePackRecord(arr))
    }
  }
}
