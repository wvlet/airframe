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

/**
  */
package object config {
  private val logger = Logger("wvlet.airframe.config")

  implicit class ConfigurableDesign(d: Design) extends ConfigPackageCompat {
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

    private[config] def bindConfigInternal[A](surface: Surface, config: A)(implicit sourceCode: SourceCode): Design = {
      val configHolder = currentConfig.registerOfSurface[A](surface, config)
      d.withConfig(configHolder)
        .bind(surface)(sourceCode).toInstance(config)
    }

    private[config] def bindConfigFromYamlInternal[A](
        surface: Surface,
        yamlFile: String
    )(implicit sourceCode: SourceCode): Design = {
      val configHolder = currentConfig.registerFromYaml[A](surface, yamlFile)
      d.withConfig(configHolder)
        .bind(surface)(sourceCode).toInstance(configHolder.ofSurface[A](surface))
    }

    private[config] def bindConfigFromYamlInternal[A](surface: Surface, yamlFile: String, defaultValue: => A)(implicit
        sourceCode: SourceCode
    ): Design = {
      val configHolder = currentConfig.registerFromYamlOrElse[A](surface, yamlFile, defaultValue)
      val newConfig    = configHolder.ofSurface[A](surface)
      d.withConfig(configHolder)
        .bind(surface)(sourceCode).toInstance(newConfig)
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
