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

private[json] class StringBuilderExt {

  private val sb = new StringBuilder

  def reset(): Unit = {
    sb.clear()
  }

  def result(): String = {
    sb.result()
  }

  def getAndReset(): String = {
    val result = sb.toString()
    reset()
    result
  }

  def removeLast(): Unit = {
    sb.setLength(sb.length() - 1)
  }

  def last(): Char = {
    sb.last
  }

  def append(b: Byte): Unit = {
    sb.append(b.toChar)
  }

  def append(c: Char): Unit = {
    sb.append(c)
  }

  def append(s: String): Unit = {
    sb.append(s)
  }
}
