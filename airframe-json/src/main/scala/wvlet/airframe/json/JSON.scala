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
package wvlet.airframe.json

import wvlet.log.LogSupport

/**
  *
  */
object JSON extends LogSupport {

  def parse(s: String): JSONValue = {
    parse(JSONSource.fromString(s))
  }
  def parse(s: Array[Byte]): JSONValue = {
    parse(JSONSource.fromBytes(s))
  }
  def parse(s: Array[Byte], offset: Int, length: Int): JSONValue = {
    parse(JSONSource.fromBytes(s, offset, length))
  }
  def parse(s: JSONSource): JSONValue = {
    val b = new JSONValueBuilder().singleContext(s, 0)
    JSONScanner.scan(s, b)
    val j = b.result
    j
  }

  sealed trait JSONValue {
    override def toString = toJSON
    def toJSON: String
  }

  case object JSONNull extends JSONValue {
    override def toJSON: String = "null"
  }

  case class JSONBoolean(val v: Boolean) extends JSONValue {
    override def toJSON: String = if (v) "true" else "false"
  }

  val JSONTrue  = JSONBoolean(true)
  val JSONFalse = JSONBoolean(false)

  trait JSONNumber extends JSONValue
  case class JSONDouble(v: Double) extends JSONNumber {
    override def toJSON: String = v.toString
  }
  case class JSONLong(v: Long) extends JSONNumber {
    override def toJSON: String = v.toString
  }
  case class JSONString(v: String) extends JSONValue {
    override def toString = v
    override def toJSON: String = {
      val s = new StringBuilder(v.length + 2)
      s.append("\"")
      s.append(quoteJSONString(v))
      s.append("\"")
      s.result()
    }
  }

  case class JSONObject(v: Seq[(String, JSONValue)]) extends JSONValue {
    override def toJSON: String = {
      val s = new StringBuilder
      s.append("{")
      s.append {
        v.map {
            case (k, v: JSONValue) =>
              val ss = new StringBuilder
              ss.append("\"")
              ss.append(quoteJSONString(k))
              ss.append("\":")
              ss.append(v.toJSON)
              ss.result()
          }.mkString(",")
      }
      s.append("}")
      s.result()
    }
  }
  case class JSONArray(v: IndexedSeq[JSONValue]) extends JSONValue {
    override def toJSON: String = {
      val s = new StringBuilder
      s.append("[")
      s.append(v.map(x => x.toJSON).mkString(","))
      s.append("]")
      s.result()
    }
  }

  /**
    * This function can be used to properly quote Strings
    * for JSON output.
    */
  def quoteJSONString(s: String): String = {
    s.map {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '/'  => "\\/"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      /* We'll unicode escape any control characters. These include:
       * 0x0 -> 0x1f  : ASCII Control (C0 Control Codes)
       * 0x7f         : ASCII DELETE
       * 0x80 -> 0x9f : C1 Control Codes
       *
       * Per RFC4627, section 2.5, we're not technically required to
       * encode the C1 codes, but we do to be safe.
       */
      case c if ((c >= '\u0000' && c <= '\u001f') || (c >= '\u007f' && c <= '\u009f')) => "\\u%04x".format(c.toInt)
      case c                                                                           => c
    }.mkString
  }
}
