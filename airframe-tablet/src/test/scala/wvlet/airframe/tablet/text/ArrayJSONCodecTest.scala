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
package wvlet.airframe.tablet.text

import wvlet.airframe.codec.MessageHolder
import wvlet.airspec.AirSpec

class ArrayJSONCodecTest extends AirSpec {
  private def assertEncode[T](codec: ArrayJSONCodec[T], input: Array[T]) = {
    val json = codec.toJSON(input)
    debug(s"encoded: $json")
    val v = new MessageHolder
    codec.fromJSON(json, v)

    v.isNull shouldBe false
    v.getLastValue shouldBe input
  }

  def `encode short array`: Unit = {
    val codec = ArrayJSONCodec.of[Short]
    val a     = Array[Short](1, 2)
    assertEncode(codec, a)
  }

  def `encode int array`: Unit = {
    val codec = ArrayJSONCodec.of[Int]
    val a     = Array(1, 2)
    assertEncode(codec, a)
  }

  def `encode long array`: Unit = {
    val codec = ArrayJSONCodec.of[Long]
    val a     = Array(1L, 2L)
    assertEncode(codec, a)
  }

  def `encode char array`: Unit = {
    val codec = ArrayJSONCodec.of[Char]
    val a     = Array('a', 'b')
    assertEncode(codec, a)
  }

  def `encode float array`: Unit = {
    val codec = ArrayJSONCodec.of[Float]
    val a     = Array(1.5f, 2.5f)
    assertEncode(codec, a)
  }

  def `encode double array`: Unit = {
    val codec = ArrayJSONCodec.of[Double]
    val a     = Array(1.5d, 2.5d)
    assertEncode(codec, a)
  }

  def `encode boolean array`: Unit = {
    val codec = ArrayJSONCodec.of[Boolean]
    val a     = Array(true, false)
    assertEncode(codec, a)
  }

  def `encode string array`: Unit = {
    val codec = ArrayJSONCodec.of[String]
    val a     = Array("a", "b")
    assertEncode(codec, a)
  }
}
