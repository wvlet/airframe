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
import java.util.Properties

import wvlet.airframe.config.Config.REPORT_UNUSED_PROPERTIES
import wvlet.surface

import scala.reflect.ClassTag

/**
  *
  */
package object config {
  import wvlet.airframe._
  import scala.reflect.runtime.{universe => ru}

  private val configKey = "wvlet.airframe.config.Config"

  implicit class ConfigurableDesign(d: Design) {

    private def getConfigHolder: Config = {
      d.getDesignConfig.props
        .getOrElse(configKey, Config())
        .asInstanceOf[Config]
    }

    def withConfigEnv(env: String, defaultEnv: String = "default"): Design = {
      d.withProperty(configKey, getConfigHolder.withEnv(env, defaultEnv))
    }

    def withConfigPaths(configPaths: Seq[String]): Design = {
      d.withProperty(configKey, getConfigHolder.withConfigPaths(configPaths))
    }

    def bindConfig[A: ru.TypeTag](config: A): Design = {
      val configHolder = getConfigHolder.register[A](config)
      val s            = surface.of[A]
      d.withProperty(configKey, configHolder)
        .bind(s).toInstance(config)
    }

    def bindConfigDefault[A: ru.TypeTag]: Design = {
      val configHolder = getConfigHolder.registerDefault[A]
      d.withProperty(configKey, configHolder)
        .bind(surface.of[A]).toInstance(configHolder.of[A])
    }

    def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String): Design = {
      val configHolder = getConfigHolder.registerFromYaml[A](yamlFile)
      d.withProperty(configKey, configHolder)
        .bind(surface.of[A]).toInstance(configHolder[A])
    }
    def bindConfigFromYaml[A: ru.TypeTag: ClassTag](yamlFile: String, defaultValue: => A): Design = {
      val configHolder = getConfigHolder.registerFromYamlOrElse[A](yamlFile, defaultValue)
      val s            = surface.of[A]
      val newConfig    = configHolder.of[A]
      d.withProperty(configKey, configHolder)
        .bind(s).toInstance(newConfig)
    }

    def withConfigOverride(props: Map[String, Any],
                           onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES): Design = {
      val configHolder = getConfigHolder.overrideWith(props, onUnusedProperties)
      val newDesign    = d.withProperty(configKey, configHolder)

      // Override alrady bounded config instances
      configHolder.getAll.foldLeft(newDesign) { (d: Design, c: ConfigHolder) =>
        d.bind(c.tpe).toInstance(c.value)
      }
    }

    def withConfigOverrideProperties(props: Properties,
                                     onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES): Design = {}

  }
}
