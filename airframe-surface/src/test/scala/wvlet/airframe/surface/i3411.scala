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

  trait Base {
    class InnerType {
      def compare(that: InnerType): Int = 0
    }
  }

  object OuterType extends Base {
    def create: InnerType = new InnerType
  }

  test("Handle inherited inner class") {
    //val mm = Surface.methodsOf[OuterType.type]
    val m = Surface.methodsOf[OuterType.InnerType]
    //m.map(_.name) shouldContain "compare"
  }

  object SomeEnum extends Enumeration {
    type SomeEnum = Value

    val A, B, C = Value
  }

  import SomeEnum.SomeEnum

  test("Handle a Scala 2 enumeration") {
    //val s = Surface.of[SomeEnum] // just check there is no error - no expected properties
    //val m = Surface.methodsOf[SomeEnum]
    //debug(s.params)
    // enumeration type (value) usually contains at least the compare method
    //m.map(_.name) shouldContain "compare"
  }
}
