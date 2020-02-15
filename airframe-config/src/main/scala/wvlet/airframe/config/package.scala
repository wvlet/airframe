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
import wvlet.airframe.surface.Surface
import wvlet.log.Logger

import scala.language.experimental.macros

/**
  *
  */
package object config {
  private val logger = Logger("wvlet.airframe.config")

  import scala.reflect.runtime.{universe => ru}

  implicit class ConfigurableDesign(d: Design) {
    def showConfig: Design = {
      processConfig { c => logger.info(c.printConfig) }
    }

    /**
      * Process Config
      */
    def processConfig(configProcessor: Config => Unit): Design = {
      configProcessor(currentConfig)
      d
    }

    // Get config binded in the design.
    def getConfig: Option[Config] = {
      d.getDesignConfig.options.get("config").map(_.asInstanceOf[Config])
    }

    def currentConfig: Config = {
      getConfig match {
        case Some(c) => c
        case None    => Config()
      }
    }

    def withConfig(c: Config): Design = {
      d.withOption("config", c)
    }

    def withConfigEnv(env: String, defaultEnv: String = "default"): Design = {
      d.withConfig(currentConfig.withEnv(env, defaultEnv))
    }

    def withConfigPaths(configPaths: Seq[String]): Design = {
      d.withConfig(currentConfig.withConfigPaths(configPaths))
    }

    def bindConfig[A: ru.TypeTag](config: A)(implicit sourceCode: SourceCode): Design = {
      val configHolder = currentConfig.register[A](config)
      val s            = Surface.of[A]
      d.withConfig(configHolder)
        .bind(s)(sourceCode).toInstance(config)
    }

    def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String)(implicit sourceCode: SourceCode): Design = {
      val configHolder = currentConfig.registerFromYaml[A](yamlFile)
      d.withConfig(configHolder)
        .bind(Surface.of[A])(sourceCode).toInstance(configHolder.of[A])
    }

    def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String, defaultValue: => A)(
        implicit sourceCode: SourceCode
    ): Design = {
      val configHolder = currentConfig.registerFromYamlOrElse[A](yamlFile, defaultValue)
      val s            = Surface.of[A]
      val newConfig    = configHolder.of[A]
      d.withConfig(configHolder)
        .bind(s)(sourceCode).toInstance(newConfig)
    }

    /**
      * Override a subset of the configuration parameters, registered to the design.
      */
    def overrideConfigParams(
        props: Map[String, Any],
        onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
    )(implicit sourceCode: SourceCode): Design = {
      overrideConfig { c => c.overrideWith(props, onUnusedProperties) }
    }

    /**
      * Override a subset of the configuration parameters, registered to the design.
      */
    def overrideConfigParamsWithProperties(
        props: Properties,
        onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
    ): Design = {
      overrideConfig { c => c.overrideWithProperties(props, onUnusedProperties) }
    }

    /**
      * Override a subset of the configuration parameters, registered to the design.
      */
    def overrideConfigWithPropertiesFile(
        propertiesFile: String,
        onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
    )(implicit sourceCode: SourceCode): Design = {
      overrideConfig { c => c.overrideWithPropertiesFile(propertiesFile, onUnusedProperties) }
    }

    private def overrideConfig(f: Config => Config)(implicit sourceCode: SourceCode): Design = {
      val configHolder = f(currentConfig)
      val d2           = d.withConfig(configHolder)

      // Override already bounded config instances
      val d3 = configHolder.getAll.foldLeft(d2) { (d: Design, c: ConfigHolder) =>
        d.bind(c.tpe)(sourceCode).toInstance(c.value)
      }
      d3
    }
  }
}
