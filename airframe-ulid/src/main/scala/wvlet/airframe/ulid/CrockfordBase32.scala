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
package wvlet.airframe.ulid

/**
  */
object CrockfordBase32 {
  private val ENCODING_CHARS: Array[Char] = Array(
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P',
    'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
  )

  private val DECODING_CHARS: Array[Byte] = Array[Byte](
    -1, -1, -1, -1, -1, -1, -1, -1, // 0
    -1, -1, -1, -1, -1, -1, -1, -1, // 8
    -1, -1, -1, -1, -1, -1, -1, -1, // 16
    -1, -1, -1, -1, -1, -1, -1, -1, // 24
    -1, -1, -1, -1, -1, -1, -1, -1, // 32
    -1, -1, -1, -1, -1, -1, -1, -1, // 40
    0, 1, 2, 3, 4, 5, 6, 7,         // 48
    8, 9, -1, -1, -1, -1, -1, -1,   // 56
    -1, 10, 11, 12, 13, 14, 15, 16, // 64
    17, 1, 18, 19, 1, 20, 21, 0,    // 72
    22, 23, 24, 25, 26, -1, 27, 28, // 80
    29, 30, 31, -1, -1, -1, -1, -1, // 88
    -1, 10, 11, 12, 13, 14, 15, 16, // 96
    17, 1, 18, 19, 1, 20, 21, 0,    // 104
    22, 23, 24, 25, 26, -1, 27, 28, // 112
    29, 30, 31                      // 120
  )

  @inline def decode(ch: Char): Byte = DECODING_CHARS(ch & 0x7f)
  @inline def encode(i: Int): Char   = ENCODING_CHARS(i & 0x1f)
  def indexOf(ch: Char): Int         = ENCODING_CHARS.indexOf(ch)

  def decode128bits(s: String): (Long, Long) = {

    /**
      * |      hi (64-bits)     |    low (64-bits)    |
      * |--|                  |----|                  |
      */
    val len = s.length
    if (len != 26) {
      throw new IllegalArgumentException(s"String length must be 26: ${s} (length: ${len})")
    }
    var i         = 0
    var hi        = 0L
    var low       = 0L
    val carryMask = ~(~0L >>> 5)
    while (i < 26) {
      val v     = decode(s.charAt(i))
      val carry = (low & carryMask) >>> (64 - 5)
      low <<= 5
      low |= v
      hi <<= 5
      hi |= carry
      i += 1
    }
    (hi, low)
  }

  def encode128bits(hi: Long, low: Long): String = {
    val s = new StringBuilder(26)
    var i = 0
    var h = hi
    var l = low
    while (i < 26) {
      s += encode((l & 0x1fL).toInt)
      val carry = (h & 0x1fL) << (64 - 5)
      l >>>= 5
      l |= carry
      h >>>= 5
      i += 1
    }
    s.reverseContents().toString()
  }

  def encodeLong(l: Long, len: Int): String = {
    val max = if (len > 12) Long.MaxValue else 1L << (len * 5)
    if (l > max) {
      throw new IllegalArgumentException(f"Cannot encode ${l}%,d with ${len} characters.")
    }
    val s      = new StringBuilder
    var cursor = len
    while (cursor > 12) {
      s += encode(0)
      cursor -= 1
    }
    while (cursor >= 0) {
      val mask = 0x1fL << (cursor * 5)
      val v    = (l & mask) >>> (cursor * 5)
      s += encode(v.toInt)
      cursor -= 1
    }
    s.result()
  }

  def decodeAsLong(s: String): Long = {
    val len = s.length
    if (len > 13) {

      throw new IllegalArgumentException(
        s"Cannot decode Base32 string longer than 12 characters with 64-bit Long: ${s}"
      )
    }

    if (len == 0) {
      0L
    } else {
      var l: Long = decode(s.charAt(0))
      var i       = 1
      while (i < len) {
        l <<= 5
        l |= decode(s.charAt(i))
        i += 1
      }
      l
    }
  }
}
