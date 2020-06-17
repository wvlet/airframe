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
package wvlet.airframe.surface

import wvlet.airspec.AirSpec

/**
  */
class InnerClassTest extends AirSpec {
  case class A(id: Int, name: String)

  def `pass inner class context to Surface`: Unit = {
    val s = Surface.of[A]
    val a = s.objectFactory.map { x => x.newInstance(Seq(1, "leo")) }
    a shouldBe Some(A(1, "leo"))
  }

  def `throw IllegalStateException when failed to find the outer class instance`: Unit = {
    val e = intercept[IllegalStateException] {
      new {
        val s = Surface.of[A]
        s.objectFactory.map { x => x.newInstance(Seq(1, "leo")) }
      }
    }
    e.getMessage.contains(s"${this.getClass.getSimpleName}") shouldBe true
  }
}
