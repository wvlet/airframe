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
import wvlet.airspec.spi.AirSpecException
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

/**
  */
class TestSpec extends AirSpec with LogSupport {
  scalaJsSupport

  protected override def design: Design = {
    Design.newDesign
      .bind[String].toInstance("my message")
  }

  def helloAirSpec(m: String): Unit = {
    trace(m)
    assert(m == "my message")
  }

  def `support free-style test description`: Unit = {
    trace("hello")
  }

  def `should support assertion`: Unit = {
    intercept[AirSpecException] {
      assert(false, "failure")
    }
  }

  def `should support skipping`: Unit = {
    skip("unnecessary test")
  }

  def `should support cancel`: Unit = {
    cancel("failed to access a necessary service")
  }

  def `should support pending`: Unit = {
    pendingUntil("fixing #012")
  }
  def `should support pending with a reason`: Unit = {
    pending("pending reason")
  }

  def interceptTest: Unit = {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}

object TestObjSpec extends AirSpec with LogSupport {
  scalaJsSupport

  def supportTestsInObjectMethods: String = {
    trace("hello companion methods")
    "hello obj"
  }
}

class WordSpecTest extends AirSpec {
  scalaJsSupport

  def `should have a natural language description` = {}
  def `should support arbitrary texts`: Unit = {}
}

class JvmSpecTest extends AirSpec {
  def `this method will not be called in Scala.js as scalaJsSupport is not called` = {}
}
