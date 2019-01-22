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

import sun.misc.Unsafe

/**
  * Canvas implementation with Unsafe memory access.
  * This provides native C-code level access performance.
  *
  */
final class UnsafeCanvas(
                         // Base ofject for resolving the relative address of the raw byte array.
                         // If base == null, the address value (the second parameter) will be a raw memory address
                         private[canvas] val base: AnyRef,
                         // The head address of the underlying memory. If the base is null, this address is a direct memory address.
                         // If not, this will be the relative address within an array object (base).
                         private[cavnas] val address: Long,
                         // The size of the underlying memory.
                         private[canvas] val size: Long,
                         // Reference is used to hold a reference to an object that holds the underlying memory
                         // so that it cannot be released by GC.
                         private[canvas] val reference: ByteBuffer)
    extends Canvas {
  import Unsafe._

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
  override def readLong(offset: Long): Long = {
    // The data will be read in the platform native endian.
    unsafe.getLong(base, address + offset)
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
  override def writeLong(offset: Long, v: Long): Unit = {
    unsafe.putLong(base, address + offset, v)
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
}

object UnsafeCanvas {
  def wrap(arr: Array[Byte], offset: Int, length: Int): Canvas = {
    new UnsafeCanvas(arr, Unsafe.arrayByteBaseOffset + offset, length, null)
  }
  def wrap(buf: ByteBuffer): Canvas = {
    if (buf.isDirect) {
      new UnsafeCanvas(base = null,
                       address = DirectBufferAccess.getAddress(buf) + buf.position(),
                       size = buf.remaining(),
                       reference = buf)
    } else if (buf.hasArray) {
      new UnsafeCanvas(base = buf.array(),
                       address = Unsafe.arrayByteBaseOffset + buf.arrayOffset() + buf.position(),
                       size = buf.remaining(),
                       reference = null)
    } else {
      throw new IllegalArgumentException(
        s"Canvas supports only array-backed ByteBuffer or DirectBuffer: The input buffer is ${buf.getClass}")
    }
  }

}
