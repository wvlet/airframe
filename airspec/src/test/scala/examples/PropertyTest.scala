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
package examples

import wvlet.airspec.spi.PropertyCheck
import wvlet.airspec.AirSpec

/**
  */
class PropertyTest extends AirSpec with PropertyCheck {

  test("forAll(Int)") {
    forAll { (i: Int) => i.isValidInt shouldBe true }
  }

  test("forAll(Int, String)") {
    forAll { (i: Int, s: String) =>
      i.isValidInt shouldBe true
      s != null shouldBe true
    }
  }

  test("forAll(Int, Float, String)") {
    forAll { (i: Int, f: Float, s: String) =>
      i.isValidInt shouldBe true
      s != null shouldBe true
    }
  }
}
