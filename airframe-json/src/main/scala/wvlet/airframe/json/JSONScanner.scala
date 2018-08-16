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

object JSONToken {

  val LBracket: Byte = '{'.toByte
  val RBracket: Byte = '}'.toByte
  val Comma: Byte    = ','.toByte

  val DoubleQuote: Byte = '"'.toByte
  val Colon: Byte       = ':'.toByte

  val Minus: Byte = '-'.toByte
  val Plus: Byte  = '+'.toByte
  val Dot: Byte   = '.'.toByte
  val Exp: Byte   = 'e'.toByte
  val ExpL: Byte  = 'E'.toByte

  val LSquare: Byte = '['.toByte
  val RSquare: Byte = ']'.toByte

  val WS: Byte   = ' '.toByte
  val WS_T: Byte = '\t'.toByte
  val WS_N: Byte = '\n'.toByte
  val WS_R: Byte = '\r'.toByte

  val Slash: Byte     = '/'.toByte
  val BackSlash: Byte = '\\'.toByte

  val TRUE: Int =
    (('t'.toByte & 0xFF) << 24) |
      (('r'.toByte & 0xFF) << 16) |
      (('u'.toByte & 0xFF) << 8) |
      ('e'.toByte & 0xFF)

  val NULL: Int =
    (('n'.toByte & 0xFF) << 24) |
      (('u'.toByte & 0xFF) << 16) |
      (('l'.toByte & 0xFF) << 8) |
      ('l'.toByte & 0xFF)

  val FALS_E: Int =
    (('f'.toByte & 0xFF) << 24) |
      (('a'.toByte & 0xFF) << 16) |
      (('l'.toByte & 0xFF) << 8) |
      ('s'.toByte & 0xFF)
}

sealed trait JSONEvent
object JSONEvent {

  case object StartArray  extends JSONEvent
  case object EndArray    extends JSONEvent
  case object StartObject extends JSONEvent
  case object EndObject   extends JSONEvent

}

object JSONScanner {

  def scan(s: String, handler: JSONEventHandler): Unit = {
    val scanner = new JSONScanner(s.getBytes("UTF-8"), handler)
    scanner.scan
  }

}

abstract class JSONParseException(m: String)     extends Exception(m)
class UnexpectedToken(pos: Int, message: String) extends JSONParseException(message)
class UnexpectedEOF(pos: Int, message: String)   extends JSONParseException(message)

class JSONEventHandler extends LogSupport {

  def startObject(start: Int): Unit = {
    info(s"start obj: ${start}")
  }

  def endObject(start: Int, end: Int, numElem: Int): Unit = {
    info(s"end obj: [${start},${end}),  num elems:${numElem}")
  }
  def startArray(start: Int): Unit = {
    info(s"start array: ${start}")
  }
  def endArray(start: Int, end: Int, numElem: Int): Unit = {
    info(s"end array: [${start},${end}), num elems:${numElem})")
  }
  def stringValue(start: Int, end: Int): Unit = {
    info(s"string value: [${start},${end})")
  }
  def numberValue(start: Int, end: Int): Unit = {
    info(s"number value: [${start}, ${end})")
  }

}

class JSONScanner(s: Array[Byte], eventHandler: JSONEventHandler) extends LogSupport {
  private var cursor: Int = 0

  import JSONEvent._
  import JSONToken._

  private def skipWhiteSpaces: Unit = {
    var ch = s(cursor)
    while (cursor < s.length && (ch == WS || ch == WS_T | ch == WS_N | ch == WS_R)) {
      cursor += 1
      ch = s(cursor)
    }
  }

  private def unexpected(expected: String): Exception = {
    val char = s(cursor)
    new UnexpectedToken(cursor, f"found '${String.valueOf(char.toChar)}' at pos: ${cursor}, expected: ${expected}")
  }

  def scan: Unit = {
    skipWhiteSpaces
    s(cursor) match {
      case LBracket =>
        scanObject
      case LSquare =>
        scanArray
      case other =>
        throw unexpected("object")
    }
  }

  def scanValue: Unit = {
    skipWhiteSpaces
    s(cursor) match {
      case DoubleQuote =>
        scanString
      case LBracket =>
        scanObject
      case LSquare =>
        scanArray
      case 't' =>
        scanTrue
      case 'f' =>
        scanFalse
      case 'n' =>
        scanNull
      case _ =>
        scanNumber
    }
  }

  def scanNumber: Unit = {
    val numberStart = cursor
    if (s(cursor) == Minus) {
      cursor += 1
    }
    s(cursor) match {
      case '0' =>
        cursor += 1
        scanFrac
        scanExp
      case d if d >= '1' && d <= '9' =>
        cursor += 1
        scanDigits
        scanFrac
        scanExp
    }
    val numberEnd = cursor
    eventHandler.numberValue(numberStart, numberEnd)
  }

