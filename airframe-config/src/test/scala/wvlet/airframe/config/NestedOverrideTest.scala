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
import wvlet.airframe.config.ConfigOverrideTest.MyAppConfig
import wvlet.airspec.AirSpec

object NestedOverrideTest {
  case class LogConfig(file: LogFileConfig)
  case class LogFileConfig(name: String, maxFileSize: Int)
}

/**
  */
class NestedOverrideTest extends AirSpec {
  private val configPaths = Seq("airframe-config/src/test/resources")

  import NestedOverrideTest._

  test("override nested yaml") {
    val config =
      Config(env = "default", configPaths = configPaths)
        .registerFromYaml[LogConfig]("override.yml")
        .of[LogConfig]

    config shouldBe LogConfig(LogFileConfig("access.log", 20))

    val configDev =
      Config(env = "development", configPaths = configPaths)
        .registerFromYaml[LogConfig]("override.yml")
        .of[LogConfig]

    configDev shouldBe LogConfig(LogFileConfig("access.log", 5))
  }
}
