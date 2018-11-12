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

import wvlet.airframe.surface

object RecursiveSurfaceTest {

  case class Leaf(name: String)
  case class Cons(head: String, tail: Cons)
  case class TypedCons[A](head: String, tail: TypedCons[A])

}

/**
  *
  */
class RecursiveSurfaceTest extends SurfaceSpec {

  import RecursiveSurfaceTest._

  "Surface" should {
    "find surface from full type name string" in {
      val s = Surface.of[Leaf]
      surface.getCached("wvlet.airframe.surface.RecursiveSurfaceTest.Leaf") shouldBe s
    }

    "support recursive type" in {
      val c: Surface = Surface.of[Cons]
      c.toString shouldBe "Cons"

      c.params should have length (2)
      val h = c.params(0)
      h.name shouldBe "head"
      h.surface shouldBe Primitive.String

      val t = c.params(1)
      t.name shouldBe "tail"
      val lazyC: Surface = t.surface
      lazyC.toString shouldBe "Cons"
      lazyC.params should have length (2)
      lazyC.isPrimitive shouldBe false
      lazyC.isOption shouldBe false
      lazyC.isAlias shouldBe false
      lazyC.objectFactory shouldBe defined
    }

    "support generic recursive type" in {
      val c: Surface = Surface.of[TypedCons[String]]
      c.toString shouldBe "TypedCons[String]"

      c.params should have length (2)
      c.params(0).surface shouldBe Primitive.String

      val lazyC: Surface = c.params(1).surface
      lazyC.toString shouldBe "TypedCons[String]"
      lazyC.params should have length (2)
      lazyC.isPrimitive shouldBe false
      lazyC.isOption shouldBe false
    }
  }
}
