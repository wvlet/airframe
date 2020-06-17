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
import sun.misc.Unsafe

/**
  */
private[canvas] object UnsafeUtil {
  // Fetch theUnsafe object for Oracle and OpenJDK
  private[canvas] val unsafe = {
    import java.lang.reflect.Field
    val field: Field = classOf[Unsafe].getDeclaredField("theUnsafe")
    field.setAccessible(true)
    field.get(null).asInstanceOf[Unsafe]
  }

  if (unsafe == null)
    throw new RuntimeException("Unsafe is unavailable")

  private[canvas] val arrayByteBaseOffset: Long = unsafe.arrayBaseOffset(classOf[Array[Byte]])
  private[canvas] val arrayByteIndexScale: Int  = unsafe.arrayIndexScale(classOf[Array[Byte]])

  // Make sure the VM thinks bytes are only one byte wide
  if (arrayByteIndexScale != 1)
    throw new IllegalStateException("Byte array index scale must be 1, but is " + arrayByteIndexScale)
}
