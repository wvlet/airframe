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
  @inline final val LBracket = '{'
  @inline final val RBracket = '}'
  @inline final val Comma    = ','

  @inline final val DoubleQuote = '"'
  @inline final val Colon       = ':'

  @inline final val Minus = '-'
  @inline final val Plus  = '+'
  @inline final val Dot   = '.'
  @inline final val Exp   = 'e'
  @inline final val ExpL  = 'E'

  @inline final val LSquare = '['
  @inline final val RSquare = ']'

  @inline final val WS   = ' '
  @inline final val WS_T = '\t'
  @inline final val WS_N = '\n'
  @inline final val WS_R = '\r'

  @inline final val Slash     = '/'
  @inline final val BackSlash = '\\'
}

object JSONScanner {
  @inline final val DATA         = 1
  @inline final val OBJECT_START = 2
  @inline final val OBJECT_KEY   = 3
  @inline final val OBJECT_END   = 4
  @inline final val ARRAY_START  = 5
  @inline final val ARRAY_END    = 6
  @inline final val SEPARATOR    = 7

  private[json] def scan(s: JSONSource): Unit = {
    scan(s, new NullJSONContext(isObject = true))
  }

  def scan[J](s: JSONSource, handler: JSONHandler[J]): Unit = {
    val scanner = new JSONScanner(s, handler)
    scanner.scan
  }

  // Scan any json value
  def scanAny[J](s: JSONSource, ctx: JSONContext[J]): J = {
    val scanner = new JSONScanner(s, ctx)
    scanner.scanAny(ctx)
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
    val b = new Array[Long](256 / 64)
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
    val i = code & 0xff
    if ((i & 0x80) == 0) {
      // 0xxxxxxx
      0
    } else if ((i & 0xe0) == 0xc0) {
      // 110xxxxx
      1
    } else if ((i & 0xf0) == 0xe0) {
      // 1110xxxx
      2
    } else if ((i & 0xf8) == 0xf0) {
      3
    } else {
      -1
    }
  }

  private[json] final val HexChars: Array[Int] = {
    val arr = Array.fill(128)(-1)
    var i   = 0
    while (i < 10) { arr(i + '0') = i; i += 1 }
    i = 0
    while (i < 16) { arr(i + 'a') = 10 + i; arr(i + 'A') = 10 + i; i += 1 }
    arr
  }
}

class JSONScanner[J](private[this] val s: JSONSource, private[this] val handler: JSONHandler[J]) extends LogSupport {
  private[this] var cursor: Int       = 0
  private[this] var lineStartPos: Int = 0
  private[this] var line: Int         = 0
  private[this] val sb                = new StringBuilder()

  import JSONScanner._
  import JSONToken._

