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
package wvlet.airframe.examples.airspec

import wvlet.airspec.spi.AirSpecContext
import wvlet.airspec.AirSpec

/**
  */
class AirSpec_02_ReuseTests extends AirSpec {
  // A template for reusable test cases

  private def fixture[A](data: Seq[A]): Unit = {
    info(s"Run tests for ${data}")

    test("emptyTest") {
      data shouldNotBe empty
    }
    test("sizeTest") {
      data.length shouldBe data.size
    }
  }

  test("reuse tests") {
    fixture(Seq(1, 2)))
    fixture(Seq("A", "B", "C")))
  }
}
