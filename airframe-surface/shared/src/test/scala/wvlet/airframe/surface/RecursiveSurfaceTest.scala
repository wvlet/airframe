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
  */
class RecursiveSurfaceTest extends SurfaceSpec {
  scalaJsSupport

  import RecursiveSurfaceTest._

  def `find surface from full type name string`: Unit = {
    val s = Surface.of[Leaf]
    assert(surface.getCached("wvlet.airframe.surface.RecursiveSurfaceTest.Leaf") == s)
  }

  def `support recursive type`: Unit = {
    val c: Surface = Surface.of[Cons]
    assert(c.toString == "Cons")

    assert(c.params.length == 2)
    val h = c.params(0)
    assert(h.name == "head")
    assert(h.surface == Primitive.String)

    val t = c.params(1)
    assert(t.name == "tail")
    val lazyC: Surface = t.surface
    assert(lazyC.toString == "Cons")
    assert(lazyC.params.length == 2)
    assert(lazyC.isPrimitive == false)
    assert(lazyC.isOption == false)
    assert(lazyC.isAlias == false)
    assert(lazyC.objectFactory.isDefined)
  }

  def `support generic recursive type`: Unit = {
    val c: Surface = Surface.of[TypedCons[String]]
    assert(c.toString == "TypedCons[String]")

    assert(c.params.length == 2)
    assert(c.params(0).surface == Primitive.String)

    val lazyC: Surface = c.params(1).surface
    assert(lazyC.toString == "TypedCons[String]")
    assert(lazyC.params.length == 2)
    assert(lazyC.isPrimitive == false)
    assert(lazyC.isOption == false)
  }
}
