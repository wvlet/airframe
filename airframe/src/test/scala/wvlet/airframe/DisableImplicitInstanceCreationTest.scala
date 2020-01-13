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
package wvlet.airframe

import wvlet.airframe.AirframeException.MISSING_DEPENDENCY
import wvlet.airspec.AirSpec

object DisableImplicitInstanceCreationTest {
  case class Component(config: Config)
  case class Config(value: String = "test")
}

class DisableImplicitInstanceCreationTest extends AirSpec {
  import DisableImplicitInstanceCreationTest._
  scalaJsSupport

  def `disable implicit instance creation`: Unit = {
    val d = Design.newDesign.bind[Component].toSingleton.disableImplicitInstanceCreation
    intercept[MISSING_DEPENDENCY] {
      d.build[Component] { _ =>
      }
    }
  }

  def `disable implicit instance creation with production mode`: Unit = {
    val d = Design.newDesign.bind[Component].toSingleton.disableImplicitInstanceCreation.withProductionMode
    intercept[MISSING_DEPENDENCY] {
      d.withSession { _ =>
      }
    }
  }

  def `enable implicit instance creation`: Unit = {
    val d = Design.newDesign.bind[Component].toSingleton
    d.build[Component] { c =>
      assert(c.config.value == "test")
    }
  }
}