  private def skipWhiteSpaces: Unit = {
    var toContinue = true
    while (toContinue && cursor < s.length) {
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
    new UnexpectedToken(
      line,
      cursor - lineStartPos,
      cursor,
      f"Found '${String.valueOf(char.toChar)}' 0x${char}%02x. expected: ${expected}"
    )
  }

  def scan: Unit = {
    try {
      skipWhiteSpaces
      (s(cursor): @switch) match {
        case LBracket =>
          cursor += 1
          rscan(OBJECT_START, handler.objectContext(s, cursor - 1) :: Nil)
        case LSquare =>
          cursor += 1
          rscan(ARRAY_START, handler.arrayContext(s, cursor - 1) :: Nil)
        case other =>
          throw unexpected("object or array")
      }
    } catch {
      case e: ArrayIndexOutOfBoundsException =>
        throw new UnexpectedEOF(line, cursor - lineStartPos, cursor, s"Unexpected EOF")
    }
  }

  private final def scanObject(stack: List[JSONContext[J]]): Unit = {
    cursor += 1
    rscan(OBJECT_START, stack.head.objectContext(s, cursor - 1) :: stack)
  }
  private final def scanArray(stack: List[JSONContext[J]]): Unit = {
    cursor += 1
    rscan(ARRAY_START, stack.head.arrayContext(s, cursor - 1) :: stack)
  }

  private final def scanAny(ctx: JSONContext[J]): J = {
    skipWhiteSpaces
    if (cursor >= s.length) {
      throw new UnexpectedEOF(line, cursor - lineStartPos, cursor, "Unexpected EOF")
    }
    (s(cursor): @switch) match {
      case DoubleQuote =>
        scanString(ctx)
      case LBracket =>
        val objectCtx = ctx.objectContext(s, cursor)
        cursor += 1
        rscan(OBJECT_START, objectCtx :: Nil)
        ctx.add(objectCtx.result)
      case LSquare =>
        val arrayCtx = ctx.arrayContext(s, cursor)
        cursor += 1
        rscan(ARRAY_START, arrayCtx :: Nil)
        ctx.add(arrayCtx.result)
      case '-' | '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
        scanNumber(ctx)
      case 't' =>
        scanTrue(ctx)
      case 'f' =>
        scanFalse(ctx)
      case 'n' =>
        scanNull(ctx)
      case _ =>
        throw unexpected("unknown json token")
    }
    ctx.result
  }

  @tailrec
  private final def rscan(state: Int, stack: List[JSONContext[J]]): Unit = {
    val ch = s(cursor)
    if (ch == WS_N) {
      cursor += 1
      line += 1
      lineStartPos = cursor
      rscan(state, stack)
    } else if (ch == WS || ch == WS_T || ch == WS_R) {
      cursor += 1
      rscan(state, stack)
    } else if (state == DATA) {
      if (ch == LSquare) {
        cursor += 1
        rscan(ARRAY_START, stack.head.arrayContext(s, cursor - 1) :: stack)
      } else if (ch == LBracket) {
        cursor += 1
        rscan(OBJECT_START, stack.head.objectContext(s, cursor - 1) :: stack)
      } else {
        val ctx = stack.head
        if ((ch >= '0' && ch <= '9') || ch == '-') {
          scanNumber(ctx)
          rscan(ctx.endScannerState, stack)
        } else if (ch == DoubleQuote) {
          scanString(ctx)
          rscan(ctx.endScannerState, stack)
        } else if (ch == 't') {
          scanTrue(ctx)
          rscan(ctx.endScannerState, stack)
        } else if (ch == 'f') {
          scanFalse(ctx)
          rscan(ctx.endScannerState, stack)
        } else if (ch == 'n') {
          scanNull(ctx)
          rscan(ctx.endScannerState, stack)
        } else {
          throw unexpected("json value")
        }
      }
    } else if (
      (ch == RSquare && (state == ARRAY_END || state == ARRAY_START)) ||
      (ch == RBracket && (state == OBJECT_END || state == OBJECT_START))
    ) {
      if (stack.isEmpty) {
        throw unexpected("obj or array")
      } else {
        val ctx1 = stack.head
        val tail = stack.tail

        ctx1.closeContext(s, cursor)
        cursor += 1
        if (tail.isEmpty) {
          // root context
        } else {
          val ctx2 = tail.head
          rscan(ctx2.endScannerState, tail)
        }
      }
    } else if (state == OBJECT_KEY) {
      if (ch == DoubleQuote) {
        scanString(stack.head)
        rscan(SEPARATOR, stack)
      } else {
        throw unexpected("DoubleQuote")
      }
    } else if (state == SEPARATOR) {
      if (ch == Colon) {
        cursor += 1
        rscan(DATA, stack)
      } else {
        throw unexpected("Colon")
      }
    } else if (state == ARRAY_END) {
      if (ch == Comma) {
        cursor += 1
        rscan(DATA, stack)
      } else {
        throw unexpected("RSquare or comma")
      }
    } else if (state == OBJECT_END) {
      if (ch == Comma) {
        cursor += 1
        rscan(OBJECT_KEY, stack)
      } else {
        throw unexpected("RBracket or comma")
      }
    } else if (state == ARRAY_START) {
      rscan(DATA, stack)
    } else {
      rscan(OBJECT_KEY, stack)
    }
  }

  private def cursorChar: Byte = {
    if (cursor < s.length) {
      s(cursor)
    } else {
      -1
    }
  }

  private def scanNumber(ctx: JSONContext[J]): Unit = {
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
        ch = cursorChar
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
          ch = cursorChar
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
      if ('0' <= ch && ch <= '9') {
        while ('0' <= ch && ch <= '9') {
          cursor += 1
          ch = cursorChar
        }
      } else {
        throw unexpected("digits")
      }
    }

    val numberEnd = cursor
    if (numberStart < numberEnd) {
      ctx.addNumber(s, numberStart, numberEnd, dotIndex, expIndex)
    }
  }

  private def ensure(length: Int): Unit = {
    if (cursor + length > s.length) {
      throw new UnexpectedEOF(
        line,
        cursor - lineStartPos,
        cursor,
        s"Expected having ${length} characters, but ${s.length - cursor} is left"
      )
    }
  }

