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

object ParamTest {
  object A {
    def hello: String       = "hello"
    def apply(s: String): A = A(s.toInt)
  }

  def getter(x: Int): Int = x * 2
  case class A(id: Int = -1, p1: Int = getter(10))
}

class ParamTest extends SurfaceSpec {
  scalaJsSupport

  def `have default value`: Unit = {
    val s = Surface.of[ParamTest.A]
    val p = s.params.head
    assert(p.getDefaultValue == Option(-1))
    val p1 = s.params(1)
    assert(p1.getDefaultValue == Option(20))
  }
}
