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

object Code {
  def isFixInt(b: Byte): Boolean = {
    val v = b & 0xFF
    v <= 0x7f || v >= 0xe0
  }

  def isPosFixInt(b: Byte): Boolean =
    (b & POSFIXINT_MASK) == 0

  def isNegFixInt(b: Byte): Boolean =
    (b & NEGFIXINT_PREFIX) == NEGFIXINT_PREFIX

  def isFixStr(b: Byte): Boolean =
    (b & 0xe0.toByte) == Code.FIXSTR_PREFIX

  def isFixedArray(b: Byte): Boolean =
    (b & 0xf0.toByte) == Code.FIXARRAY_PREFIX

  def isFixedMap(b: Byte): Boolean =
    (b & 0xf0.toByte) == Code.FIXMAP_PREFIX

  def isFixedRaw(b: Byte): Boolean =
    (b & 0xe0.toByte) == Code.FIXSTR_PREFIX

  val POSFIXINT_MASK: Byte   = 0x80.toByte
  val FIXMAP_PREFIX: Byte    = 0x80.toByte
  val FIXARRAY_PREFIX: Byte  = 0x90.toByte
  val FIXSTR_PREFIX: Byte    = 0xa0.toByte
  val NIL: Byte              = 0xc0.toByte
  val NEVER_USED: Byte       = 0xc1.toByte
  val FALSE: Byte            = 0xc2.toByte
  val TRUE: Byte             = 0xc3.toByte
  val BIN8: Byte             = 0xc4.toByte
  val BIN16: Byte            = 0xc5.toByte
  val BIN32: Byte            = 0xc6.toByte
  val EXT8: Byte             = 0xc7.toByte
  val EXT16: Byte            = 0xc8.toByte
  val EXT32: Byte            = 0xc9.toByte
  val FLOAT32: Byte          = 0xca.toByte
  val FLOAT64: Byte          = 0xcb.toByte
  val UINT8: Byte            = 0xcc.toByte
  val UINT16: Byte           = 0xcd.toByte
  val UINT32: Byte           = 0xce.toByte
  val UINT64: Byte           = 0xcf.toByte
  val INT8: Byte             = 0xd0.toByte
  val INT16: Byte            = 0xd1.toByte
  val INT32: Byte            = 0xd2.toByte
  val INT64: Byte            = 0xd3.toByte
  val FIXEXT1: Byte          = 0xd4.toByte
  val FIXEXT2: Byte          = 0xd5.toByte
  val FIXEXT4: Byte          = 0xd6.toByte
  val FIXEXT8: Byte          = 0xd7.toByte
  val FIXEXT16: Byte         = 0xd8.toByte
  val STR8: Byte             = 0xd9.toByte
  val STR16: Byte            = 0xda.toByte
  val STR32: Byte            = 0xdb.toByte
  val ARRAY16: Byte          = 0xdc.toByte
  val ARRAY32: Byte          = 0xdd.toByte
  val MAP16: Byte            = 0xde.toByte
  val MAP32: Byte            = 0xdf.toByte
  val NEGFIXINT_PREFIX: Byte = 0xe0.toByte

  private[spi] val formatTable = new Array[MessageFormat](256)

  {
    // Preparing a look up table for converting byte values into MessageFormat types
    for (b <- 0 to 0xFF) {
      val mf = toMessageFormat(b.toByte)
      formatTable(b) = mf
    }
  }

  /**
    * Returns a MessageFormat type of the specified byte value
    *
    * @param code MessageFormat of the given byte
    * @return
    */
  def messageFormatOf(code: Byte): MessageFormat = formatTable(code & 0xFF)

  /**
    * Converting a byte value into MessageFormat. For faster performance, use {@link #valueOf}
    *
    * @param b MessageFormat of the given byte
    * @return
    */
  private[spi] def toMessageFormat(b: Byte): MessageFormat = {
    if (Code.isPosFixInt(b))
      MessageFormat.POSFIXINT
    else if (Code.isNegFixInt(b))
      MessageFormat.NEGFIXINT
    else if (Code.isFixStr(b))
      MessageFormat.FIXSTR
    else if (Code.isFixedArray(b))
      MessageFormat.FIXARRAY
    else if (Code.isFixedMap(b))
      MessageFormat.FIXMAP
    else {
      b match {
        case Code.NIL =>
          MessageFormat.NIL
        case Code.FALSE =>
          MessageFormat.BOOLEAN
        case Code.TRUE =>
          MessageFormat.BOOLEAN
        case Code.BIN8 =>
          MessageFormat.BIN8
        case Code.BIN16 =>
          MessageFormat.BIN16
        case Code.BIN32 =>
          MessageFormat.BIN32
        case Code.EXT8 =>
          MessageFormat.EXT8
        case Code.EXT16 =>
          MessageFormat.EXT16
        case Code.EXT32 =>
          MessageFormat.EXT32
        case Code.FLOAT32 =>
          MessageFormat.FLOAT32
        case Code.FLOAT64 =>
          MessageFormat.FLOAT64
        case Code.UINT8 =>
          MessageFormat.UINT8
        case Code.UINT16 =>
          MessageFormat.UINT16
        case Code.UINT32 =>
          MessageFormat.UINT32
        case Code.UINT64 =>
          MessageFormat.UINT64
        case Code.INT8 =>
          MessageFormat.INT8
        case Code.INT16 =>
          MessageFormat.INT16
        case Code.INT32 =>
          MessageFormat.INT32
        case Code.INT64 =>
          MessageFormat.INT64
        case Code.FIXEXT1 =>
          MessageFormat.FIXEXT1
        case Code.FIXEXT2 =>
          MessageFormat.FIXEXT2
        case Code.FIXEXT4 =>
          MessageFormat.FIXEXT4
        case Code.FIXEXT8 =>
          MessageFormat.FIXEXT8
        case Code.FIXEXT16 =>
          MessageFormat.FIXEXT16
        case Code.STR8 =>
          MessageFormat.STR8
        case Code.STR16 =>
          MessageFormat.STR16
        case Code.STR32 =>
          MessageFormat.STR32
        case Code.ARRAY16 =>
          MessageFormat.ARRAY16
        case Code.ARRAY32 =>
          MessageFormat.ARRAY32
        case Code.MAP16 =>
          MessageFormat.MAP16
        case Code.MAP32 =>
          MessageFormat.MAP32
        case _ =>
          MessageFormat.NEVER_USED
      }
    }
  }
}
