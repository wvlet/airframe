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
package wvlet.airframe.spec

import wvlet.airframe.{Design, _}
import wvlet.log.LogSupport

/**
  *
  */
class TestSpec extends AirSpec with LogSupport {
  override protected def design: Design =
    newDesign
      .bind[String].toInstance("my message")

  def hello(m: String): Unit = {
    trace(m)
    assert(m == "my message")
  }

  def `support free-style test description`: Unit = {
    trace("hello")
  }

  def `should support assertion`: Unit = {
    //assert(false, "failure")
  }

  def `should support skipping`: Unit = {
    //skip("skip test")
  }

  def `should support cancel`: Unit = {
    //cancel("cancelled")
  }

  def `should support pending`: Unit = {
    //pending("pending test")
  }

  def interceptTest: Unit = {
    intercept[NoSuchElementException] {
      Seq.empty.head
    }
  }
}

object TestObjSpec extends AirSpec with LogSupport {
  def supportTestsInObjectMethods: String = {
    trace("hello companion methods")
    "hello obj"
  }
}

object WordSpecTest extends AirSpec {
  def `should have a natural language description` = {}
  def `should support arbitrary texts`: Unit       = {}
}
