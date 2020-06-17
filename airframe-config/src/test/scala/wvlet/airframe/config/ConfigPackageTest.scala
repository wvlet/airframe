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
import wvlet.airframe._
import wvlet.airframe.surface.tag._
import wvlet.airspec.AirSpec

trait AppTag

/**
  */
class ConfigPackageTest extends AirSpec {
  private val configPaths = Seq("airframe-config/src/test/resources")

  def `bind config with Airframe design`: Unit = {
    val d = newDesign.noLifeCycleLogging
      .withConfigEnv(env = "development")
      .withConfigPaths(configPaths)
      .bindConfig[DefaultConfig](DefaultConfig(10, "aina"))
      .bindConfigFromYaml[ClassConfig]("classes.yml")
      // Specifying non exising yaml file
      .bindConfigFromYaml[SampleConfig]("myconfig--.yml", SampleConfig(1, "leo"))
      // Switching env
      .withConfigEnv(env = "staging")
      .bindConfigFromYaml[SampleConfig @@ AppTag]("myconfig.yml")
      .overrideConfigParams(Map("sample.id" -> 2))

    d.withSession { session =>
      session.build[DefaultConfig] shouldBe DefaultConfig(10, "aina")

      val classConfig = session.build[ClassConfig]
      classConfig.classes shouldBe Seq("class1", "class2", "class3")
      classConfig.classAssignments shouldBe Map("nobita" -> "class1", "takeshi" -> "class2", "suneo" -> "class3")
      session.build[SampleConfig] shouldBe SampleConfig(2, "leo")
      session.build[SampleConfig @@ AppTag] shouldBe SampleConfig(2, "staging-config")
    }
  }
}
