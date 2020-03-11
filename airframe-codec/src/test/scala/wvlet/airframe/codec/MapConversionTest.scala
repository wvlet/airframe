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
import java.util.UUID

import wvlet.airspec.AirSpec

/**
  *
  */
object MapConversionTest extends AirSpec {

  case class A(id: Int, name: String, key: UUID)

  test("convert Map[String, Any] to object") {
    val uuid  = UUID.randomUUID()
    val m     = Map("id" -> 10, "name" -> "leo", "key" -> uuid)
    val codec = MessageCodec.of[A]
    val a     = codec.fromMap(m)
    a shouldBe A(10, "leo", uuid)
  }
}
