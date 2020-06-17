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
package wvlet.airframe.canvas
import java.nio.ByteBuffer

/**
  * Canvas implementation with Unsafe memory access.
  * This provides native C-code level access performance.
  */
final class UnsafeCanvas(
    // Base object for resolving the relative address of the raw byte array.
    // If base == null, the address value (the second parameter) will be a raw memory address
    private[canvas] val base: AnyRef,
    // The head address of the underlying memory. If the base is null, this address is a direct memory address.
    // If not, this will be the relative address within an array object (base).
    private[canvas] val address: Long,
    // The size of the underlying memory.
    val size: Long,
    // Reference is used to hold a reference to an object that holds the underlying memory
    // so that it cannot be released by GC.
    private[canvas] val reference: AnyRef
) extends Canvas {
  import UnsafeUtil._

  override def readByte(offset: Long): Byte = {
    unsafe.getByte(base, address + offset)
  }
  override def readBoolean(offset: Long): Boolean = {
    unsafe.getBoolean(base, address + offset)
  }
  override def readShort(offset: Long): Short = {
    unsafe.getShort(base, address + offset)
  }
  override def readInt(offset: Long): Int = {
    // The data will be read in the platform native endian.
    unsafe.getInt(base, address + offset)
  }
  override def readIntBigEndian(offset: Long): Int = {
    Integer.reverseBytes(readInt(offset))
  }
  override def readLong(offset: Long): Long = {
    // The data will be read in the platform native endian.
    unsafe.getLong(base, address + offset)
  }
  override def readLongBigEndian(offset: Long): Long = {
    java.lang.Long.reverseBytes(readLong(offset))
  }

  override def readFloat(offset: Long): Float = {
    unsafe.getFloat(base, address + offset)
  }
  override def readDouble(offset: Long): Double = {
    unsafe.getDouble(base, address + offset)
  }
  override def readBytes(offset: Long, dest: Canvas, destOffset: Long, length: Long): Unit = {
    dest match {
      case u: UnsafeCanvas =>
        unsafe.copyMemory(base, address + offset, u.base, u.address + destOffset, length)
      case other =>
        throw new UnsupportedOperationException(s"readBytes from ${other.getClass}")
    }
  }
  override def readBytes(offset: Long, dest: Array[Byte], destOffset: Int, length: Int): Unit = {
    unsafe.copyMemory(base, address + offset, dest, arrayByteBaseOffset + destOffset, length)
  }
  override def writeByte(offset: Long, v: Byte): Unit = {
    unsafe.putByte(base, address + offset, v)
  }
  override def writeBoolean(offset: Long, v: Boolean): Unit = {
    unsafe.putBoolean(base, address + offset, v)
  }
  override def writeShort(offset: Long, v: Short): Unit = {
    unsafe.putShort(base, address + offset, v)
  }
  override def writeInt(offset: Long, v: Int): Unit = {
    unsafe.putInt(base, address + offset, v)
  }
  override def writeIntBigEndian(offset: Long, v: Int): Unit = {
    unsafe.putInt(base, address + offset, Integer.reverseBytes(v))
  }
  override def writeLong(offset: Long, v: Long): Unit = {
    unsafe.putLong(base, address + offset, v)
  }
  override def writeLongBigEndian(offset: Long, v: Long): Unit = {
    unsafe.putLong(base, address + offset, java.lang.Long.reverseBytes(v))
  }
  override def writeFloat(offset: Long, v: Float): Unit = {
    unsafe.putFloat(base, address + offset, v)
  }
  override def writeDouble(offset: Long, v: Double): Unit = {
    unsafe.putDouble(base, address + offset, v)
  }
  override def writeBytes(offset: Long, src: Array[Byte], srcOffset: Int, length: Int): Unit = {
    unsafe.copyMemory(src, arrayByteBaseOffset + srcOffset, base, address + offset, length)
  }
  override def writeBytes(offset: Long, src: Canvas, srcOffset: Long, length: Long): Unit = {
    src match {
      case u: UnsafeCanvas =>
        unsafe.copyMemory(u.base, u.address + srcOffset, base, address + offset, length)
      case other =>
        throw new UnsupportedOperationException(s"writeBytes to ${other.getClass}")
    }
  }

  override def slice(offset: Long, length: Long): Canvas = {
    if (offset == 0 && length == size) {
      this
    } else {
      if (offset + length > size) {
        throw new IllegalArgumentException(s"${length} is longer than the Canvas size ${size}")
      }
      new UnsafeCanvas(base, address + offset, length, reference);
    }
  }
  override def toByteArray: Array[Byte] = {
    if (!size.isValidInt) {
      throw new IllegalArgumentException(s"Canvas size ${size} exceeds Array[Byte] size limit")
    }
    val b = new Array[Byte](size.toInt)
    unsafe.copyMemory(base, address, b, arrayByteBaseOffset, size)
    b
  }

  override def release: Unit = {
    if (this.base == null) {
      this.reference match {
        case m: Memory =>
          m.release
        case d: ByteBuffer if d.isDirect =>
          DirectBufferAccess.clean(d)
        case _ =>
        // No need to release
      }
    }
  }
}

object UnsafeCanvas {
  def wrap(arr: Array[Byte], offset: Int, length: Int): Canvas = {
    new UnsafeCanvas(arr, UnsafeUtil.arrayByteBaseOffset + offset, length, null)
  }
  def wrap(buf: ByteBuffer): Canvas = {
    if (buf.isDirect) {
      new UnsafeCanvas(
        base = null,
        address = DirectBufferAccess.getAddress(buf) + buf.position(),
        size = buf.remaining(),
        reference = buf
      )
    } else if (buf.hasArray) {
      new UnsafeCanvas(
        base = buf.array(),
        address = UnsafeUtil.arrayByteBaseOffset + buf.arrayOffset() + buf.position(),
        size = buf.remaining(),
        reference = null
      )
    } else {
      throw new IllegalArgumentException(
        s"Canvas supports only array-backed ByteBuffer or DirectBuffer: The input buffer is ${buf.getClass}"
      )
    }
  }
}
