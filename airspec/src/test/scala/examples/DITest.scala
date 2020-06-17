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

import wvlet.airframe.{Design, newDesign}
import wvlet.airspec.AirSpec

/**
  */
class DITest extends AirSpec {
  scalaJsSupport

  import wvlet.airframe._

  protected val v: Int = 1000

  protected override def design: Design      = newDesign.bind[String].toInstance("hello airframe")
  protected override def localDesign: Design = newDesign.bind[Int].toInstance(v)

  def globalDesignTest(msg: String): Unit = {
    msg shouldBe "hello airframe"
  }

  def localDesignTest(intValue: Int): Unit = {
    intValue shouldBe v
  }
}

class DIExtTest extends DITest {
  override protected val v: Int = 2000
}
