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

import wvlet.airframe.Design
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AirSpecContext

/**
  *
  */
class ContextTest extends AirSpec {
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

    def testFixtureName(context: AirSpecContext): Unit = {
      if (isScalaJS) {
        pendingUntil("Getting class names in Scala.js is unstable")
      }
      context.specName.contains("TestFixture") shouldBe true
    }
  }

  def `support passing the test context`(context: AirSpecContext): Unit = {
    context.specName shouldBe "ContextTest"
    context.testName shouldBe "support passing the test context"
  }

  def `support running AirSpec instances`(context: AirSpecContext): Unit = {
    context.specName shouldBe "ContextTest"
    context.testName shouldBe "support running AirSpec instances"

    // This import statement is necessary for Scala.js
    import scala.language.reflectiveCalls

    val f = new TestFixture {
      var callCountC = 0
      def testC: Unit = {
        callCountC += 1
      }

      // TODO: This shows compilation error in Scala.js. It looks like a bug in Surface
      //      def testContext(context: AirSpecContext): Unit = {
      //        context.testName shouldBe "testContext"
      //        context.specName.contains("TestFixture") shouldBe true
      //      }
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
    val f = context.test[TestFixture]

    f.callCountA shouldBe 1
    f.callCountB shouldBe 1

    val f2 = context.run(f)
    f2 shouldBeTheSameInstanceAs f2

    f.callCountA shouldBe 2
    f.callCountB shouldBe 2
  }

  class MySpec extends AirSpec {
    def `check local context`(context: AirSpecContext): Unit = {
      context.parentContext shouldBe defined
      context.testName shouldBe "check local context"
      context.indentLevel shouldBe 1
    }

    def checkClassName(context: AirSpecContext): Unit = {
      if (isScalaJS) {
        pendingUntil("Getting class names in Scala.js is unstable")
      }
      context.specName.contains("MySpec") shouldBe true
    }
  }

  def `support passing a context to spec instances`(context: AirSpecContext): Unit = {
    context.indentLevel shouldBe 0
    context.test[MySpec]
    context.run(new MySpec)
  }
}

class ContextWithDI extends AirSpec {
  scalaJsSupport

  trait SpecWithDI extends AirSpec {
    import wvlet.airframe._
    private val port = bind[Int]

    def `check binding`: Unit = {
      port shouldBe 1000
    }
  }

  protected override def design: Design = {
    Design.newDesign
      .bind[Int].toInstance(1000)
  }

  def `delegate bindings from the global session`(context: AirSpecContext): Unit = {
    context.test[SpecWithDI]
  }
}
