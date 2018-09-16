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
import wvlet.log.{LogSupport, Logger}
import wvlet.surface

import scala.reflect.ClassTag

/**
  *
  */
package object config {
  private val logger = Logger("wvlet.airframe.config")

  import wvlet.airframe._
  import scala.reflect.runtime.{universe => ru}

  def printConfig(c: Config): Unit = {
    logger.info("Configurations:")
    for (c <- c.getAll) {
      logger.info(s"${c.tpe}: ${c.value}")
    }
  }

  implicit class ConfigurableDesign(d: Design) {

    def showConfig: Design = {
      bootstrapWithConfigProcessor(printConfig)
    }

    def bootstrapWithConfigProcessor(configProcessor: Config => Unit): Design = {
      configProcessor(getConfig)
      d
    }

    private def getConfig: Config = {
      d.getDesignConfig match {
        case dc: DesignOptionsWithConfig =>
          dc.config
        case _ =>
          Config()
      }
    }

    def withConfig(c: Config): Design = {
      d.designOptions match {
        case dc: DesignOptionsWithConfig =>
          Design(dc.withConfig(c), d.binding)
        case other =>
          Design(new DesignOptionsWithConfig(other.enabledLifeCycleLogging, other.stage, c), d.binding)
      }
    }

    def withConfigEnv(env: String, defaultEnv: String = "default"): Design = {
      d.withConfig(getConfig.withEnv(env, defaultEnv))
    }

    def withConfigPaths(configPaths: Seq[String]): Design = {
      d.withConfig(getConfig.withConfigPaths(configPaths))
    }

    def bindConfig[A: ru.TypeTag](config: A): Design = {
      val configHolder = getConfig.register[A](config)
      val s            = surface.of[A]
      d.withConfig(configHolder)
        .bind(s).toInstance(config)
    }

    def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String): Design = {
      val configHolder = getConfig.registerFromYaml[A](yamlFile)
      d.withConfig(configHolder)
        .bind(surface.of[A]).toInstance(configHolder.of[A])
    }

    def bindConfigFromYaml[A: ru.TypeTag: ClassTag](yamlFile: String, defaultValue: => A): Design = {
      val configHolder = getConfig.registerFromYamlOrElse[A](yamlFile, defaultValue)
      val s            = surface.of[A]
      val newConfig    = configHolder.of[A]
      d.withConfig(configHolder)
        .bind(s).toInstance(newConfig)
    }

    def withConfigOverride(props: Map[String, Any],
                           onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES): Design = {
      val prevConfig   = getConfig
      val configHolder = getConfig.overrideWith(props, onUnusedProperties)
      val d2           = d.withConfig(configHolder)

      // Override already bounded config instances
      val d3 = configHolder.getAll.foldLeft(d2) { (d: Design, c: ConfigHolder) =>
        d.bind(c.tpe).toInstance(c.value)
      }
      d3
    }

    def withConfigOverrideProperties(props: Properties,
                                     onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES): Design = {
      import scala.collection.JavaConverters._
      val m = for (key <- props.propertyNames().asScala) yield {
        key.toString -> props.get(key).asInstanceOf[Any]
      }
      withConfigOverride(m.toMap, onUnusedProperties)
    }
  }
}
