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
package wvlet.airframe.msgpack.io

import java.math.BigInteger
import java.time.Instant

import wvlet.airframe.msgpack.spi._

import scala.annotation.tailrec

/**
  *
  */
class StreamUnpacker(in: BufferInput) extends Unpacker with AutoCloseable {
  import BufferUnpacker._

  private var totalReadBytes: Long = 0L

  private var currentBuffer: Buffer  = null
  private var cursor: Int            = 0
  private var cursorStack: List[Int] = List.empty

  def getTotalReadBytes: Long = totalReadBytes + cursor

  @tailrec
  private def ensureBuffer: Boolean = {
    if (cursor < currentBuffer.size) {
      true
    } else {
      in.next match {
        case None =>
          false
        case Some(nextBuffer) =>
          totalReadBytes += currentBuffer.size
          currentBuffer = nextBuffer
          // TODO: cursorStack handling
          cursor = 0
          // Continue until we can get non-empty buffer
          ensureBuffer
      }
    }
  }

  override def hasNext: Boolean = ensureBuffer

  override def getNextFormat: MessageFormat = {
    if (!ensureBuffer) {
      throw new InsufficientBufferException(1);
    }
    val b = currentBuffer.readByte(cursor)
    MessageFormat.of(b)
  }

  override def getNextValueType: ValueType = {
    getNextFormat.valueType
  }

  override def skipValue: Unit = skipValue(1)
  override def skipValue(count: Int): Unit = {
    while (count > 0) {
      // TODO
    }
  }
  override def unpackNil: Unit = {
    ensureBuffer
    currentBuffer.readByte(cursor) match {
      case Code.NIL => // OK
        cursor += 1
      case other =>
        throw unexpected("Nil", other)
    }
  }

  /**
    * Peeks a Nil byte and reads it if next byte is a nil value.
    *
    * The difference from [[unpackNil]] is that unpackNil throws an exception if the next byte is not nil value
    * while this tryUnpackNil method returns false without changing position.
    *
    */
  override def tryUnpackNil: Boolean = {
    currentBuffer.readByte(cursor) match {
      case Code.NIL => // OK
        cursor += 1
        true
      case other =>
        false
    }
  }

  override def unpackBoolean: Boolean = {
    currentBuffer.readBytes(cursor) match {

    }
  }

  override def unpackByte: Byte                                              = ???
  override def unpackShort: Short                                            = ???
  override def unpackInt: Int                                                = ???
  override def unpackLong: Long                                              = ???
  override def unpackBigInteger: BigInteger                                  = ???
  override def unpackFloat: Float                                            = ???
  override def unpackDouble: Double                                          = ???
  override def unpackString: String                                          = ???
  override def unpackTimestamp: Instant                                      = ???
  override def unpackArrayHeader: Int                                        = ???
  override def unpackMapHeader: Int                                          = ???
  override def unpackExtensionTypeHeader: ExtensionTypeHeader                = ???
  override def unpackRawStringHeader: Int                                    = ???
  override def unpackBinaryHeader: Int                                       = ???
  override def unpackValue: Value                                            = ???
  override def skipPayload(numBytes: Int): Unit                              = ???
  override def readPayload(dst: Array[Byte]): Unit                           = ???
  override def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit = ???
  override def readPayload(length: Int): Array[Byte]                         = ???

  override def close(): Unit = in.close
}
