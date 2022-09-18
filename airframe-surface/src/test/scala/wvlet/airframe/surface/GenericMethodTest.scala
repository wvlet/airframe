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

object GenericMethodTest {
  class A {
    def helloX[X](v: X): String = "hello"
  }
}

class GenericMethodTest extends SurfaceSpec {
  import GenericMethodTest._

  test("generic method") {
    val methods = Surface.methodsOf[A]
    assertEquals(methods.size, 1)
    val m = methods(0)

    val obj = new GenericMethodTest.A
    assertEquals(m.call(obj, "dummy"), "hello")
  }

}
