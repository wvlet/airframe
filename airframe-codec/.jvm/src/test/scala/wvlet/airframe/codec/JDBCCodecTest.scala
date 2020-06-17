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

import java.sql.{DriverManager, ResultSet}
import java.util

import wvlet.airframe.codec.JDBCCodec._
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil.withResource

import scala.collection.compat._

/**
  */
class JDBCCodecTest extends AirSpec {
  protected def withQuery[U](sql: String)(body: ResultSet => U): U = {
    Class.forName("org.sqlite.JDBC")
    withResource(DriverManager.getConnection("jdbc:sqlite::memory:")) { conn =>
      withResource(conn.createStatement()) { stmt =>
        debug(sql)
        withResource(stmt.executeQuery(sql)) { rs => body(rs) }
      }
    }
  }

  def `encode all JDBC types`: Unit = {
    withQuery("""
            |select
            |1,
            |'a',
            |cast(1 as boolean),
            |cast('1' as varchar(1)),
            |null,
            |cast('2019-01-23' as date),
            |cast('04:56:07' as time),
            |cast('01:23:45' as time with time zone),
            |cast('2019-01-23 04:56:07.000' as timestamp),
            |cast('2019-01-23 04:56:07.890' as timestamp with time zone),
            |cast(12345 as decimal),
            |cast(12345 as numeric),
            |cast(0.1 as float),
            |cast(0.1 as real),
            |cast(0.1 as double),
            |cast(12345 as bigint),
            |cast(1 as tinyint),
            |cast(1 as smallint),
            |cast(0 as bit),
            |cast('hello' as char),
            |cast('hello' as varchar),
            |cast('hello' as longvarchar),
            |cast('hello' as blob),
            |cast('hello' as clob)
            |            """.stripMargin) { rs =>
      // TODO: It seems there is a bug in sqlite-jdbc around date, time, timestamp handling
      val md = rs.getMetaData
      //(1 to md.getColumnCount).foreach(i => info(s"${md.getColumnType(i)}: ${md.getColumnTypeName(i)}"))

      val codec   = JDBCCodec(rs)
      val msgpack = codec.toMsgPack
      val json    = JSONCodec.toJson(msgpack)
      debug(json)
    }
  }

  def `encode null`: Unit = {
    val types = Seq(
      "bit",
      "tinyint",
      "smallint",
      "integer",
      "bigint",
      "float",
      "real",
      "double",
      "numeric",
      "decimal",
      "char",
      "varchar(1)",
      "longvarchar",
      "date",
      "time",
      "timestamp",
      "binary",
      "varbinary",
      "longvarbinary",
      "array",
      "blob",
      "clob",
      "ref",
      "boolean",
      "rowid",
      "nchar",
      "nvarchar",
      "longvarchar",
      "nclob",
      "xml",
      "time with time zone",
      "timestamp with time zone"
    )

    JavaSqlTimestampCodec.unpackBytes(MessagePack.newBufferPacker.packLong(15000000).toByteArray) shouldBe Some(
      new java.sql.Timestamp(15000000)
    )
    val selectItems = types.map(x => s"cast(null as ${x})").mkString(", ")

    withQuery(s"select ${selectItems}") { rs =>
      val codec   = JDBCCodec(rs)
      val msgpack = codec.toMsgPack
      val json    = JSONCodec.toJson(msgpack)
      debug(json)
    }
  }

  def `support sql date`: Unit = {
    JavaSqlDateCodec.unpackBytes(MessagePack.newBufferPacker.packString("2019-01-23").toByteArray) shouldBe Some(
      java.sql.Date.valueOf("2019-01-23")
    )
    JavaSqlDateCodec.unpackBytes(MessagePack.newBufferPacker.packLong(15000000).toByteArray) shouldBe Some(
      new java.sql.Date(15000000)
    )
    JavaSqlDateCodec.unpackBytes(MessagePack.newBufferPacker.packNil.toByteArray) shouldBe None
  }

