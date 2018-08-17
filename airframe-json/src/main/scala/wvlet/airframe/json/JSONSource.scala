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

/**
  *
  */
object JSONSource {
  def fromBytes(b: Array[Byte])                         = new ByteArrayJSONSource(b, 0, b.length)
  def fromBytes(b: Array[Byte], offset: Int, size: Int) = new ByteArrayJSONSource(b, offset, size)
}

trait JSONSource {
  def length: Int = size
  def size: Int
  def apply(index: Int): Byte
  def substring(start: Int, end: Int): String
}

class ByteArrayJSONSource(b: Array[Byte], offset: Int, val size: Int) extends JSONSource {
  assert(offset >= 0, s"The offset must be >= 0: ${offset}")
  assert(size >= 0, s"The size must be >= 0: ${size}")
  assert(offset + size <= b.length, s"The offset + size must be <= ${b.length}: ${offset}+${size}")

  def apply(index: Int): Byte = {
    b(index + offset)
  }
  override def substring(start: Int, end: Int): String = new String(b, offset + start, end - start)
}
