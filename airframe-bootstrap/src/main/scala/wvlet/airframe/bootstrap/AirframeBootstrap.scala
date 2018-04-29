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

object AirframeBootstrap extends LogSupport {
  // Use base path that can be set by sbt-pack generated scripts
  def basePath           = sys.props.getOrElse("prog.home", ".")
  def defautlConfigPaths = Seq(s"${basePath}/config")

  def apply(module: AirframeModule, env: String = "default", defaultEnv: String = "default", configPaths: Seq[String] = defautlConfigPaths): AirframeBootstrap = {
    new AirframeBootstrap(module, env, defaultEnv, configPaths)
  }

  def toDesign(c: Config): Design = {
    c.getAll.foldLeft(Design.blanc) { (d: Design, c: ConfigHolder) =>
      d.bind(c.tpe).toInstance(c.value)
    }
  }

  def showConfig(c: Config): Unit = {
    info("Configurations:")
    for (c <- c.getAll) {
      info(s"${c.tpe}: ${c.value}")
    }
  }
}

import wvlet.airframe.bootstrap.AirframeBootstrap._
class AirframeBootstrap(module: AirframeModule,
                        env: String = "default",
                        defaultEnv: String = "default",
                        configPaths: Seq[String] = AirframeBootstrap.defautlConfigPaths,
                        overrideConfigParams: Map[String, Any] = Map.empty,
                        configProcessor: Config => Unit = AirframeBootstrap.showConfig) {

  def withEnv(env: String, defaultEnv: String = "default") =
    new AirframeBootstrap(module, env, defaultEnv, configPaths, overrideConfigParams, configProcessor)
  def withConfigPaths(configPaths: Seq[String]) =
    new AirframeBootstrap(module, env, defaultEnv, configPaths, overrideConfigParams, configProcessor)
  def withConfigOverrides(configParams: Map[String, Any]) =
    new AirframeBootstrap(module, env, defaultEnv, configPaths, configParams, configProcessor)

  /**
    * Change the configuration processor. The default behavior is dumping all configurations to the logger
    * @param p
    */
  def withConfigProcessor(p: Config => Unit): Unit = {
    new AirframeBootstrap(module, env, defaultEnv, configPaths, overrideConfigParams, p)
  }

  /**
    * Perform the initialization steps.
    * It loads the configurations and binds designs, then returns the final design object.
    * @return the final design
    */
  def bootstrap: Design = {
    // Load configurations first
    val newConfig =
      module
        .config(Config(env = env, defaultEnv = defaultEnv, configPaths = configPaths))
        .overrideWith(overrideConfigParams)

    // Process config (default is showing configuration values)
    configProcessor(newConfig)

    // Then start design, then add the config as designs
    val newDesign = module.design(Design.blanc) + toDesign(newConfig)
    newDesign
  }

}
