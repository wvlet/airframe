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
import java.sql.{JDBCType, ResultSet}

import wvlet.airframe.codec.JavaSQLTypeCodec.{JavaSqlDateCodec, JavaSqlTimeCodec, JavaSqlTimestampCodec}
import wvlet.airframe.codec.PrimitiveCodec._
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.log.LogSupport

/**
  *
  */
object JDBCCodec {

  class ResultSetCodec(rs: ResultSet) extends MessageCodec[ResultSet] {
    private lazy val md                                   = rs.getMetaData
    private val columnCount                               = md.getColumnCount
    private def columnTypes: IndexedSeq[Int]              = (1 to md.getColumnCount).map(i => md.getColumnType(i))
    private val columnCodecs: IndexedSeq[JDBCColumnCodec] = columnTypes.map(toJDBCColumnCodec).toIndexedSeq

    override def pack(p: Packer, v: ResultSet): Unit = {
      p.packArrayHeader(columnCount)
      var col = 1
      while (col <= columnCount) {
        columnCodecs(col - 1).pack(p, rs, col)
        col += 1
      }
    }

    // TODO: We need a pack only MessageCodec interface
    override def unpack(u: Unpacker, v: MessageHolder): Unit = ???
  }

  def toJDBCColumnCodec(jdbcType: Int): JDBCColumnCodec = {
    JDBCType.valueOf(jdbcType) match {
      case JDBCType.BIT | JDBCType.BOOLEAN      => JDBCBooleanCodec
      case JDBCType.TINYINT | JDBCType.SMALLINT => JDBCShortCodec
      case JDBCType.INTEGER                     => JDBCIntCodec
      case JDBCType.BIGINT                      => JDBCLongCodec
      case JDBCType.REAL                        => JDBCFloatCodec
      case JDBCType.FLOAT | JDBCType.DOUBLE     => JDBCDoubleCodec
      case JDBCType.NUMERIC | JDBCType.DECIMAL  => JDBCStringCodec
      case JDBCType.CHAR | JDBCType.VARCHAR | JDBCType.LONGNVARCHAR | JDBCType.SQLXML | JDBCType.NCHAR |
          JDBCType.NVARCHAR | JDBCType.LONGNVARCHAR =>
        JDBCStringCodec
      case JDBCType.BLOB | JDBCType.BINARY | JDBCType.VARBINARY | JDBCType.LONGVARBINARY => JDBCBinaryCodec
      case JDBCType.DATE                                                                 => JDBCDateCodec
      case JDBCType.TIME | JDBCType.TIME_WITH_TIMEZONE                                   => JDBCTimeCodec
      case JDBCType.TIMESTAMP | JDBCType.TIMESTAMP_WITH_TIMEZONE                         => JDBCTimestampCodec
      case JDBCType.ARRAY                                                                => JDBCArrayCodec
      case JDBCType.JAVA_OBJECT                                                          => JDBCJavaObjectCodec
      case JDBCType.ROWID                                                                => JDBCRowIdCodec
      case other                                                                         => throw new UnsupportedOperationException(s"Unsupported JDBC type: ${other}")
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
        JavaSqlTimeCodec.pack(p, rs.getTime(colIndex))
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
        val elemType = v.getBaseType
        debug(s"elemType:${elemType}, ${java.sql.JDBCType.valueOf(elemType)}")
        val jdbcType    = java.sql.JDBCType.valueOf(elemType)
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
          case a: Array[AnyRef] =>
            debug(s"element class: ${a.head.getClass}")
            throw new UnsupportedOperationException(
              s"Reading array type of ${arr.getClass} is not supported:\n${a.mkString(", ")}")
          case other =>
            throw new UnsupportedOperationException(s"Reading array type of ${arr.getClass} is not supported: ${arr}")
        }
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

  object JDBCRowIdCodec extends JDBCColumnCodec {
    override def pack(p: Packer, rs: ResultSet, colIndex: Int): Unit = {
      val v = rs.getRowId(colIndex)
      if (rs.wasNull()) {
        p.packNil
      } else {
        p.packString(v.toString)
      }
    }
  }

}
