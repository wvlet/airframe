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

object OpaqueTypeTest extends AirSpec:
  opaque type Objc[A] <: Map[String, A] = Map[String, A]
  type AliasType[A]                     = Map[String, A]

  class Local0:
    def related: AliasType[Any] = null

  test("type alias with type args") {
    val x2 = Surface.methodsOf[Local0]
    val m1 = x2.find(_.name == "related").get
    m1.returnType.toString shouldBe "AliasType[Any]:=Map[String,Any]"
  }

  class Local1:
    def related: Objc[Any] = null

  test("opaque type with type args") {
    val x2 = Surface.methodsOf[Local1]
    val m1 = x2.find(_.name == "related").get
    m1.returnType.toString shouldBe "Objc[Any]:=Map[String,Any]"
  }
