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

object i3411 extends AirSpec {

  object SomeEnum extends Enumeration {
    type SomeEnum = Value

    val A, B, C = Value
  }

  import SomeEnum.SomeEnum

  test("Handle a Scala 2 enumeration") {
    val s = Surface.of[SomeEnum] // just check there is no error - no expected properties
    val m = Surface.methodsOf[SomeEnum]
    debug(s.params)
    // enumeration type (value) usually contains at least the compare method
    // note: we are unable to handle compare at the moment and some other methods inherited from Value, see https://github.com/lampepfl/dotty/issues/19825
    // m.map(_.name) shouldContain "compare"
  }
}
