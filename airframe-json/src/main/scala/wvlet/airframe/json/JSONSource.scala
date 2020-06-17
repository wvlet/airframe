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
package wvlet.airframe.json

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
  */
object JSONSource {
  def fromString(s: String): JSONSource                 = fromBytes(s.getBytes(StandardCharsets.UTF_8))
  def fromBytes(b: Array[Byte]): JSONSource             = fromBytes(b, 0, b.length)
  def fromBytes(b: Array[Byte], offset: Int, size: Int) = new JSONSource(b, offset, size)
  def fromByteBuffer(b: ByteBuffer) = {
    val a       = new Array[Byte](b.remaining())
    val current = b.position()
    b.get(a, 0, a.length)
    b.position(current)
    fromBytes(a)
  }
}

final class JSONSource(private[this] val b: Array[Byte], private[this] val offset: Int, private[this] val size: Int) {
  assert(offset >= 0, s"The offset must be >= 0: ${offset}")
  assert(size >= 0, s"The size must be >= 0: ${size}")
  assert(offset + size <= b.length, s"The offset + size must be <= ${b.length}: ${offset}+${size}")
  def length: Int             = size
  def apply(index: Int): Byte = b(index + offset)
  def substring(start: Int, end: Int): String = {
    new String(b, offset + start, end - start, StandardCharsets.UTF_8)
  }
}

//class ByteArrayJSONSource(b: Array[Byte], offset: Int, val size: Int) extends JSONSource {
//  assert(offset >= 0, s"The offset must be >= 0: ${offset}")
//  assert(size >= 0, s"The size must be >= 0: ${size}")
//  assert(offset + size <= b.length, s"The offset + size must be <= ${b.length}: ${offset}+${size}")
//
//  def apply(index: Int): Byte = {
//    b(index + offset)
//  }
//  override def substring(start: Int, end: Int): String =
//    new String(b, offset + start, end - start, StandardCharsets.UTF_8)
//}

//class ByteBufferJSONSource(b: ByteBuffer) extends JSONSource {
//  private val offset = b.position()
//  val size           = b.limit() - offset
//
//  def apply(index: Int): Byte = {
//    b.get(index + offset)
//  }
//  override def substring(start: Int, end: Int): String = {
//    val s          = new Array[Byte](end - start)
//    val currentPos = b.position()
//    b.get(s, 0, s.length)
//    b.position(currentPos)
//    new String(s, 0, s.length, StandardCharsets.UTF_8)
//  }
//
//}
