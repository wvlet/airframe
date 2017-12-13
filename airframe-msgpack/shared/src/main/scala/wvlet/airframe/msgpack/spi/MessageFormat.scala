/*
 * Licensed under the Apache License, Version 2.0  extends MessageFormatthe "License");
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

sealed abstract class MessageFormat(val valueType: ValueType)

/**
  * Describes the list of the message format types defined in the MessagePack specification.
  */
object MessageFormat {
  // INT7
  case object POSFIXINT extends MessageFormat(ValueType.INTEGER)
  // MAP4
  case object FIXMAP extends MessageFormat(ValueType.MAP)
  // ARRAY4
  case object FIXARRAY extends MessageFormat(ValueType.ARRAY)
  // STR5
  case object FIXSTR     extends MessageFormat(ValueType.STRING)
  case object NIL        extends MessageFormat(ValueType.NIL)
  case object NEVER_USED extends MessageFormat(null)
  case object BOOLEAN    extends MessageFormat(ValueType.BOOLEAN)
  case object BIN8       extends MessageFormat(ValueType.BINARY)
  case object BIN16      extends MessageFormat(ValueType.BINARY)
  case object BIN32      extends MessageFormat(ValueType.BINARY)
  case object EXT8       extends MessageFormat(ValueType.EXTENSION)
  case object EXT16      extends MessageFormat(ValueType.EXTENSION)
  case object EXT32      extends MessageFormat(ValueType.EXTENSION)
  case object FLOAT32    extends MessageFormat(ValueType.FLOAT)
  case object FLOAT64    extends MessageFormat(ValueType.FLOAT)
  case object UINT8      extends MessageFormat(ValueType.INTEGER)
  case object UINT16     extends MessageFormat(ValueType.INTEGER)
  case object UINT32     extends MessageFormat(ValueType.INTEGER)
  case object UINT64     extends MessageFormat(ValueType.INTEGER)

  case object INT8      extends MessageFormat(ValueType.INTEGER)
  case object INT16     extends MessageFormat(ValueType.INTEGER)
  case object INT32     extends MessageFormat(ValueType.INTEGER)
  case object INT64     extends MessageFormat(ValueType.INTEGER)
  case object FIXEXT1   extends MessageFormat(ValueType.EXTENSION)
  case object FIXEXT2   extends MessageFormat(ValueType.EXTENSION)
  case object FIXEXT4   extends MessageFormat(ValueType.EXTENSION)
  case object FIXEXT8   extends MessageFormat(ValueType.EXTENSION)
  case object FIXEXT16  extends MessageFormat(ValueType.EXTENSION)
  case object STR8      extends MessageFormat(ValueType.STRING)
  case object STR16     extends MessageFormat(ValueType.STRING)
  case object STR32     extends MessageFormat(ValueType.STRING)
  case object ARRAY16   extends MessageFormat(ValueType.ARRAY)
  case object ARRAY32   extends MessageFormat(ValueType.ARRAY)
  case object MAP16     extends MessageFormat(ValueType.MAP)
  case object MAP32     extends MessageFormat(ValueType.MAP)
  case object NEGFIXINT extends MessageFormat(ValueType.INTEGER)

  private val formatTable = new Array[MessageFormat](256)

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
  def of(code: Byte): MessageFormat = formatTable(code & 0xFF)

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
