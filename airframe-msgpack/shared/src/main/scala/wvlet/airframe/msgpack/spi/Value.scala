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

import java.nio.charset.StandardCharsets
import java.time.Instant

import wvlet.log.{LogFormatter, LogTimestampFormatter}

/**
  *
  */
trait Value {
  override def toString = toJson
  def toJson: String
  def valueType: ValueType
  def writeTo(packer: Packer)
}

object Value {
  case object NilValue extends Value {
    override def toJson    = "null"
    override def valueType = ValueType.NIL
    override def writeTo(packer: Packer): Unit = {
      packer.packNil
    }
  }
  case class BooleanValue(v: Boolean) extends Value {
    override def toJson    = if (v) "true" else "false"
    override def valueType = ValueType.BOOLEAN
    override def writeTo(packer: Packer): Unit = {
      packer.packBoolean(v)
    }
  }

  case class LongValue(v: Long) extends Value {
    override def toJson    = v.toString
    override def valueType = ValueType.INTEGER
    override def writeTo(packer: Packer): Unit = {
      packer.packLong(v)
    }
  }

  case class DoubleValue(v: Double) extends Value {
    override def toJson               = if (v.isNaN || v.isInfinite) "null" else v.toString
    override def valueType: ValueType = ValueType.FLOAT
    override def writeTo(packer: Packer): Unit = {
      packer.packDouble(v)
    }
  }

  abstract class RawValue extends Value {
    override def toJson = {
      val b = new StringBuilder
      appendJsonString(b, toRawString)
      b.result
    }
    protected def toRawString: String
  }

  case class StringValue(v: String) extends RawValue {
    override protected def toRawString: String = v
    override def valueType                     = ValueType.STRING
    override def writeTo(packer: Packer): Unit = {
      packer.packString(v)
    }
  }

  case class BinaryValue(v: Array[Byte]) extends RawValue {
    @transient private var decodedStringCache: String = null

    override def valueType: ValueType = ValueType.BINARY
    override def writeTo(packer: Packer): Unit = {
      packer.packBinaryHeader(v.length)
      packer.writePayload(v)
    }

    override protected def toRawString: String = {
      synchronized {
        if (decodedStringCache == null) {
          decodedStringCache = new String(v, StandardCharsets.UTF_8)
        }
      }
      decodedStringCache
    }
  }

  case class ExtensionValue(extType: Byte, v: Array[Byte]) extends Value {
    override def toJson = {
      val sb = new StringBuilder
      for (e <- v) {
        // Binary to HEX
        sb.append(Integer.toString(e.toInt, 16))
      }
      s"[${extType.toInt.toString},${sb.result()}]"
    }
    override def valueType: ValueType = ValueType.EXTENSION
    override def writeTo(packer: Packer): Unit = {
      packer.packExtensionTypeHeader(extType, v.length)
      packer.writePayload(v)
    }
  }

  case class TimestampValue(v: Instant) extends Value {
    override def toJson: String = {
      LogTimestampFormatter.formatTimestamp(v.toEpochMilli)
    }
    override def valueType: ValueType = ValueType.TIMESTAMP
    override def writeTo(packer: Packer): Unit = {
      packer.packTimestamp(v)
    }
  }

  case class ArrayValue(elems: IndexedSeq[Value]) extends Value {
    def apply(i: Int): Value = elems.apply(i)
    def size: Int            = elems.size

    override def toJson: String = {
      s"[${elems.map(_.toJson).mkString(",")}]"
    }
    override def valueType: ValueType = ValueType.ARRAY
    override def writeTo(packer: Packer): Unit = {
      packer.packArrayHeader(elems.length)
      elems.foreach(x => x.writeTo(packer))
    }
  }

  case class MapValue(entries: Map[Value, Value]) extends Value {
    def apply(key: Value): Value       = entries.apply(key)
    def get(key: Value): Option[Value] = entries.get(key)
    def size: Int                      = entries.size

    override def toJson: String = {
      s"{${entries.map(x => s"${x._1.toJson}:${x._2.toJson}").mkString(",")}"
    }
    override def valueType: ValueType = ValueType.MAP
    override def writeTo(packer: Packer): Unit = {
      packer.packMapHeader(entries.size)
      entries.foreach { x =>
        x._1.writeTo(packer)
        x._2.writeTo(packer)
      }
    }
  }

  private def appendJsonString(sb: StringBuilder, string: String): Unit = {
    sb.append("\"")
    var i = 0
    while ({ i < string.length }) {
      val ch = string.charAt(i)
      if (ch < 0x20) ch match {
        case '\n' =>
          sb.append("\\n")
        case '\r' =>
          sb.append("\\r")
        case '\t' =>
          sb.append("\\t")
        case '\f' =>
          sb.append("\\f")
        case '\b' =>
          sb.append("\\b")
        case _ =>
          // control chars
          escapeChar(sb, ch)
      } else if (ch <= 0x7f) ch match {
        case '\\' =>
          sb.append("\\\\")
        case '"' =>
          sb.append("\\\"")
        case _ =>
          sb.append(ch)
      } else if (ch >= 0xd800 && ch <= 0xdfff) { // surrogates
        escapeChar(sb, ch)
      } else sb.append(ch)

      { i += 1; i - 1 }
    }
    sb.append("\"")
  }

  private val HEX_TABLE = "0123456789ABCDEF".toCharArray

  private def escapeChar(sb: StringBuilder, ch: Int): Unit = {
    sb.append("\\u")
    sb.append(HEX_TABLE((ch >> 12) & 0x0f))
    sb.append(HEX_TABLE((ch >> 8) & 0x0f))
    sb.append(HEX_TABLE((ch >> 4) & 0x0f))
    sb.append(HEX_TABLE(ch & 0x0f))
  }
}
