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
package wvlet.airframe.legacy

import wvlet.airspec.AirSpec

object DefaultValueTest {
  // This type of default values often used in configuration classes
  case class A(a: Long = 10, b: Long = 100, c: Long = 1000)

  case class B(a: A)
}

/**
  */
class DefaultValueTest extends AirSpec {
  import DefaultValueTest.*
  test("populate default values") { (b: B) =>
    b.a shouldBe A()
  }
}
