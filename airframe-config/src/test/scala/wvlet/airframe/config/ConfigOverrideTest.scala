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
package wvlet.airframe.config

import wvlet.airspec.AirSpec

object ConfigOverrideTest {
  case class MyAppConfig(
      coordinatorAddress: String = "localhost:8080",
      name: String = "myapp"
  )
}

/**
  */
class ConfigOverrideTest extends AirSpec {
  import ConfigOverrideTest._

  def newConfig: Config = Config(env = "default").register[MyAppConfig](MyAppConfig())

  test("override config via canonical param name") {
    val prop = Map("myapp.coordinator_address" -> "mylocalhost:8081")
    val appConfig =
      newConfig
        .overrideWith(prop)
        .of[MyAppConfig]
    debug(s"AppConfig: ${appConfig}")

    appConfig.coordinatorAddress shouldBe "mylocalhost:8081"
    appConfig.name shouldBe "myapp"
  }

  test("override config with key names with hyphen") {
    val prop = Map("my-app.coordinator-address" -> "mylocalhost:8081")

    val appConfig =
      newConfig
        .overrideWith(prop)
        .of[MyAppConfig]
    debug(s"AppConfig: ${appConfig}")

    appConfig.coordinatorAddress shouldBe "mylocalhost:8081"
    appConfig.name shouldBe "myapp"
  }
}