  def `support sql time`: Unit = {
    JavaSqlTimeCodec.unpackBytes(MessagePack.newBufferPacker.packString("01:23:45").toByteArray) shouldBe Some(
      java.sql.Time.valueOf("01:23:45")
    )
    JavaSqlTimeCodec.unpackBytes(MessagePack.newBufferPacker.packLong(15000000).toByteArray) shouldBe Some(
      new java.sql.Time(15000000)
    )
    JavaSqlTimeCodec.unpackBytes(MessagePack.newBufferPacker.packNil.toByteArray) shouldBe None
  }

  def `support sql timestamp`: Unit = {
    JavaSqlTimestampCodec.unpackBytes(
      MessagePack.newBufferPacker.packString("2019-01-23 01:23:45.000").toByteArray
    ) shouldBe Some(
      java.sql.Timestamp.valueOf("2019-01-23 01:23:45.000")
    )
    JavaSqlTimestampCodec.unpackBytes(MessagePack.newBufferPacker.packLong(15000000).toByteArray) shouldBe Some(
      new java.sql.Timestamp(15000000)
    )
    JavaSqlTimestampCodec.unpackBytes(MessagePack.newBufferPacker.packNil.toByteArray) shouldBe None
  }

  def `support java.math.BigDecimal`: Unit = {
    BigDecimalCodec.unpackBytes(MessagePack.newBufferPacker.packString("12345").toByteArray) shouldBe Some(
      new java.math.BigDecimal(12345)
    )
    BigDecimalCodec.unpackBytes(MessagePack.newBufferPacker.packLong(12345L).toByteArray) shouldBe Some(
      new java.math.BigDecimal(12345)
    )
    BigDecimalCodec.unpackBytes(MessagePack.newBufferPacker.packDouble(12345.0).toByteArray) shouldBe Some(
      java.math.BigDecimal.valueOf(12345.0)
    )
    BigDecimalCodec.unpackBytes(MessagePack.newBufferPacker.packNil.toByteArray) shouldBe None
  }

  def `support array types`: Unit = {
    val p = MessagePack.newBufferPacker
    JavaSqlArrayCodec.pack(p, MockArray(Array("a", "b")))
    JavaSqlArrayCodec.pack(p, MockArray(Array(1, 2)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(1.toShort, 2.toShort)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(true, false)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(0.1f, 0.2f)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(0.1, 0.2)))
    JavaSqlArrayCodec.pack(p, MockArray(Array('a', 'b', 'c')))
    JavaSqlArrayCodec.pack(p, MockArray(Array(0.toByte, 1.toByte)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(10L, 20L)))
    JavaSqlArrayCodec.pack(p, MockArray(Array(1, "a", 10L)))
    intercept[UnsupportedOperationException] {
      JavaSqlArrayCodec.pack(p, MockArray(Seq(1, 2, 3)))
    }
  }

  def `ResultSet to JSON maps`: Unit = {
    withQuery("""with a(id, name) as
                |(select * from (values (1, 'leo'), (2, 'yui')))
                |select * from a order by id asc
                |""".stripMargin) { rs =>
      val jsonSeq = JDBCCodec(rs).toJsonSeq.iterator.toIndexedSeq
      jsonSeq(0) shouldBe """{"id":1,"name":"leo"}"""
      jsonSeq(1) shouldBe """{"id":2,"name":"yui"}"""
    }
  }
}

case class MockArray(v: AnyRef) extends java.sql.Array {
  override def getBaseTypeName: String                                                           = ""
  override def getBaseType: Int                                                                  = 0
  override def getArray: AnyRef                                                                  = v
  override def getArray(map: util.Map[String, Class[_]]): AnyRef                                 = ???
  override def getArray(index: Long, count: Int): AnyRef                                         = ???
  override def getArray(index: Long, count: Int, map: util.Map[String, Class[_]]): AnyRef        = ???
  override def getResultSet: ResultSet                                                           = ???
  override def getResultSet(map: util.Map[String, Class[_]]): ResultSet                          = ???
  override def getResultSet(index: Long, count: Int): ResultSet                                  = ???
  override def getResultSet(index: Long, count: Int, map: util.Map[String, Class[_]]): ResultSet = ???
  override def free(): Unit                                                                      = ???
}
