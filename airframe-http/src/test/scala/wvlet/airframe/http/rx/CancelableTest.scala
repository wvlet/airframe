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
package wvlet.airframe.http.rx

import wvlet.airspec.AirSpec

/**
  */
class CancelableTest extends AirSpec {

  test("Merge Cancelables") {

    val m1 = Cancelable.merge(Cancelable.empty, Cancelable.empty)
    m1.cancel

    var x  = 0
    val m2 = Cancelable.merge(Cancelable.empty, Cancelable { () => x += 1 })
    m2.cancel
    x shouldBe 1

    var y  = 0
    var z  = 0
    val m3 = Cancelable.merge(Cancelable { () => y += 1 }, Cancelable { () => z += 1 })
    m3.cancel
    y shouldBe 1
    z shouldBe 1

    var a  = 0
    val m4 = Cancelable.merge(Cancelable { () => a += 1 }, Cancelable.empty)
    m4.cancel
    a shouldBe 1
  }

  test("Merge sequence of Cancelables") {
    val m0 = Cancelable.merge(Seq.empty)
    m0.cancel

    var a  = 0
    val m1 = Cancelable.merge(Seq(Cancelable(() => a += 1)))
    a shouldBe 0
    m1.cancel
    a shouldBe 1

    var x = 0
    var z = 0
    val m = Cancelable.merge(Seq(Cancelable(() => x += 1), Cancelable.empty, Cancelable(() => z += 1)))

    x shouldBe 0
    z shouldBe 0
    m.cancel

    x shouldBe 1
    z shouldBe 1
  }

}
