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
class StreamUnpacker(in: MessageSource) extends Unpacker with AutoCloseable {
  import MessageException._

  private var totalReadBytes: Long = 0L

  private var currentBuffer: ReadBuffer = null
  private var cursor: Int               = 0
  private var cursorStack: List[Int]    = List.empty

  private val decoder: Decoder = new Decoder

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
      throw new InsufficientBufferException(cursor, 1);
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
    decoder.unpackNil(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
  }

  /**
    * Peeks a Nil byte and reads it if next byte is a nil value.
    *
    * The difference from [[unpackNil]] is that unpackNil throws an exception if the next byte is not nil value
    * while this tryUnpackNil method returns false without changing position.
    *
    */
  override def tryUnpackNil: Boolean = {
    ensureBuffer
    val b = decoder.tryUnpackNil(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    b
  }

  override def unpackBoolean: Boolean = {
    ensureBuffer

    // TODO handle InsufficientBufferException
    val b = decoder.unpackBoolean(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    b
  }

  override def unpackByte: Byte = {
    ensureBuffer
    val b = decoder.unpackByte(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    b
  }

  override def unpackShort: Short = {
    ensureBuffer
    val s = decoder.unpackShort(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    s
  }

  override def unpackInt: Int = {
    ensureBuffer
    val i = decoder.unpackInt(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    i
  }

  override def unpackLong: Long = {
    ensureBuffer
    val l = decoder.unpackLong(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    l
  }

  override def unpackBigInteger: BigInteger = {
    ensureBuffer
    val b = decoder.unpackBigInteger(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    b
  }

  override def unpackFloat: Float = {
    ensureBuffer
    val f = decoder.unpackFloat(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    f
  }

  override def unpackDouble: Double = {
    ensureBuffer
    val d = decoder.unpackDouble(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    d
  }

  override def unpackString: String = {
    ensureBuffer
    val s = decoder.unpackString(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    s
  }

  override def unpackTimestamp: Instant = {
    ensureBuffer
    val t = decoder.unpackTimestamp(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    t
  }

  override def unpackArrayHeader: Int = {
    ensureBuffer
    val arrayLen = decoder.unpackArrayHeader(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    arrayLen
  }

  override def unpackMapHeader: Int = {
    ensureBuffer
    val mapLen = decoder.unpackMapHeader(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    mapLen
  }

  override def unpackExtTypeHeader: ExtTypeHeader = {
    ensureBuffer
    val extType = decoder.unpackExtTypeHeader(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    extType
  }

  override def unpackRawStringHeader: Int = {
    ensureBuffer
    val h = decoder.unpackRawStringHeader(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    h
  }

  override def unpackBinaryHeader: Int = {
    ensureBuffer
    val h = decoder.unpackBinaryHeader(currentBuffer, cursor)
    cursor += decoder.lastReadByteLength
    h
  }

  override def unpackValue: Value = {}

  override def skipPayload(numBytes: Int): Unit = {
    var skippedLen = 0
    while (skippedLen < numBytes) {
      val remaining = numBytes - skippedLen
      ensureBuffer
      val available = if (cursor + remaining < currentBuffer.size) remaining else currentBuffer.size - cursor
      cursor += available
      skippedLen += available
    }
  }

  override def readPayload(dst: Array[Byte]): Unit = readPayload(dst, 0, dst.length)

  override def readPayload(dst: Array[Byte], offset: Int, length: Int): Unit = {
    ensureBuffer
    var readLen = 0
    while (readLen < length) {
      val remaining = dst.length - readLen
      val available = if (cursor + remaining < currentBuffer.size) remaining else currentBuffer.size - cursor
      currentBuffer.readBytes(cursor, available, dst, offset + readLen)
      readLen += available
    }
  }

  override def readPayload(length: Int): Array[Byte] = {
    val dst = new Array[Byte](length)
    readPayload(dst)
    dst
  }

  override def close(): Unit = in.close

}
