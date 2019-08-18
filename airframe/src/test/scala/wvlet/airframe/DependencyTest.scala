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

import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airspec.AirSpec

object DependencyTest1 {
  trait A {
    val b = bind[B]
  }
  trait B {
    val c = bind[C]
  }
  case class C(d: D)
  trait D
  trait DImpl extends D
}

class DependencyTest extends AirSpec {
  scalaJsSupport

  def `show missing dependencies`: Unit = {
    val d = newSilentDesign
    d.withSession { session =>
      val m = intercept[MISSING_DEPENDENCY] {
        val a = session.build[DependencyTest1.A]
      }
      val msg = m.getMessage
      msg.contains("D <- C") shouldBe true
    }
  }

  def `resolve concrete dependencies`: Unit = {
    val d = newSilentDesign
      .bind[DependencyTest1.D].to[DependencyTest1.DImpl] // abstract class to a concrete trait
    d.withSession { session =>
      val a = session.build[DependencyTest1.A]
    }
  }
}
