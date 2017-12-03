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

/**
  *
  */
trait Value {
  def toJson: String
  def valueType: ValueType
  def toImmutable: Value

  def writeTo(packer: Packer)
//  def isNil: Boolean
//  def isBoolean: Boolean
//  def isNumber: Boolean
//  def isInteger: Boolean
//  def isFloat: Boolean
//  def isRaw: Boolean
//  def isBinary: Boolean
//  def isString: Boolean
//  def isArray: Boolean
//  def isMap: Boolean
}

object Value {
  case object NilValue extends Value {
    def toJson             = "null"
    def valueType          = ValueType.NIL
    def toImmutable: Value = this
    def writeTo(packer: Packer): Unit = {
      packer.packNil
    }
  }
  case class BooleanValue(v: Boolean) extends Value {
    def toJson             = if (v) "true" else "false"
    def valueType          = ValueType.BOOLEAN
    def toImmutable: Value = this
    def writeTo(packer: Packer): Unit = {
      packer.packBoolean(v)
    }
  }

  case class StringValue(v: String) extends Value {
    def toJson = {
      val b = new StringBuilder
      appendJsonString(b, v)
      b.result
    }

    def valueType          = ValueType.STRING
    def toImmutable: Value = this
    def writeTo(packer: Packer): Unit = {
      packer.packString(v)
    }
  }

  private[impl] def appendJsonString(sb: StringBuilder, string: String): Unit = {
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

class Variable extends Value {
  // TODO impl
  def toJson: String                         = ""
  override def valueType: ValueType          = ???
  override def toImmutable: Value            = ???
  override def writeTo(packer: Packer): Unit = ???
}
