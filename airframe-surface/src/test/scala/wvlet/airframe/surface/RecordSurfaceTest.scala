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

class RecordSurfaceTest extends AirSpec {
  test("build custom surface") {
    val p1 = RecordParameter(0, "p1", Primitive.Int)
    val p2 = RecordParameter(1, "p2", Primitive.String)
    val p3 = RecordParameter(2, "p3", OptionSurface(classOf[Option[Long]], Primitive.Long))
    val s = RecordSurface
      .newSurface("myrecord")
      .addParam(p1)
      .addParam(p2)
      .addParam(p3)

    s.typeArgs shouldBe empty
    s.params.length shouldBe 3
    s.params(0) shouldBe p1
    s.params(1) shouldBe p2
    s.params(2) shouldBe p3
  }
}
