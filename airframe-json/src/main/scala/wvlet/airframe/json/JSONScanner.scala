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

import scala.annotation.tailrec

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

  val FALSE: Long =
    (('f'.toByte & 0xFFL) << 32) |
      (('a'.toByte & 0xFFL) << 24) |
      (('l'.toByte & 0xFFL) << 16) |
      (('s'.toByte & 0xFFL) << 8) |
      ('e'.toByte & 0xFFL)
}

sealed trait JSONEvent
object JSONEvent {

  case object StartArray  extends JSONEvent
  case object EndArray    extends JSONEvent
  case object StartObject extends JSONEvent
  case object EndObject   extends JSONEvent

}

object JSONScanner {

  def scan(s: JSONSource, handler: JSONEventHandler): Unit = {
    val scanner = new JSONScanner(s, handler)
    scanner.scan
  }

  // 2-bit vector of utf8 character length table
  // Using the first 4-bits of an utf8 string
  private[json] val utf8CharLenTable: Long = {
    var l: Long = 0L
    for (i <- 0 until 16) {
      val code = i << 4
      val len  = utf8CharLen(code)
      if (len > 0) {
        l = l | (len << (i * 2))
      }
    }
    l
  }
  // Check the valid utf8 by using the first 5-bits of utf8 string
  private[json] val validUtf8BitVector: Long = {
    var l: Long = 0L
    for (i <- 0 until 32) {
      val code = i << 3
      val len  = utf8CharLen(code)
      if ((len == 0 && code >= 0x20) || len >= 0) {
        l = l | (1L << i)
      }
    }
    l
  }

  private[json] val whiteSpaceBitVector: Array[Long] = {
    var b = new Array[Long](256 / 64)
    for (i <- 0 until 256) {
      import JSONToken._
      i match {
        case WS | WS_T | WS_R | WS_N =>
          b(i / 64) |= (1L << (i % 64))
        case _ =>
      }
    }
    b
  }

  private def utf8CharLen(code: Int): Int = {
    val i = code & 0xFF
    if ((i & 0x80) == 0) {
      // 0xxxxxxx
      0
    } else if ((i & 0xE0) == 0xC0) {
      // 110xxxxx
      1
    } else if ((i & 0xF0) == 0xE0) {
      // 1110xxxx
      2
    } else if ((i & 0xF8) == 0xF0) {
      3
    } else {
      -1
    }
  }
}

class JSONScanner(s: JSONSource, eventHandler: JSONEventHandler) extends LogSupport {
  private var cursor: Int       = 0
  private var lineStartPos: Int = 0
  private var line: Int         = 0

  import JSONToken._
  import JSONScanner._

  private def skipWhiteSpaces: Unit = {
    var toContinue = true
    while (toContinue && cursor < s.length) {
      val ch = s(cursor)
      ch match {
        case WS | WS_T | WS_R | WS_N =>
          cursor += 1
          if (ch == WS_N) {
            line += 1
            lineStartPos = cursor
          }
        case _ =>
          toContinue = false
      }
    }
  }

  private def unexpected(expected: String): Exception = {
    val char = s(cursor)
    new UnexpectedToken(line,
                        cursor - lineStartPos,
                        cursor,
                        f"Found '${String.valueOf(char.toChar)}' 0x${char}%02x. expected: ${expected}")
  }

  def scan: Unit = {
    try {
      skipWhiteSpaces
      s(cursor) match {
        case LBracket =>
          scanObject
        case LSquare =>
          scanArray
        case other =>
          throw unexpected("object")
      }
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        throw new UnexpectedEOF(line, cursor - lineStartPos, cursor, s"Unexpected EOF")
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
    eventHandler.numberValue(s, numberStart, numberEnd)
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
    if (cursor + length >= s.length) {
      throw new UnexpectedEOF(line,
                              cursor - lineStartPos,
                              cursor,
                              s"Expected having ${length} characters, but ${s.length - cursor} is left")
    }
  }

  private def get4bytesAsInt: Int = {
    ensure(4)
    ((s(cursor) & 0xFF) << 24) |
      ((s(cursor + 1) & 0xFF) << 16) |
      ((s(cursor + 2) & 0xFF) << 8) |
      (s(cursor + 3) & 0xFF)
  }

  private def get5bytesAsLong: Long = {
    ensure(5)
    ((s(cursor) & 0xFFL) << 32) |
      ((s(cursor + 1) & 0xFFL) << 24) |
      ((s(cursor + 2) & 0xFFL) << 16) |
      ((s(cursor + 3) & 0xFFL) << 8) |
      (s(cursor + 4) & 0xFFL)
  }

  def scanTrue: Unit = {
    if (get4bytesAsInt == TRUE) {
      cursor += 4
      eventHandler.booleanValue(s, true, cursor - 4, cursor)
    } else {
      throw unexpected("true")
    }
  }

  def scanFalse: Unit = {
    if (get5bytesAsLong == FALSE) {
      cursor += 5
      eventHandler.booleanValue(s, false, cursor - 5, cursor)
    } else {
      throw unexpected("false")
    }
  }

  def scanNull: Unit = {
    ensure(4)
    if (get4bytesAsInt == NULL) {
      cursor += 4
      eventHandler.nullValue(s, cursor - 4, cursor)
    } else {
      throw unexpected("null")
    }
  }

  def scanObject: Unit = {
    val objStart = cursor
    eventHandler.startObject(s, objStart)
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
      skipWhiteSpaces
    }
    cursor += 1
    val objEnd = cursor
    eventHandler.endObject(s, objStart, objEnd, numElem)
  }

