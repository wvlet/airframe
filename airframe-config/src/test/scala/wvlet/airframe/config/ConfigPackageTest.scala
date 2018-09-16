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
import wvlet.airframe.AirframeSpec

/**
  *
  */
class ConfigPackageTest extends AirframeSpec {
  import wvlet.airframe._
  import wvlet.airframe.config._

  private val configPaths = Seq("airframe-config/src/test/resources")

  def loadConfig(env: String) =
    Config(env = env, configPaths = configPaths)
      .registerFromYaml[SampleConfig]("myconfig.yml")
      .registerFromYaml[ClassConfig]("classes.yml")

  "config package" should {
    "bind config with Airframe design" in {
      val d = newDesign.noLifeCycleLogging
        .withConfigEnv(env = "development")
        .withConfigPaths(configPaths)
        .bindConfig[DefaultConfig](DefaultConfig(10, "aina"))
        .bindConfigFromYaml[ClassConfig]("classes.yml")
        .bindConfigFromYaml[SampleConfig]("myconfig--.yml", SampleConfig(1, "leo"))

      d.withSession { session =>
        session.build[DefaultConfig] shouldBe DefaultConfig(10, "aina")

        val classConfig = session.build[ClassConfig]
        classConfig.classes shouldBe Seq("class1", "class2", "class3")
        classConfig.classAssignments shouldBe Map("nobita" -> "class1", "takeshi" -> "class2", "suneo" -> "class3")
        session.build[SampleConfig] shouldBe SampleConfig(1, "leo")
      }
    }
  }
}
