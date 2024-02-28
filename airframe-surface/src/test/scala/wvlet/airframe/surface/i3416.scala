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

object i3416 extends AirSpec {

  object O {
    class C(private[O] val id: Int) {
      private[O] def getId: Int = id
    }
  }

  test("List package private fields as parameters") {
    val s = Surface.of[O.C]
    debug(s.params)
    s.params.size shouldBe 1
    val p1 = s.params(0)
    p1.name shouldBe "id"
  }

  test("Do not list package private methods") {
    val s = Surface.methodsOf[O.C]
    s.size shouldBe 0
  }
}
