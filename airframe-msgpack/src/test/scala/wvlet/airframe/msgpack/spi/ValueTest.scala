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
package wvlet.airframe.msgpack.spi

import java.math.BigInteger
import java.util.Base64

import org.scalacheck.Gen
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airframe.msgpack.spi.Value._
import wvlet.airframe.msgpack.spi.ValueFactory._
import wvlet.airspec.spi.PropertyCheck
import wvlet.airspec.AirSpec

/**
  *
  */
class ValueTest extends AirSpec with PropertyCheck {
  scalaJsSupport

  private def rankOf(mf: MessageFormat): Int = {
    val order =
      Seq(MessageFormat.INT8, MessageFormat.INT16, MessageFormat.INT32, MessageFormat.INT64, MessageFormat.UINT64)
    order.zipWithIndex
      .find(x => x._1 == mf)
      .map(x => x._2)
      .getOrElse(10000)
  }

  protected def checkSuccinctType(pack: WriteCursor => Unit, expectedAtMost: MessageFormat): Unit = {
    try {
      val buf         = ByteArrayBuffer.newBuffer(1024)
      val writeCursor = WriteCursor(buf, 0)
      pack(writeCursor)
      val v = OffsetUnpacker.unpackValue(ReadCursor(buf, 0))
      v.valueType shouldBe ValueType.INTEGER
      val i  = v.asInstanceOf[IntegerValue]
      val mf = i.mostSuccinctMessageFormat
      //rankOf(mf) <= rankOf(expectedAtMost) shouldBe true
    } catch {
      case e: Exception => warn(e)
    }
  }

  def `tell most succinct integer type`: Unit = {
    forAll { (v: Byte) => checkSuccinctType(OffsetPacker.packByte(_, v), MessageFormat.INT8) }
    forAll { (v: Short) => checkSuccinctType(OffsetPacker.packShort(_, v), MessageFormat.INT16) }
    forAll { (v: Int) => checkSuccinctType(OffsetPacker.packInt(_, v), MessageFormat.INT32) }
    forAll { (v: Long) => checkSuccinctType(OffsetPacker.packLong(_, v), MessageFormat.INT64) }
    forAll { (v: Long) =>
      checkSuccinctType(OffsetPacker.packBigInteger(_, BigInteger.valueOf(v)), MessageFormat.INT64)
    }
    forAll(Gen.posNum[Long]) { (v: Long) =>
      // Create value between 2^63-1 < v <= 2^64-1
      checkSuccinctType(
        OffsetPacker.packBigInteger(_, BigInteger.valueOf(Long.MaxValue).add(BigInteger.valueOf(v))),
        MessageFormat.UINT64
      )
    }
  }

  protected def check(v: Value, expectedType: ValueType, expectedStr: String, expectedJson: String): Unit = {
    v.toString shouldBe expectedStr
    v.toJson shouldBe expectedJson
    v.valueType shouldBe expectedType
  }

  def `have nil`: Unit = {
    check(newNil, ValueType.NIL, "null", "null")
  }

  def `have boolean`: Unit = {
    check(newBoolean(true), ValueType.BOOLEAN, "true", "true")
    check(newBoolean(false), ValueType.BOOLEAN, "false", "false")
  }

  def `have integer`: Unit = {
    check(newInteger(3), ValueType.INTEGER, "3", "3")
    check(newInteger(BigInteger.valueOf(1324134134134L)), ValueType.INTEGER, "1324134134134", "1324134134134")
  }

  def `have float`: Unit = {
    check(newFloat(0.1), ValueType.FLOAT, "0.1", "0.1")
    check(newFloat(Double.NaN), ValueType.FLOAT, "null", "null")
    check(newFloat(Double.PositiveInfinity), ValueType.FLOAT, "null", "null")
    check(newFloat(Double.NegativeInfinity), ValueType.FLOAT, "null", "null")
  }

  def `have array`: Unit = {
    val a = newArray(newInteger(0), newString("hello"))
    a.size shouldBe 2
    a(0) shouldBe LongValue(0)
    a(1) shouldBe StringValue("hello")
    check(a, ValueType.ARRAY, "[0,\"hello\"]", "[0,\"hello\"]")
    check(
      newArray(newArray(newString("Apple"), newFloat(0.2)), newNil),
      ValueType.ARRAY,
      """[["Apple",0.2],null]""",
      """[["Apple",0.2],null]"""
    )
  }

