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
import wvlet.config.{Config, ConfigHolder}
import wvlet.log.LogSupport

object AirframeModule {
  // Use base path that can be set by sbt-pack generated scripts
  def basePath           = sys.props.getOrElse("prog.home", ".")
  def defautlConfigPaths = Seq(s"${basePath}/config")

  implicit class RichDesign(d: Design) {
    def bindConfig(config: Config): Design = {
      config.getAll.foldLeft(d) { (d: Design, c: ConfigHolder) =>
        d.bind(c.tpe).toInstance(c.value)
      }
    }
  }

}

trait AirframeModule extends LogSupport {
  import AirframeModule._

  def configure(config: Config): Config = config
  def design(d: Design): Design         = d

  def bootstrap(env: String = "default",
                defaultEnv: String = "default",
                configPaths: Seq[String] = defautlConfigPaths,
                overrideConfigParams: Map[String, Any] = Map.empty): AirframeBootstrap = {
    val newConfig =
      configure(Config(env = env, defaultEnv = defaultEnv, configPaths = configPaths))
        .overrideWith(overrideConfigParams)
    val newDesign = design(Design.blanc).bindConfig(newConfig)

    new AirframeBootstrap {
      override val config = newConfig
      override val design = newDesign
    }
  }

  def +(other: AirframeModule): AirframeModule = ModuleChain(this, other)
}

private[bootstrap] case class ModuleChain(prev: AirframeModule, next: AirframeModule) extends AirframeModule {
  override def configure(config: Config): Config = next.configure(prev.configure(config))
  override def design(design: Design): Design    = next.design(prev.design(design))
}
