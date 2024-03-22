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
  * note: this is a regression test for a non-deterministic issue, see https://github.com/scala/scala3/issues/19795
  */

object i3417 extends AirSpec {
  trait MyOption[T]

  class Wrap(val option: MyOption[Int])
  class MultiWrap(option: MyOption[Int], seq: Seq[(Double, MyOption[String])], map: Map[MyOption[Double], Double])

  test("handle generic types on parameters") {
    val s = Surface.of[Wrap]
    s.params.size shouldBe 1
    s.params.head.surface.name shouldBe "MyOption[Int]"

    val m = Surface.of[MultiWrap]
    m.params.size shouldBe 3
    m.params(0).surface.name shouldBe "MyOption[Int]"
    m.params(1).surface.name shouldBe "Seq[Tuple2[Double,MyOption[String]]]"
    m.params(2).surface.name shouldBe "Map[MyOption[Double],Double]"
  }

}
