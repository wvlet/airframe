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
package dotty.test

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.log.LogSupport

/**
  */
object CodecTest extends LogSupport {

  case class A(id: Int, name: String)

  def run: Unit = {
    {
      val codec = MessageCodec.of[A]
      val json  = codec.toJson(A(1, "leo"))
      debug(json)
      val a = MessageCodec.fromJson[A](json)
      debug(a)
      val json2 = MessageCodec.toJson(a)
      debug(json2)
    }

    {
      val f     = MessageCodecFactory.defaultFactory
      val codec = f.of[A]
      val json  = f.toJson(A(1, "leo"))
      debug(json)
      val a = f.fromJson[A](json)
      debug(a)
    }
  }
}
