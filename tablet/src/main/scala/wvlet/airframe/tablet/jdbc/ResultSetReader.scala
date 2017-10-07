package wvlet.airframe.tablet.jdbc

import java.sql.ResultSet

import org.msgpack.core.MessagePack
import wvlet.airframe.tablet.{MessagePackRecord, Record, Schema, TabletReader}

/**
  *
  */
class ResultSetReader(rs: ResultSet) extends TabletReader {
  private val typeMap = SQLTypeMapping.default

  private lazy val m       = rs.getMetaData
  private lazy val columns = m.getColumnCount
  private lazy val schema: Seq[Schema.ColumnType] =
    (1 to columns).map { i =>
      typeMap(java.sql.JDBCType.valueOf(m.getColumnType(i)))
    }.toIndexedSeq

  override def read: Option[Record] = {
    if (!rs.next()) {
      None
    } else {
      val b = MessagePack.newDefaultBufferPacker()
      b.packArrayHeader(columns)

      def pack[A, U](v: A, packBody: A => U) {
        if (rs.wasNull())
          b.packNil()
        else {
          packBody(v)
        }
      }

      for ((colType, i) <- schema.zipWithIndex) {
        colType match {
          case Schema.INTEGER =>
            pack(rs.getLong(i + 1), b.packLong)
          case Schema.FLOAT =>
            pack(rs.getDouble(i + 1), b.packDouble)
          case Schema.BOOLEAN =>
            pack(rs.getBoolean(i + 1), b.packBoolean)
          case Schema.TIMESTAMP =>
            val v: java.sql.Timestamp = rs.getTimestamp(i + 1)
            val epochMillis           = v.getTime
            pack(epochMillis, b.packLong)
          case Schema.STRING =>
            pack(rs.getString(i + 1), b.packString)
          case Schema.ARRAY(elemType) =>
            val v = rs.getArray(i + 1)
            pack(
              v, { v: java.sql.Array =>
                val elemType    = v.getBaseType
                val arr: AnyRef = v.getArray
                arr match {
                  case a: Array[String] =>
                    b.packArrayHeader(a.length)
                    a.foreach(b.packString(_))
                  case a: Array[Int] =>
                    b.packArrayHeader(a.length)
                    a.foreach(b.packInt(_))
                  case a: Array[Float] =>
                    b.packArrayHeader(a.length)
                    a.foreach(b.packFloat(_))
                  case a: Array[Double] =>
                    b.packArrayHeader(a.length)
                    a.foreach(b.packDouble(_))
                  case a: Array[Boolean] =>
                    b.packArrayHeader(a.length)
                    a.foreach(b.packBoolean(_))
                  case _ =>
                    throw new UnsupportedOperationException(s"Reading array type of ${arr.getClass} is not supported")
                }
              }
            )
          case Schema.JSON =>
            val v = rs.getString(i + 1)
            // Convert to JSON?
            pack(v, b.packString)
          case Schema.BINARY =>
            val arr = rs.getBytes(i + 1)
            pack(arr, { in: Array[Byte] =>
              b.packBinaryHeader(in.length)
              b.writePayload(in)
            })
          case _ =>
            throw new UnsupportedOperationException(s"reading ${colType} is not supported")
        }
      }
      val arr = b.toByteArray
      Some(MessagePackRecord(arr))
    }
  }

}
