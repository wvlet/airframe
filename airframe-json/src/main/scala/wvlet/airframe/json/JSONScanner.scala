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

import scala.annotation.{switch, tailrec}

object JSONToken {

  final val LBracket = '{'
  final val RBracket = '}'
  final val Comma    = ','

  final val DoubleQuote = '"'
  final val Colon       = ':'

  final val Minus = '-'
  final val Plus  = '+'
  final val Dot   = '.'
  final val Exp   = 'e'
  final val ExpL  = 'E'

  final val LSquare = '['
  final val RSquare = ']'

  final val WS   = ' '
  final val WS_T = '\t'
  final val WS_N = '\n'
  final val WS_R = '\r'

  final val Slash     = '/'
  final val BackSlash = '\\'
}

object JSONScanner {

  def scan(s: JSONSource, handler: JSONEventHandler): Unit = {
    val scanner = new JSONScanner(s, handler)
    scanner.scan
  }

  // 2-bit vector of utf8 character length table
  // Using the first 4-bits of an utf8 string
  private[json] final val utf8CharLenTable: Long = {
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
  private[json] final val validUtf8BitVector: Long = {
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

  private[json] final val whiteSpaceBitVector: Array[Long] = {
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
    while (toContinue) {
      val ch = s(cursor)
      (ch: @switch) match {
        case WS | WS_T | WS_R =>
          cursor += 1
        case WS_N =>
          cursor += 1
          line += 1
          lineStartPos = cursor
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
      eventHandler.startJson(s, cursor)
      s(cursor) match {
        case LBracket =>
          scanObject
        case LSquare =>
          scanArray
        case other =>
          throw unexpected("object or array")
      }
      eventHandler.endJson(s, cursor)
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        throw new UnexpectedEOF(line, cursor - lineStartPos, cursor, s"Unexpected EOF")
    }
  }

  private[this] final def scanValue: Unit = {
    (s(cursor): @switch) match {
      case WS | WS_T | WS_R =>
        cursor += 1
        scanValue
      case WS_N =>
        cursor += 1
        line += 1
        lineStartPos = cursor
        scanValue
      case DoubleQuote =>
        scanString
      case LBracket =>
        scanObject
      case LSquare =>
        scanArray
      case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
        scanNumber
      case 't' =>
        scanTrue
      case 'f' =>
        scanFalse
      case 'n' =>
        scanNull
      case _ =>
        throw unexpected("object or array")
    }
  }

  private def scanNumber: Unit = {
    val numberStart = cursor

    var ch = s(cursor)
    if (ch == Minus) {
      cursor += 1
      ch = s(cursor)
    }

    if (ch == '0') {
      cursor += 1
      ch = s(cursor)
    } else if ('1' <= ch && ch <= '9') {
      while ('0' <= ch && ch <= '9') {
        cursor += 1
        ch = s(cursor)
      }
    } else {
      throw unexpected("digits")
    }

    // frac
    var dotIndex = -1
    if (ch == '.') {
      dotIndex = cursor
      cursor += 1
      ch = s(cursor)
      if ('0' <= ch && ch <= '9') {
        while ('0' <= ch && ch <= '9') {
          cursor += 1
          ch = s(cursor)
        }
      } else {
        throw unexpected("digist")
      }
    }

    // exp
    var expIndex = -1
    if (ch == Exp || ch == ExpL) {
      expIndex = cursor
      cursor += 1
      ch = s(cursor)
      if (ch == Plus | ch == Minus) {
        cursor += 1
        ch = s(cursor)
      }
      if (ch >= '1' && ch <= '9') {
        while ('0' <= ch && ch <= '9') {
          cursor += 1
          ch = s(cursor)
        }
      } else {
        throw unexpected("digits")
      }
    }

    val numberEnd = cursor
    if (numberStart < numberEnd) {
      eventHandler.numberValue(s, numberStart, numberEnd, dotIndex, expIndex)
    }
  }

  private def scanDigits: Unit = {
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

  private def ensure(length: Int): Unit = {
    if (cursor + length >= s.length) {
      throw new UnexpectedEOF(line,
                              cursor - lineStartPos,
                              cursor,
                              s"Expected having ${length} characters, but ${s.length - cursor} is left")
    }
  }

  private def scanTrue: Unit = {
    ensure(4)
    if (s(cursor) == 't' && s(cursor + 1) == 'r' && s(cursor + 2) == 'u' && s(cursor + 3) == 'e') {
      cursor += 4
      eventHandler.booleanValue(s, true, cursor - 4, cursor)
    } else {
      throw unexpected("true")
    }
  }

  private def scanFalse: Unit = {
    ensure(5)
    if (s(cursor) == 'f' && s(cursor + 1) == 'a' && s(cursor + 2) == 'l' && s(cursor + 3) == 's' && s(cursor + 4) == 'e') {
      cursor += 5
      eventHandler.booleanValue(s, false, cursor - 5, cursor)
    } else {
      throw unexpected("false")
    }
  }

  private def scanNull: Unit = {
    ensure(4)
    if (s(cursor) == 'n' && s(cursor + 1) == 'u' && s(cursor + 2) == 'l' && s(cursor + 3) == 'l') {
      cursor += 4
      eventHandler.nullValue(s, cursor - 4, cursor)
    } else {
      throw unexpected("null")
    }
  }

  private def scanObject: Unit = {
    val objStart = cursor
    eventHandler.startObject(s, objStart)
    var numElem = 0
    cursor += 1

    skipWhiteSpaces
    var ch = s(cursor)
    while (ch != RBracket) {
      if (numElem > 0) {
        if (ch == Comma) {
          cursor += 1
        } else {
          throw unexpected("comma")
        }
      }
      scanString
      scanColon
      scanValue
      numElem += 1
      skipWhiteSpaces
      ch = s(cursor)
    }
    cursor += 1
    val objEnd = cursor
    eventHandler.endObject(s, objStart, objEnd, numElem)
  }

  private def scanArray: Unit = {
    val arrStart = cursor
    eventHandler.startArray(s, arrStart)
    var numElem = 0
    cursor += 1

    skipWhiteSpaces
    var ch = s(cursor)
    while (s(cursor) != RSquare) {
      if (numElem > 0) {
        if (ch == Comma) {
          cursor += 1
        } else {
          throw unexpected("comma")
        }
      }
      scanValue
      numElem += 1
      skipWhiteSpaces
      ch = s(cursor)
    }
    cursor += 1
    val arrEnd = cursor
    eventHandler.endArray(s, arrStart, arrEnd, numElem)
  }

  private def scanString: Unit = {
    skipWhiteSpaces
    s(cursor) match {
      case DoubleQuote =>
        cursor += 1
        val stringStart = cursor

        var continue = true
        while (continue) {
          val ch = s(cursor)
          (ch: @switch) match {
            case DoubleQuote =>
              cursor += 1
              continue = false
            case BackSlash =>
              scanEscape
            case _ =>
              scanUtf8
          }
        }
        eventHandler.stringValue(s, stringStart, cursor - 1)
      case _ =>
        throw unexpected("string")
    }
  }

//  def scanUtf8_slow: Unit = {
//    // utf-8: 0020 ... 10ffff
//    val ch = s(cursor)
//    val b1 = ch & 0xFF
//    if ((b1 & 0x80) == 0 && ch >= 0x20) {
//      // 0xxxxxxx
//      cursor += 1
//    } else if ((b1 & 0xE0) == 0xC0) {
//      // 110xxxxx
//      cursor += 1
//      scanUtf8Body(1)
//    } else if ((b1 & 0xF0) == 0xE0) {
//      // 1110xxxx
//      cursor += 1
//      scanUtf8Body(2)
//    } else if ((b1 & 0xF8) == 0xF0) {
//      // 11110xxx
//      cursor += 1
//      scanUtf8Body(3)
//    } else {
//      throw unexpected("utf8")
//    }
//  }

  def scanUtf8: Unit = {
    val ch                = s(cursor)
    val first5bit         = (ch & 0xF8) >> 3
    val isValidUtf8Header = (validUtf8BitVector & (1L << first5bit))
    if (isValidUtf8Header != 0L) {
      val pos     = (ch & 0xF0) >> (4 - 1)
      val mask    = 0x03L << pos
      val utf8len = (utf8CharLenTable & mask) >> pos
      cursor += 1
      scanUtf8Body(utf8len.toInt)
    } else {
      throw unexpected("utf8")
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
    (s(cursor): @switch) match {
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
