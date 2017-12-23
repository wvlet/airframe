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
package wvlet.config

import wvlet.airframe.AirframeSpec

object ConfigOverrideTest {
  case class AppConfig(
      coordinatorAddress: String = "localhost:8080",
      name: String = "myapp"
  )
}

/**
  *
  */
class ConfigOverrideTest extends AirframeSpec {
  import ConfigOverrideTest._

  "Config" should {
    "override config via canonical param name" in {

      val prop = Map("app.coordinator_address" -> "mylocalhost:8081")

      val config: Config = Config(env = "default").register[AppConfig](AppConfig()).overrideWith(prop)

      val appConfig = config.of[AppConfig]
      info(s"AppConfig: ${appConfig}")
      appConfig.coordinatorAddress shouldBe "mylocalhost:8081"
      appConfig.name shouldBe "myapp"
    }
  }
}
