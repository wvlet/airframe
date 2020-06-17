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
package wvlet.airframe.examples.di

import wvlet.airframe.surface.Surface

/**
  */
object DI_21_BindConfig extends App {
  case class LogConfig(file: String)
  case class ServerConfig(host: String)
  case class X()

  import wvlet.airframe._
  // Import config._ to use bindConfigXXX methods
  import wvlet.airframe.config._

  // Load "production" configurations from Yaml files
  val design =
    newDesign
    // Set an environment to use
      .withConfigEnv(env = "production", defaultEnv = "default")
      // Load configs from YAML files
      .bindConfigFromYaml[LogConfig]("access-log.yml")
      .bindConfigFromYaml[ServerConfig]("server.yml")
      // Bind other designs
      .bind[X].toInstance(X())

  // If you need to override some config parameters, prepare Map[String, Any] objects:
  val properties = Map(
    "server.host" -> "xxx.xxx.xxx"
  )

  // Override config with property values
  val finalDesign =
    design.overrideConfigParams(properties)

  finalDesign.build[X] { x =>
    // ... start the application
  }
}
