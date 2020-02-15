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

import wvlet.airspec.AirSpec

/**
  *
  */
class DISupportTest extends AirSpec {
  scalaJsSupport

  class A(val session: Session) extends DISupport {
    private val s = bind[String]

    def getString = s
  }

  def `support in-class bind`: Unit = {
    val d = newSilentDesign
      .bind[String].toInstance("hello")

    d.build[A] { a => a.getString shouldBe "hello" }
  }
}