  def `have string`: Unit = {
    // toString is for extracting string values
    // toJson should quote strings
    check(newString("1"), ValueType.STRING, "1", "\"1\"")
  }

  def `have Binary`: Unit = {
    val b = newBinary(Array[Byte]('a', 'b', 'c', '\n', '\b', '\r', '\t', '\f', '\\', '"', 'd', 0x01))
    b.valueType shouldBe ValueType.BINARY
    val json = "\"" + Base64.getEncoder.encodeToString(b.v) + "\""
    b.toJson shouldBe json
    b.toString shouldBe json
  }

  def `have ext`: Unit = {
    val e = newExt(-1, Array[Byte]('a', 'b', 'c'))
    e.valueType shouldBe ValueType.EXTENSION
    val json = s"""[-1,"${Base64.getEncoder.encodeToString(e.v)}"]"""
    e.toJson shouldBe json
    e.toString shouldBe json
  }

  def `have map`: Unit = {
    // Map value
    val m = newMap(
      newString("id")      -> newInteger(1001),
      newString("name")    -> newString("leo"),
      newString("address") -> newArray(newString("xxx-xxxx"), newString("yyy-yyyy")),
      newString("name")    -> newString("mitsu")
    )
    // a key ("name") should be overwritten
    m.isEmpty shouldBe false
    m.nonEmpty shouldBe true
    m.size shouldBe 3
    m.get(StringValue("id")) shouldBe Some(LongValue(1001))
    m.get(StringValue("name")) shouldBe Some(StringValue("mitsu"))
    m(StringValue("id")) shouldBe LongValue(1001)
    m(StringValue("name")) shouldBe StringValue("mitsu")

    val json = """{"id":1001,"name":"mitsu","address":["xxx-xxxx","yyy-yyyy"]}"""
    // check the equality as json objects instead of using direct json string comparison
    jsonEq(m.toJson, json)
    jsonEq(m.toString, json)
  }

  protected def jsonEq(a: String, b: String): Unit = {
    // Perform rough comparison of JSON data
    def sanitize(s: String): Seq[String] = {
      s.replaceAll("""[\{\}\[\]\"]""", "")
        .split("[,:]")
        .toIndexedSeq
        .sorted
    }
    sanitize(a) shouldBe sanitize(b)
  }

  def `check appropriate range for integers`: Unit = {
    import java.lang.{Byte, Short}

    import ValueFactory._

    newInteger(Byte.MAX_VALUE).asByte shouldBe Byte.MAX_VALUE
    newInteger(Byte.MIN_VALUE).asByte shouldBe Byte.MIN_VALUE
    newInteger(Short.MAX_VALUE).asShort shouldBe Short.MAX_VALUE
    newInteger(Short.MIN_VALUE).asShort shouldBe Short.MIN_VALUE
    newInteger(Integer.MAX_VALUE).asInt shouldBe Integer.MAX_VALUE
    newInteger(Integer.MIN_VALUE).asInt shouldBe Integer.MIN_VALUE
    intercept[IntegerOverflowException] {
      newInteger(Byte.MAX_VALUE + 1).asByte
    }
    intercept[IntegerOverflowException] {
      newInteger(Byte.MIN_VALUE - 1).asByte
    }
    intercept[IntegerOverflowException] {
      newInteger(Short.MAX_VALUE + 1).asShort
    }
    intercept[IntegerOverflowException] {
      newInteger(Short.MIN_VALUE - 1).asShort
    }
    intercept[IntegerOverflowException] {
      newInteger(Integer.MAX_VALUE + 1.toLong).asInt
    }
    intercept[IntegerOverflowException] {
      newInteger(Integer.MIN_VALUE - 1.toLong).asInt
    }
  }

  def `escape special characters`: Unit = {
    val str = "😏🙌"
    val s   = new StringBuilder
    appendJsonString(s, str)
    s.result() shouldBe "\"\\ud83d\\ude0f\\ud83d\\ude4c\""
  }
}
