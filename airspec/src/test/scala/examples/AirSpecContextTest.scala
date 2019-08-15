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

import wvlet.airframe.spec.AirSpec
import wvlet.airframe.spec.spi.AirSpecContext

trait TestFixture extends AirSpec {
  def testA: Unit = {}
  def testB: Unit = {}
}

/**
  *
  */
class AirSpecContextTest extends AirSpec {
  scalaJsSupport

  def `support passing the test context`(context: AirSpecContext): Unit = {
    context.specName shouldBe "AirSpecContextTest"
    context.testName shouldBe "support passing the test context"

    val f = new TestFixture {
      val v = "hello2"
    }

    context.run(f)
    context.run[TestFixture]
  }
}
