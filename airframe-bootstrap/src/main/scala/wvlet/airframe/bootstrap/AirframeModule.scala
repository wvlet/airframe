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
package wvlet.airframe.bootstrap

import wvlet.airframe.Design
import wvlet.airframe.config.Config

/**
  * Airframe
  */
object AirframeModule {
  def newModule =
    new AirframeModule(design = { d: Design =>
      d
    }, config = { c: Config =>
      c
    })
}

case class AirframeModule(design: Design => Design, config: Config => Config) {
  def withDesign(d: Design => Design): AirframeModule =
    new AirframeModule(design.andThen(d), config)
  def withConfig(c: Config => Config): AirframeModule =
    new AirframeModule(design, config.andThen(c))

  def +(other: AirframeModule): AirframeModule =
    new AirframeModule(design.andThen(other.design), config.andThen(other.config))
}
