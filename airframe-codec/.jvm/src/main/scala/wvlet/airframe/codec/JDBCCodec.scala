/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.codec
import java.sql.{ResultSet, Time, Timestamp, Types}

import wvlet.airframe.codec.PrimitiveCodec._
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker, ValueType}
import wvlet.log.LogSupport

import scala.collection.compat._

/**
  */
object JDBCCodec extends LogSupport {
  def apply(rs: ResultSet): ResultSetCodec = new ResultSetCodec(rs)

  class ResultSetCodec(rs: ResultSet) {
    private lazy val md                      = rs.getMetaData
    private lazy val columnCount             = md.getColumnCount
    private def columnTypes: IndexedSeq[Int] = (1 to columnCount).map(i => md.getColumnType(i))
    private lazy val columnCodecs: IndexedSeq[JDBCColumnCodec] =
      (1 to columnCount).map(i => toJDBCColumnCodec(md.getColumnType(i), md.getColumnTypeName(i))).toIndexedSeq
    private lazy val columnNames = (1 to columnCount).map(i => md.getColumnName(i))

    /**
      * Encode the all ResultSet rows as MsgPack map values
      */
    def toMsgPack: Array[Byte] = {
      val p = MessagePack.newBufferPacker
      packAllRowsAsMap(p)
      p.toByteArray
    }

    /**
      * Encode the all ResultSet rows as JSON object values
      */
    def toJsonSeq: IterableOnce[String] = {
      mapMsgPackMapRows { msgpack => JSONCodec.toJson(msgpack) }
    }

    /**
      * Pack the all ResultSet rows as MsgPack array values
      */
    def packAllRowsAsArray(p: Packer): Unit = {
      while (rs.next()) {
        packRowAsArray(p)
      }
    }

    /**
      * pack the all ResultSet rows as MsgPack map values
      */
    def packAllRowsAsMap(p: Packer): Unit = {
      while (rs.next()) {
        packRowAsMap(p)
      }
    }

    private class RStoMsgPackIterator[A](f: Array[Byte] => A, packer: Packer => Unit) extends Iterator[A] {
      private var hasNextElem: Option[Boolean] = None
      override def hasNext: Boolean = {
        hasNextElem match {
          case Some(x) => x
          case None =>
            val x = rs.next()
            hasNextElem = Some(x)
            x
        }
      }

      override def next(): A = {
        // TODO Optimize it by resetting the internal buffer of Packer
        val p = MessagePack.newBufferPacker
        packer(p)
        hasNextElem = None
        f(p.toByteArray)
      }
    }

    /**
      * Create an interator for reading ResultSet as a sequence of MsgPack Map values
      */
    def mapMsgPackMapRows[U](f: Array[Byte] => U): IterableOnce[U] = {
      new RStoMsgPackIterator[U](f, packer = packRowAsMap(_))
    }

    /**
      * Create an interator for reading ResultSet as a sequence of MsgPack array values
      */
    def mapMsgPackArrayRows[U](f: Array[Byte] => U): IterableOnce[U] = {
      new RStoMsgPackIterator[U](f, packer = packRowAsArray(_))
    }

    /**
      * Read a row from ResultSet and pack as a MsgPack array
      */
    def packRowAsArray(p: Packer): Unit = {
      p.packArrayHeader(columnCount)
      var col = 1
      while (col <= columnCount) {
        columnCodecs(col - 1).pack(p, rs, col)
        col += 1
      }
    }

    /**
      * Read a row from ResultSet and pack as a MsgPack map
      */
    def packRowAsMap(p: Packer): Unit = {
      p.packMapHeader(columnCount)
      var col = 1
      while (col <= columnCount) {
        p.packString(columnNames(col - 1))
        columnCodecs(col - 1).pack(p, rs, col)
        col += 1
      }
    }
  }

  def toJDBCColumnCodec(sqlType: Int, typeName: String): JDBCColumnCodec = {
    typeName match {
      // workaround for sqlite-jdbc, which doesn't support all JDBC types
      case "DATE"                                   => JDBCDateCodec
      case "TIME" | "TIME WITH TIME ZONE"           => JDBCTimeCodec
      case "TIMESTAMP" | "TIMESTAMP WITH TIME ZONE" => JDBCTimestampCodec
      case x if x.startsWith("DECIMAL")             => JDBCDecimalCodec
      case "BIT"                                    => JDBCBooleanCodec
      case _ =>
        sqlType match {
          case Types.BIT | Types.BOOLEAN      => JDBCBooleanCodec
          case Types.TINYINT | Types.SMALLINT => JDBCShortCodec
          case Types.INTEGER                  => JDBCIntCodec
          case Types.BIGINT                   => JDBCLongCodec
          case Types.REAL                     => JDBCFloatCodec
          case Types.FLOAT | Types.DOUBLE     => JDBCDoubleCodec
          case Types.NUMERIC | Types.DECIMAL  => JDBCStringCodec
          case Types.CHAR | Types.VARCHAR | Types.LONGNVARCHAR | Types.SQLXML | Types.NCHAR | Types.NVARCHAR |
              Types.CLOB | Types.ROWID =>
            JDBCStringCodec
          case Types.BLOB | Types.BINARY | Types.VARBINARY | Types.LONGVARBINARY => JDBCBinaryCodec
          case Types.DATE                                                        => JDBCDateCodec
          case Types.TIME | Types.TIME_WITH_TIMEZONE                             => JDBCTimeCodec
          case Types.TIMESTAMP | Types.TIMESTAMP_WITH_TIMEZONE                   => JDBCTimestampCodec
          case Types.ARRAY                                                       => JDBCArrayCodec
          case Types.JAVA_OBJECT                                                 => JDBCJavaObjectCodec
          case Types.NULL                                                        => JDBCNullCodec
          case other =>
            warn(s"Unsupported JDBC type: ${other}. Assume string type")
            JDBCStringCodec
        }
    }
  }

