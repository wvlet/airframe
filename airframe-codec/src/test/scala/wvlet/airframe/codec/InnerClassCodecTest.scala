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
package wvlet.airframe.codec

import wvlet.airspec.AirSpec

/**
  */
class InnerClassCodecTest extends AirSpec {
  scalaJsSupport

  case class A(id: Int, name: String)

  def `support codec for inner classes`: Unit = {
    val codec   = MessageCodec.of[A]
    val a       = A(1, "leo")
    val msgpack = codec.toMsgPack(a)
    val a1      = codec.unpackMsgPack(msgpack)
    a1 shouldBe Some(a)
  }
}
