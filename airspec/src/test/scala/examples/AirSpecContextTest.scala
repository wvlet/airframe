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

/**
  *
  */
class AirSpecContextTest extends AirSpec {
  scalaJsSupport

  trait TestFixture extends AirSpec {
    var callCountA = 0
    var callCountB = 0

    def testA(context: AirSpecContext): Unit = {
      callCountA += 1
      context.testName shouldBe "testA"
    }
    def testB(context: AirSpecContext): Unit = {
      callCountB += 1
      context.testName shouldBe "testB"
    }
  }

  def `support passing the test context`(context: AirSpecContext): Unit = {
    context.specName shouldBe "AirSpecContextTest"
    context.testName shouldBe "support passing the test context"
  }

  def `support running AirSpec instances`(context: AirSpecContext): Unit = {
    context.specName shouldBe "AirSpecContextTest"
    context.testName shouldBe "support running AirSpec instances"

    // This import statement is necessary for Scala.js
    import scala.language.reflectiveCalls

    val f = new TestFixture {
      var callCountC = 0
      def testC: Unit = {
        callCountC += 1
      }
    }

    f.callCountA shouldBe 0
    f.callCountB shouldBe 0
    f.callCountC shouldBe 0

    val f2 = context.run(f)
    f2 shouldBeTheSameInstanceAs f

    f.callCountA shouldBe 1
    f.callCountB shouldBe 1
    f.callCountC shouldBe 1
  }

  def `support running AirSpec from a type`(context: AirSpecContext): Unit = {
    val f = context.run[TestFixture]

    f.callCountA shouldBe 1
    f.callCountB shouldBe 1

    val f2 = context.run(f)
    f2 shouldBeTheSameInstanceAs f2

    f.callCountA shouldBe 2
    f.callCountB shouldBe 2
  }
}
