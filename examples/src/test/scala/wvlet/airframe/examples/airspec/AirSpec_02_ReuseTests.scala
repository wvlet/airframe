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
  class Fixture[A](data: Seq[A]) extends AirSpec {
    override protected def beforeAll: Unit = {
      info(s"Run tests for ${data}")
    }
    def emptyTest: Unit = {
      data shouldNotBe empty
    }
    def sizeTest: Unit = {
      data.length shouldBe data.size
    }
  }

  def test(context: AirSpecContext): Unit = {
    context.run(new Fixture(Seq(1, 2)))
    context.run(new Fixture(Seq("A", "B", "C")))
  }
}