  private def scanTrue(ctx: JSONContext[J]): Unit = {
    ensure(4)
    if (s(cursor) == 't' && s(cursor + 1) == 'r' && s(cursor + 2) == 'u' && s(cursor + 3) == 'e') {
      cursor += 4
      ctx.addBoolean(s, true, cursor - 4, cursor)
    } else {
      throw unexpected("true")
    }
  }

  private def scanFalse(ctx: JSONContext[J]): Unit = {
    ensure(5)
    if (
      s(cursor) == 'f' && s(cursor + 1) == 'a' && s(cursor + 2) == 'l' && s(cursor + 3) == 's' && s(cursor + 4) == 'e'
    ) {
      cursor += 5
      ctx.addBoolean(s, false, cursor - 5, cursor)
    } else {
      throw unexpected("false")
    }
  }

  private def scanNull(ctx: JSONContext[J]): Unit = {
    ensure(4)
    if (s(cursor) == 'n' && s(cursor + 1) == 'u' && s(cursor + 2) == 'l' && s(cursor + 3) == 'l') {
      cursor += 4
      ctx.addNull(s, cursor - 4, cursor)
    } else {
      throw unexpected("null")
    }
  }

  private final def scanSimpleString: Int = {
    var i  = 0
    var ch = s(cursor + i) & 0xff
    while (ch != DoubleQuote) {
      if (ch < 0x20) {
        throw unexpected("utf8")
      }
      if (ch == BackSlash) {
        return -1
      }
      i += 1
      ch = s(cursor + i) & 0xff
    }
    i
  }

  private final def scanString(ctx: JSONContext[J]): Unit = {
    cursor += 1
    val stringStart = cursor
    val k           = scanSimpleString
    if (k != -1) {
      cursor += k + 1
      ctx.addString(s, stringStart, cursor - 1)
      return
    }

    sb.clear()
    var continue = true
    while (continue) {
      val ch = s(cursor)
      (ch: @switch) match {
        case DoubleQuote =>
          cursor += 1
          continue = false
        case BackSlash =>
          scanEscape(sb)
        case _ =>
          scanUtf8(sb)
      }
    }
    ctx.addUnescapedString(sb.result())
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

  private def scanUtf8(sb: StringBuilder): Unit = {
    val ch                = s(cursor)
    val first5bit         = (ch & 0xf8) >> 3
    val isValidUtf8Header = validUtf8BitVector & (1L << first5bit)
    if (isValidUtf8Header != 0L) {
      val pos     = (ch & 0xf0) >> (4 - 1)
      val mask    = 0x03L << pos
      val utf8len = (utf8CharLenTable & mask) >> pos
      val start   = cursor
      cursor += 1
      scanUtf8Body(utf8len.toInt)
      sb.append(s.substring(start, cursor))
    } else {
      throw unexpected("utf8")
    }
  }

  @tailrec
  private def scanUtf8Body(n: Int): Unit = {
    if (n > 0) {
      val ch = s(cursor)
      val b  = ch & 0xff
      if ((b & 0xc0) == 0x80) {
        // 10xxxxxx
        cursor += 1
        scanUtf8Body(n - 1)
      } else {
        throw unexpected("utf8 body")
      }
    }
  }

  private def scanEscape(sb: StringBuilder): Unit = {
    cursor += 1
    val ch = s(cursor)
    (ch: @switch) match {
      case DoubleQuote =>
        sb.append('"')
        cursor += 1
      case BackSlash =>
        sb.append('\\')
        cursor += 1
      case Slash =>
        sb.append('/')
        cursor += 1
      case 'b' =>
        sb.append('\b')
        cursor += 1
      case 'f' =>
        sb.append('\f')
        cursor += 1
      case 'n' =>
        sb.append('\n')
        cursor += 1
      case 'r' =>
        sb.append('\r')
        cursor += 1
      case 't' =>
        sb.append('\t')
        cursor += 1
      case 'u' =>
        cursor += 1
        val hexCode = scanHex(4, 0).toChar
        sb.append(hexCode)
      case _ =>
        throw unexpected("escape")
    }
  }

  @tailrec
  private def scanHex(n: Int, code: Int): Int = {
    if (n == 0) {
      code
    } else {
      val ch = s(cursor)
      if ((ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F') || (ch >= '0' && ch <= '9')) {
        // OK
        cursor += 1
        scanHex(n - 1, (code << 4) | HexChars(ch))
      } else {
        throw unexpected("hex")
      }
    }
  }
}
