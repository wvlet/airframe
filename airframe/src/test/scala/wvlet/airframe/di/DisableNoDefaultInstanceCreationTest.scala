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
package wvlet.airframe.di

import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airframe.Design
import wvlet.airspec.AirSpec

object DisableNoDefaultInstanceCreationTest {
  case class Component(config: Config)
  case class Config(value: String = "test")
}

class DisableNoDefaultInstanceCreationTest extends AirSpec {
  import DisableNoDefaultInstanceCreationTest._

  test("disable implicit instance creation") {
    val d = Design.newSilentDesign.bind[Component].toSingleton.noDefaultInstanceInjection
    intercept[MISSING_DEPENDENCY] {
      d.build[Component] { _ => }
    }
  }

  test("disable implicit instance creation with production mode") {
    val d = Design.newSilentDesign.bind[Component].toSingleton.noDefaultInstanceInjection.withProductionMode
    intercept[MISSING_DEPENDENCY] {
      d.withSession { _ => }
    }
  }

  test("enable implicit instance creation") {
    val d = Design.newSilentDesign.bind[Component].toSingleton
    d.build[Component] { c => assert(c.config.value == "test") }
  }
}
