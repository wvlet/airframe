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

  type A1 = A

  case class B(pub: Int, private val priv1: Int, private val priv2: Int = 100)
}

class ParamTest extends SurfaceSpec {

  test("have default value") {
    val s = Surface.of[ParamTest.A]
    val p = s.params.head
    assert(p.getDefaultValue == Option(-1))
    val p1 = s.params(1)
    assert(p1.getDefaultValue == Option(20))
  }

  test("public field access") {
    val s  = Surface.of[ParamTest.B]
    val p1 = s.params(0)
    val v  = ParamTest.B(1, 2, 3)
    assertEquals(p1.get(v), 1)
  }

  test("access params through alias") {
    val s               = Surface.of[ParamTest.A1]
    val p1              = s.params(0)
    val p2              = s.params(1)
    val v: ParamTest.A1 = ParamTest.A(10, 20)
    assertEquals(p1.get(v), 10)
    assertEquals(p2.get(v), 20)
  }

  test("private field access") {
    if (isScalaJS || Surface.scalaMajorVersion == 3) {
      pendingUntil("Find a way to access private fields in Scala.js and Scala 3")
    }
    val s  = Surface.of[ParamTest.B]
    val p1 = s.params(0)
    val p2 = s.params(1)
    val p3 = s.params(2)
    val v  = ParamTest.B(1, 2, 3)
    assertEquals(p1.get(v), 1)
    assertEquals(p2.get(v), 2)
    assertEquals(p3.get(v), 3)
  }
}
