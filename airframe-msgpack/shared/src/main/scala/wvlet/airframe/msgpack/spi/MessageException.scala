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

import java.math.BigInteger

trait ErrorCode
object ErrorCode {
  // Type conversion errors (data is valid, but reading to the target type failed)
  trait TypeConversionError     extends ErrorCode
  case object INTEGER_OVERFLOW  extends TypeConversionError
  case object INVALID_TYPE_CAST extends TypeConversionError
  case object INVALID_TYPE      extends TypeConversionError

  // Internal errors
  trait InternalError             extends ErrorCode
  case object INSUFFICIENT_BUFFER extends InternalError

  // Format errors (incompatible MessagePack format is used)
  trait InvalidFormatError          extends ErrorCode
  case object NEVER_USED_FORMAT     extends InvalidFormatError
  case object INVALID_STRING_CODING extends InvalidFormatError
  case object TOO_LARGE_MESSAGE     extends InvalidFormatError
  case object INVALID_EXT_FORMAT    extends InvalidFormatError

}

/**
  * Base class for message pack errors
  */
class MessageException(val errorCode: ErrorCode, message: String = null, cause: Throwable = null) extends Exception(message, cause) {
  override def getMessage: String = {
    val s = new StringBuilder
    s.append(s"[${errorCode}]")
    if (message != null) {
      s.append(s" ${message}")
    }
    if (cause != null) {
      s.append(s", cause: ${cause.getMessage}")
    }
    s.result()
  }
}

case class InsufficientBufferException(currentPosition: Int, expectedLength: Int)
    extends MessageException(ErrorCode.INSUFFICIENT_BUFFER, s"Need at least ${expectedLength} more bytes")

/**
  * This error is thrown when the user tries to read an integer value
  * using a smaller types. For example, calling MessageUnpacker.unpackInt() for an integer value
  * that is larger than Integer.MAX_VALUE will cause this exception.
  */
case class IntegerOverflowException(bigInteger: BigInteger) extends MessageException(ErrorCode.INTEGER_OVERFLOW, s"Too large integer: ${bigInteger}")
case class TooLargeMessageException(size: Long)             extends MessageException(ErrorCode.TOO_LARGE_MESSAGE, s"Too large message size: ${size}")

object MessageException {
  def overflowU8(u8: Byte)    = new IntegerOverflowException(BigInteger.valueOf((u8 & 0xFF).toLong))
  def overflowU16(u16: Short) = new IntegerOverflowException(BigInteger.valueOf((u16 & 0xFFFF).toLong))
  def overflowU32(u32: Int)   = new IntegerOverflowException(BigInteger.valueOf((u32 & 0xFFFFFFFF).toLong))
  def overflowU64(u64: Long)  = new IntegerOverflowException(BigInteger.valueOf(u64 + Long.MaxValue + 1L).setBit(63))

  def overflowI16(i16: Short) = new IntegerOverflowException(BigInteger.valueOf(i16.toLong))
  def overflowI32(i32: Int)   = new IntegerOverflowException(BigInteger.valueOf(i32.toLong))
  def overflowI64(i64: Long)  = new IntegerOverflowException(BigInteger.valueOf(i64))

  def overflowU32Size(u32: Int) = new TooLargeMessageException(((u32 & 0x7fffffff) + 0x80000000L).toLong)
}
