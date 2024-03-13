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

object i3451 extends AirSpec {

  case class Cons(head: String, tail: Cons)
  case class TypedCons[A](head: Int, tail: TypedCons[A])

  trait Recursive[T <: Recursive[T]]

  case class E(x: Int) extends Recursive[E] {
    def cons(c: Cons): Cons                      = c
    def typedCons(c: TypedCons[_]): TypedCons[_] = c
    def r3(r: Recursive[_]): Recursive[_]        = r
  }

  test("Support methods with lazy and non-lazy types mixed in any order") {
    val s = Surface.of[E]
    debug(s.params)
    val m     = Surface.methodsOf[E]
    val names = m.map(_.name)
    names shouldContain "cons"
    names shouldContain "typedCons"
    names shouldContain "r3"
  }
}