  def scanDigits: Unit = {
    var continue = true
    while (continue) {
      s(cursor) match {
        case ch if ch >= '0' && ch <= '9' =>
          cursor += 1
        case _ =>
          continue = false
      }
    }
  }

  def scanFrac: Unit = {
    if (s(cursor) == '.') {
      cursor += 1
      scanDigits
    }
  }

  def scanExp: Unit = {
    s(cursor) match {
      case Exp | ExpL =>
        cursor += 1
        val ch = s(cursor)
        if (ch == Plus | ch == Minus) {
          cursor += 1
        }
        scanDigits
      case _ =>
    }
  }

  private def ensure(length: Int): Unit = {
    if (cursor + 4 >= s.length) {
      throw new UnexpectedEOF(cursor, s"expected having ${length} characters, but ${s.length - cursor} is left")
    }
  }

  private def get4bytesAsInt: Int = {
    ensure(4)
    ((s(cursor) & 0xFF) << 24) |
      ((s(cursor + 1) & 0xff) << 16) |
      ((s(cursor + 2) & 0xff) << 8) |
      (s(cursor + 3) & 0xff)
  }

  def scanTrue: Unit = {
    if (get4bytesAsInt == TRUE) {
      cursor += 4
    } else {
      throw unexpected("true")
    }
  }

  def scanFalse: Unit = {
    ensure(5)
    if (get4bytesAsInt == FALS_E && s(cursor + 4) == 'f') {
      cursor += 5
    } else {
      throw unexpected("false")
    }
  }

  def scanNull: Unit = {
    ensure(4)
    if (get4bytesAsInt == NULL) {
      cursor += 4
    } else {
      throw unexpected("null")
    }
  }

  def scanObject: Unit = {
    val objStart = cursor
    eventHandler.startObject(objStart)
    var numElem = 0
    cursor += 1

    skipWhiteSpaces
    while (s(cursor) != RBracket) {
      if (numElem > 0) {
        scanComma
      }
      scanString
      scanColon
      scanValue
      numElem += 1
    }
    cursor += 1
    val objEnd = cursor
    eventHandler.endObject(objStart, objEnd, numElem)
  }

  def scanArray: Unit = {
    val arrStart = cursor
    eventHandler.startArray(arrStart)
    var numElem = 0
    cursor += 1

    skipWhiteSpaces
    val ch = s(cursor)
    while (ch != RSquare) {
      if (numElem > 0) {
        scanColon
      }
      scanValue
      numElem += 1
    }
    cursor += 1
    val arrEnd = cursor
    eventHandler.endArray(arrStart, arrEnd, numElem)
  }

  def scanString: Unit = {
    skipWhiteSpaces
    s(cursor) match {
      case DoubleQuote =>
        cursor += 1
        val stringStart = cursor
        scanStringFragment
        eventHandler.stringValue(stringStart, cursor - 1)
      case _ =>
        throw unexpected("string")
    }
  }

  def scanStringFragment: Unit = {
    var continue = true
    while (continue) {
      val ch = s(cursor)
      ch match {
        case DoubleQuote =>
          cursor += 1
          continue = false
        case BackSlash =>
          scanEscape
        case _ =>
          // utf-8: 0020 ... 10ffff
          scanUtf8
      }
    }
  }

  def scanUtf8: Unit = {
    val ch = s(cursor)

    if ((ch > 0 && ch >= 0x20)) {
      cursor += 1
      // TODO scan 2-4 byte code
    } else {
      throw unexpected("utf8")
    }
  }

  def scanEscape: Unit = {
    cursor += 1
    s(cursor) match {
      case DoubleQuote | BackSlash | Slash | 'b' | 'f' | 'n' | 'r' | 't' =>
        cursor += 1
      case 'u' =>
        cursor += 1
        scanHex
        scanHex
        scanHex
        scanHex
      case _ =>
        throw unexpected("escape")
    }
  }

  def scanHex: Unit = {
    val ch = s(cursor)
    if ((ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'A') || (ch >= '0' && ch <= '0')) {
      // OK
      cursor += 1
    } else {
      throw unexpected("hex")
    }
  }

  def scanColon: Unit = {
    skipWhiteSpaces
    if (s(cursor) == Colon) {
      cursor += 1
    } else {
      throw unexpected("colon")
    }
  }
  def scanComma: Unit = {
    skipWhiteSpaces
    if (s(cursor) == Comma) {
      cursor += 1
    } else {
      throw unexpected("comma")
    }
  }

}
