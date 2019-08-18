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

object ObjectJSONCodecTest {
  case class A(id: Int, name: String)
}

/**
  *
  */
class ObjectJSONCodecTest extends AirSpec {
  import ObjectJSONCodecTest._

  def `convert object into json`: Unit = {
    val codec = ObjectJSONCodec.of[A]
    val a     = A(1, "leo")
    val json  = codec.toJSON(a)
    val v     = new MessageHolder
    codec.fromJSON(json, v)

    v.isNull shouldBe false
    v.getLastValue shouldBe a
  }

  def `forbid creating codecs for non-object`: Unit = {
    intercept[IllegalArgumentException] {
      ObjectJSONCodec.of[String]
    }
  }
}
