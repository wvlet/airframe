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
package wvlet.airframe.tablet.text

import org.msgpack.core.MessageUnpacker
import org.msgpack.value.ValueType
import wvlet.airframe.tablet.{Record, TabletWriter}

import scala.util.parsing.json.JSONFormat

object TextTabletWriter {

  trait RecordFormatter {
    def sanitize(s: String): String         = s
    def sanitizeEmbedded(s: String): String = sanitize(s)
    def format(record: Seq[String]): String

    def quote(s: String) = {
      val b = new StringBuilder(s.length + 2)
      b.append("\"")
      b.append(s)
      b.append("\"")
      b.result
    }
  }

  object JSONRecordFormatter extends RecordFormatter {
    override def sanitize(s: String): String         = quote(JSONFormat.quoteString(s))
    override def sanitizeEmbedded(s: String): String = s
    override def format(record: Seq[String]): String = {
      s"[${record.mkString(", ")}]"
    }
  }

  object TSVRecordFormatter extends RecordFormatter {
    override def sanitize(s: String): String = {
      s.map {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case c    => c
      }.mkString
    }

    override def format(record: Seq[String]): String = {
      record.mkString("\t")
    }
  }

  object CSVRecordFormatter extends RecordFormatter {
    override def sanitize(s: String): String = {
      var hasComma = false
      val sanitized = s.map {
        case '\n' => "\\n"
        case '\r' => "\\r"
        case ',' =>
          hasComma = true
          ','
        case c => c
      }.mkString
      if (hasComma) quote(sanitized) else sanitized
    }

    override def format(record: Seq[String]): String = {
      record.mkString(",")
    }
  }

}

import wvlet.airframe.tablet.text.TextTabletWriter._

/**
  *
  */
class TabletPrinter(val formatter: RecordFormatter) extends TabletWriter[String] {

  def read(unpacker: MessageUnpacker, depth: Int): String = {
    if (!unpacker.hasNext) {
      ""
    } else {
      val f = unpacker.getNextFormat
      f.getValueType match {
        case ValueType.NIL =>
          unpacker.unpackNil
          // TODO Switch output mode: empty string or "null"
          "null"
        case ValueType.BOOLEAN =>
          val b = unpacker.unpackBoolean()
          if (b) "true" else "false"
        case ValueType.INTEGER =>
          unpacker.unpackLong.toString
        case ValueType.FLOAT =>
          unpacker.unpackDouble.toString
        case ValueType.STRING =>
          val s = unpacker.unpackString
          formatter.sanitize(s)
        case ValueType.BINARY =>
          val b = unpacker.unpackValue()
          // TODO formatting
          formatter.sanitize(b.toJson)
        case ValueType.ARRAY =>
          val arrSize = unpacker.unpackArrayHeader()
          val r       = Seq.newBuilder[String]
          var i       = 0
          while (i < arrSize) {
            val col = read(unpacker, depth + 1)
            r += col
            i += 1
          }
          if (depth < 1) {
            formatter.format(r.result())
          } else {
            formatter.sanitizeEmbedded(formatter.format(r.result()))
          }
        case ValueType.MAP =>
          formatter.sanitizeEmbedded(unpacker.unpackValue().toJson)
        case ValueType.EXTENSION =>
          "null"
      }
    }
  }

  def write(record: Record): String = {
    val s = Seq.newBuilder[String]
    read(record.unpacker, 0)
  }
  override def close(): Unit = {}
}

object JSONTabletPrinter extends TabletPrinter(JSONRecordFormatter)
object CSVTabletPrinter  extends TabletPrinter(CSVRecordFormatter)
object TSVTabletPrinter  extends TabletPrinter(TSVRecordFormatter)
