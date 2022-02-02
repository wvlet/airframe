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
package wvlet.airframe.surface.reflect

import scala.language.higherKinds
import wvlet.airframe.surface.Surface

/**
  */
class NamedParameterTest extends munit.FunSuite {

  trait MyService[F[_]] {
    def hello: F[String]
  }

  trait A[Elem]

  test("read F[_]") {
    val s = Surface.of[MyService[A]]
    assertEquals(s.toString, "MyService[A]")

    val m = Surface.methodsOf[MyService[A]]
    assert(m.headOption.isDefined)

    val m1 = m.head
    // info(m1.returnType.getClass())
    assertEquals(m1.returnType.toString, "F[String]")
  }
}
