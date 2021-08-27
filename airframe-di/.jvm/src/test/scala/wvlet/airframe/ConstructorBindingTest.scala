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
package wvlet.airframe

import wvlet.airspec.AirSpec

/**
  * TODO: This works only in JVM, because we cannot extract constructor default parameters in Scala.js
  */
class ConstructorBindingTest extends AirSpec {
  import ConstructorBindingTest._

  test("build objects using default constructor parameters") {
    newSilentDesign.build[CbTest] { a =>
      debug(a)
      a shouldBe CbTest(-1, "leo")
    }
  }

  test("build objects using default param and binding") {
    newSilentDesign
      .bind[MyValue].toInstance("hello")
      .build[CbTest1] { a =>
        debug(a)
        a shouldBe CbTest1(-1, "hello")
      }
  }
}

object ConstructorBindingTest {
  type MyValue = String
  case class CbTest(id: Int = -1, name: String = "leo")
  case class CbTest1(id: Int = -1, value: MyValue = "ppp")
}
