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

import scala.language.higherKinds

object RecursiveHigherKindTypeTest {
  trait Holder[M[_]]

  class MyTask[A]

  object Holder {
    type BySkinny[A] = MyTask[A]
    def bySkinny: Holder[BySkinny] = new InterpretedHolder
  }

  import Holder._
  class InterpretedHolder extends Holder[BySkinny] {}
}

/**
  */
class RecursiveHigherKindTypeTest extends SurfaceSpec {

  import RecursiveHigherKindTypeTest._
  import Holder.BySkinny

  test("support recursive higher kind types") {
    val s = Surface.of[Holder[BySkinny]]
    assertEquals(s.name, "Holder[BySkinny]")
    assertEquals(s.isAlias, false)
    assertEquals(s.isPrimitive, false)
    assertEquals(s.isOption, false)
    assertEquals(s.dealias.toString, "Holder[BySkinny]")

    assertEquals(s.typeArgs(0).name, "BySkinny")
    assertEquals(s.typeArgs(0).dealias.name, "MyTask[A]")
  }
}
