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

object i3418 extends AirSpec {

  case class A[P](data: List[(String, P)])
  case class AA[T, P](data: List[(T, P)])

  case class S(a: A[Int])
  case class SS(a: AA[String, Long])

  test("Support generic List of tuples") {
    val s = Surface.of[S]
    debug(s.params)
    s.params.size shouldBe 1
    val p1 = s.params(0)
    p1.name shouldBe "a"
    val a1 = p1.surface.params(0)
    a1.name shouldBe "data"
    a1.surface.name shouldBe "List[Tuple2[String,Int]]"
  }

  test("Support generic List of tuples with two type parameters") {
    val s = Surface.of[SS]
    debug(s.params)
    s.params.size shouldBe 1
    val p1 = s.params(0)
    p1.name shouldBe "a"
    val a1 = p1.surface.params(0)
    a1.name shouldBe "data"
    a1.surface.name shouldBe "List[Tuple2[String,Long]]"
  }
}
