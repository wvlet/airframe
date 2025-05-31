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
package wvlet.airframe.surface

import wvlet.airspec.AirSpec

object MT extends MT

trait MT {
  case class B(min: Int = 0, max: Int = 0)
}

class TraitInheritedClassTest extends AirSpec:
  test("Surface of case class defined in trait object should work") {
    val schema = Surface.of[MT.B]
    schema.name shouldBe "B"
    schema.fullName shouldBe "wvlet.airframe.surface.MT.B"
  }