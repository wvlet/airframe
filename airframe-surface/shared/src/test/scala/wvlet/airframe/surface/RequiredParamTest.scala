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

case class ModelWithRequiredParam(@required id: String, name: String)

/**
  *
  */
class RequiredParamTest extends AirSpec {
  scalaJsSupport

  def `find required annotation`: Unit = {
    val s      = Surface.of[ModelWithRequiredParam]
    val p_id   = s.params(0)
    val p_name = s.params(1)

    p_id.isRequired shouldBe true
    p_name.isRequired shouldBe false
  }
  case class LocalA(@required id: String, name: String)

  def `find required annotation from local classes`: Unit = {
    val s    = Surface.of[LocalA]
    val p_id = s.params.find(_.name == "id").get
    p_id.isRequired shouldBe true
  }
}