  def scanArray: Unit = {
    val arrStart = cursor
    eventHandler.startArray(s, arrStart)
    var numElem = 0
    cursor += 1

    skipWhiteSpaces
    while (s(cursor) != RSquare) {
      if (numElem > 0) {
        scanComma
      }
      scanValue
      numElem += 1
      skipWhiteSpaces
    }
    cursor += 1
    val arrEnd = cursor
    eventHandler.endArray(s, arrStart, arrEnd, numElem)
  }

  def scanString: Unit = {
    skipWhiteSpaces
    s(cursor) match {
      case DoubleQuote =>
        cursor += 1
        val stringStart = cursor
        scanStringFragment
        eventHandler.stringValue(s, stringStart, cursor - 1)
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
          scanUtf8
      }
    }
  }

  def scanUtf8_slow: Unit = {
    // utf-8: 0020 ... 10ffff
    val ch = s(cursor)
    val b1 = ch & 0xFF
    if ((b1 & 0x80) == 0 && ch >= 0x20) {
      // 0xxxxxxx
      cursor += 1
    } else if ((b1 & 0xE0) == 0xC0) {
      // 110xxxxx
      cursor += 1
      scanUtf8Body(1)
    } else if ((b1 & 0xF0) == 0xE0) {
      // 1110xxxx
      cursor += 1
      scanUtf8Body(2)
    } else if ((b1 & 0xF8) == 0xF0) {
      // 11110xxx
      cursor += 1
      scanUtf8Body(3)
    } else {
      throw unexpected("utf8")
    }
  }

  def scanUtf8: Unit = {
    val ch = s(cursor)
    if (ch <= 0x7F && ch >= 0x20) {
      // Fast path for 1-byte utf8 chars
      cursor += 1
    } else {
      val first5bit         = (ch & 0xF8) >> 3
      val isValidUtf8Header = (validUtf8BitVector & (1L << first5bit))
      val pos               = (ch & 0xF0) >> (4 - 1)
      val mask              = 0x03L << pos
      val utf8len           = (utf8CharLenTable & mask) >> pos
      if (isValidUtf8Header != 0L) {
        cursor += 1
        scanUtf8Body(utf8len.toInt)
      } else {
        throw unexpected("utf8")
      }
    }
  }

  @tailrec
  private def scanUtf8Body(n: Int): Unit = {
    if (n > 0) {
      val ch = s(cursor)
      val b  = ch & 0xFF
      if ((b & 0xC0) == 0x80) {
        // 10xxxxxx
        cursor += 1
        scanUtf8Body(n - 1)
      } else {
        throw unexpected("utf8 body")
      }
    }
  }

  private def scanEscape: Unit = {
    cursor += 1
    s(cursor) match {
      case DoubleQuote | BackSlash | Slash | 'b' | 'f' | 'n' | 'r' | 't' =>
        cursor += 1
      case 'u' =>
        cursor += 1
        scanHex(4)
      case _ =>
        throw unexpected("escape")
    }
  }

  @tailrec
  private def scanHex(n: Int): Unit = {
    if (n > 0) {
      val ch = s(cursor)
      if ((ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F') || (ch >= '0' && ch <= '9')) {
        // OK
        cursor += 1
        scanHex(n - 1)
      } else {
        throw unexpected("hex")
      }
    }
  }

  private def scanColon: Unit = {
    skipWhiteSpaces
    if (s(cursor) == Colon) {
      cursor += 1
    } else {
      throw unexpected("colon")
    }
  }

  private def scanComma: Unit = {
    skipWhiteSpaces
    if (s(cursor) == Comma) {
      cursor += 1
    } else {
      throw unexpected("comma")
    }
  }

}