  // Base codec for reading data from the specific column in a JDBC ResultSet
  trait JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit
  }

  object JDBCBooleanCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getBoolean(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        BooleanCodec.pack(p, v)
      }
    }
  }

  object JDBCShortCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getShort(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        ShortCodec.pack(p, v)
      }
    }
  }

  object JDBCIntCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getInt(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        IntCodec.pack(p, v)
      }
    }
  }

  object JDBCLongCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getLong(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        LongCodec.pack(p, v)
      }
    }
  }

  object JDBCFloatCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getFloat(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        FloatCodec.pack(p, v)
      }
    }
  }

  object JDBCDoubleCodec extends JDBCColumnCodec {
    def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getDouble(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        DoubleCodec.pack(p, v)
      }
    }
  }

  object JDBCDecimalCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getBigDecimal(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        BigDecimalCodec.pack(p, v)
      }
    }
  }

  object JDBCStringCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getString(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        StringCodec.pack(p, v)
      }
    }
  }

  object JDBCBinaryCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getBytes(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        ByteArrayCodec.pack(p, v)
      }
    }
  }

  object JDBCDateCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getDate(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        JavaSqlDateCodec.pack(p, v)
      }
    }
  }

  object JDBCTimeCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      // Use the string representation of java.sql.Time
      val v = rs.getTime(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        JavaSqlTimeCodec.pack(p, v)
      }
    }
  }
  object JDBCTimestampCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      // Use the string representation of java.sql.Timestamp
      val v = rs.getTimestamp(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        JavaSqlTimestampCodec.pack(p, v)
      }
    }
  }

  object JDBCArrayCodec extends JDBCColumnCodec with LogSupport {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getArray(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        JavaSqlArrayCodec.pack(p, v)
      }
    }
  }

  object JDBCJavaObjectCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val obj = rs.getObject(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        // Just store the string representation of the object
        StringCodec.pack(p, obj.toString)
      }
    }
  }

  object JDBCNullCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      p.packNil
    }
  }

  object JavaSqlDateCodec extends MessageCodec[java.sql.Date] with LogSupport {
    override def pack(p: Packer, v: java.sql.Date): Unit = {
      // Store string representation of java.sql.Date
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(java.sql.Date.valueOf(u.unpackString))
        case ValueType.INTEGER =>
          // Milliseconds since 1970-01-01
          val epochMillis = u.unpackLong
          v.setObject(new java.sql.Date(epochMillis))
        case other =>
          v.setError(
            new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Data from ${other} type")
          )
      }
    }
  }

  object JavaSqlTimeCodec extends MessageCodec[java.sql.Time] {
    override def pack(p: Packer, v: Time): Unit = {
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(java.sql.Time.valueOf(u.unpackString))
        case ValueType.INTEGER =>
          val epochMillis = u.unpackLong
          v.setObject(new java.sql.Time(epochMillis))
        case other =>
          v.setError(
            new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Time from ${other} type")
          )
      }
    }
  }

  object JavaSqlTimestampCodec extends MessageCodec[java.sql.Timestamp] {
    override def pack(p: Packer, v: Timestamp): Unit = {
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          v.setObject(java.sql.Timestamp.valueOf(u.unpackString))
        case ValueType.INTEGER =>
          val epochMillis = u.unpackLong
          v.setObject(new java.sql.Timestamp(epochMillis))
        case other =>
          v.setError(
            new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.sql.Time from ${other} type")
          )
      }
    }
  }

  object BigDecimalCodec extends MessageCodec[java.math.BigDecimal] {
    override def pack(p: Packer, v: java.math.BigDecimal): Unit = {
      p.packString(v.toString)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      u.getNextValueType match {
        case ValueType.STRING =>
          val s = u.unpackString
          v.setObject(new java.math.BigDecimal(s))
        case ValueType.INTEGER =>
          val l = u.unpackLong
          v.setObject(java.math.BigDecimal.valueOf(l))
        case ValueType.FLOAT =>
          val f = u.unpackDouble
          v.setObject(java.math.BigDecimal.valueOf(f))
        case other =>
          v.setError(
            new MessageCodecException(INVALID_DATA, this, s"Cannot construct java.math.BigDecimal from ${other} type")
          )
      }
    }
  }

  object JavaSqlArrayCodec extends MessageCodec[java.sql.Array] with LogSupport {
    def pack(p: Packer, v: java.sql.Array): Unit = {
      val arr: AnyRef = v.getArray
      arr match {
        case a: Array[java.lang.String] =>
          StringArrayCodec.pack(p, a)
        case a: Array[Int] =>
          IntArrayCodec.pack(p, a)
        case a: Array[Short] =>
          ShortArrayCodec.pack(p, a)
        case a: Array[Long] =>
          LongArrayCodec.pack(p, a)
        case a: Array[Char] =>
          CharArrayCodec.pack(p, a)
        case a: Array[Byte] =>
          ByteArrayCodec.pack(p, a)
        case a: Array[Float] =>
          FloatArrayCodec.pack(p, a)
        case a: Array[Double] =>
          DoubleArrayCodec.pack(p, a)
        case a: Array[Boolean] =>
          BooleanArrayCodec.pack(p, a)
        case a: Array[_] =>
          AnyArrayCodec.pack(p, a.asInstanceOf[Array[Any]])
        case other =>
          throw new UnsupportedOperationException(s"Reading array type of ${arr.getClass} is not supported: ${arr}")
      }
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = ???
  }
}
