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

case class ModelWithRequiredParam(@required id: String, name: String) {
  def method(a: String, @required b: Int): Unit = {}
}

/**
  */
class RequiredParamTest extends AirSpec {
  test("find required annotation") {
    val s      = Surface.of[ModelWithRequiredParam]
    val p_id   = s.params(0)
    val p_name = s.params(1)

    p_id.isRequired shouldBe true
    p_name.isRequired shouldBe false
  }

  test("find required method param annotation") {
    val ms = Surface.methodsOf[ModelWithRequiredParam]
    val m  = ms.find(_.name == "method").get

    m.args(0).isRequired shouldBe false
    m.args(1).isRequired shouldBe true
  }

  case class LocalA(@required id: String, name: String)

  test("find required annotation from local classes") {
    val s    = Surface.of[LocalA]
    val p_id = s.params.find(_.name == "id").get
    p_id.isRequired shouldBe true
  }
}
