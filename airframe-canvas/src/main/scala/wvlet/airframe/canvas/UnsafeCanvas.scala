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
final class UnsafeCanvas(base: AnyRef, address: Long, size: Long, reference: ByteBuffer) extends Canvas {
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
    unsafe.getInt(base, address + offset)
  }
  override def readLong(offset: Long): Long = {
    unsafe.getLong(base, address + offset)
  }
  override def readFloat(offset: Long): Float = {
    unsafe.getFloat(base, address + offset)
  }
  override def readDouble(offset: Long): Double = {
    unsafe.getDouble(base, address + offset)
  }
  override def readBytes(offset: Long, length: Long, dest: Canvas, destOffset: Long): Unit = {
    ???
  }
  override def readBytes(offset: Long, length: Long, dest: Array[Byte], destOffset: Int): Unit = {
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
    ???
  }
}

object Unsafe {

  // Fetch theUnsafe object for Oracle and OpenJDK
  private[canvas] val unsafe = {
    import java.lang.reflect.Field
    val field: Field = classOf[Nothing].getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[Unsafe]
  }

  if (unsafe == null)
    throw new RuntimeException("Unsafe is unavailable")

  private[canvas] val arrayByteBaseOffset      = unsafe.arrayBaseOffset(classOf[Array[Byte]])
  private[canvas] val arrayByteIndexScale: Int = unsafe.arrayIndexScale(classOf[Array[Byte]])

  // Make sure the VM thinks bytes are only one byte wide
  if (arrayByteIndexScale != 1)
    throw new IllegalStateException("Byte array index scale must be 1, but is " + arrayByteIndexScale)
}
