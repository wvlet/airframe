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

import java.io.{File, FileNotFoundException, StringReader}
import java.util.Properties

import wvlet.airframe.Design
import wvlet.airframe.config.PropertiesConfig.ConfigKey
import wvlet.airframe.config.YamlReader.loadMapOf
import wvlet.airframe.surface.{Surface, Zero}
import wvlet.log.LogSupport
import wvlet.log.io.{IOUtil, Resource}

import scala.reflect.runtime.{universe => ru}

case class ConfigHolder(tpe: Surface, value: Any)

case class ConfigPaths(configPaths: Seq[String]) extends LogSupport {
  info(s"Config file paths: [${configPaths.mkString(", ")}]")
}

object Config extends LogSupport {
  def empty = Config()

  private[config] def defaultConfigPath =
    cleanupConfigPaths(
      Seq(
        // current directory
        ".",
        // program home for wvlet-launcher
        sys.props.getOrElse("prog.home", "")
      )
    )

  def apply(
      env: String = "default",
      defaultEnv: String = "default",
      configPaths: Seq[String] = defaultConfigPath
  ): Config =
    Config(ConfigEnv(env, defaultEnv, configPaths), Map.empty[Surface, ConfigHolder])

  def cleanupConfigPaths(paths: Seq[String]) = {
    val b = Seq.newBuilder[String]
    for (p <- paths) {
      if (!p.isEmpty) {
        b += p
      }
    }
    val result = b.result()
    if (result.isEmpty) {
      Seq(".") // current directory
    } else {
      result
    }
  }

  def REPORT_UNUSED_PROPERTIES: Properties => Unit = { unused: Properties =>
    warn(s"There are unused properties: ${unused}")
  }
  def REPORT_ERROR_FOR_UNUSED_PROPERTIES: Properties => Unit = { unused: Properties =>
    throw new IllegalArgumentException(s"There are unused properties: ${unused}")
  }
}

case class ConfigEnv(env: String, defaultEnv: String, configPaths: Seq[String]) {
  def withConfigPaths(paths: Seq[String]): ConfigEnv = ConfigEnv(env, defaultEnv, paths)
}

case class ConfigChange(tpe: Surface, key: ConfigKey, default: Any, current: Any) {
  override def toString = s"[${tpe}] ${key} = ${current} (default = ${default})"
}

import wvlet.airframe.config.Config._

case class Config private[config] (env: ConfigEnv, holder: Map[Surface, ConfigHolder])
    extends Iterable[ConfigHolder]
    with Design.AdditiveDesignOption[Config]
    with LogSupport {
  override def toString: String = printConfig

  def printConfig: String = {
    val s = Seq.newBuilder[String]
    s += "Configurations:"
    for (c <- getAll) {
      s += s"[${c.tpe}]"
      val paramWidth = c.tpe.params.map(_.name.length).max
      for (p <- c.tpe.params) {
        import wvlet.airframe.surface.reflect._
        val v = Option(p.get(c.value))
          .map { x =>
            x match {
              case Some(xx) =>
                // Unwrap optional value
                xx.toString
              case _ => x.toString
            }
          }
          .getOrElse("")
        val processedValue = p.findAnnotationOf[secret] match {
          case Some(h) =>
            if (h.mask()) {
              v.replaceAll(".", "x")
            } else {
              val trimLen = h.trim().min(v.length)
              s"${v.substring(0, trimLen)}..."
            }
          case _ =>
            v
        }
        val ws = " " * (paramWidth - p.name.length).max(0)
        s += s" - ${p.name}${ws}: ${processedValue}"
      }
    }
    val lines = s.result().mkString("\n")
    lines
  }

  // Customization
  def withEnv(newEnv: String, defaultEnv: String = "default"): Config = {
    Config(ConfigEnv(newEnv, defaultEnv, env.configPaths), holder)
  }

  def withConfigPaths(paths: Seq[String]): Config = {
    Config(env.withConfigPaths(paths), holder)
  }

  // Accessors to configurations
  def getAll: Seq[ConfigHolder]                 = holder.values.toSeq
  override def iterator: Iterator[ConfigHolder] = holder.values.iterator

  def getConfigChanges: Seq[ConfigChange] = {
    val b = Seq.newBuilder[ConfigChange]
    for (c <- getAll) {
      val defaultProps = PropertiesConfig.toConfigProperties(c.tpe, getDefaultValueOf(c.tpe))
      val currentProps = PropertiesConfig.toConfigProperties(c.tpe, c.value)

      for ((k, props) <- defaultProps.groupBy(_.key); defaultValue <- props;
           current    <- currentProps.filter(x => x.key == k)) {
        b += ConfigChange(c.tpe, k, defaultValue.v, current.v)
      }
    }
    b.result
  }

  private def find[A](tpe: Surface): Option[Any] = {
    holder.get(tpe).map(_.value)
  }

  def of[ConfigType: ru.TypeTag]: ConfigType = {
    val t = Surface.of[ConfigType]
    find(t) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"No config value for ${t} is found")
    }
  }

  def getOrElse[ConfigType: ru.TypeTag](default: => ConfigType): ConfigType = {
    val t = Surface.of[ConfigType]
    find(t) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        default
    }
  }
  def defaultValueOf[ConfigType: ru.TypeTag]: ConfigType = {
    val tpe = Surface.of[ConfigType]
    getDefaultValueOf(tpe).asInstanceOf[ConfigType]
  }

  def +(h: ConfigHolder): Config = Config(env, this.holder + (h.tpe -> h))
  def +(other: Config): Config = {
    Config(env, this.holder ++ other.holder)
  }
  override private[airframe] def addAsDesignOption[Config1 >: Config](other: Config1): Config1 = {
    Config(env, this.holder ++ other.asInstanceOf[Config].holder)
  }

  def register[ConfigType: ru.TypeTag](config: ConfigType): Config = {
    val tpe = Surface.of[ConfigType]
    this + ConfigHolder(tpe, config)
  }

  /**
    * Register the default value of the object as configuration
    *
    * @tparam ConfigType
    * @return
    */
  def registerDefault[ConfigType: ru.TypeTag]: Config = {
    val tpe = Surface.of[ConfigType]
    this + ConfigHolder(tpe, defaultValueOf[ConfigType])
  }

  def registerFromYaml[ConfigType: ru.TypeTag](yamlFile: String): Config = {
    val tpe = Surface.of[ConfigType]
    val config: Option[ConfigType] = loadFromYaml[ConfigType](yamlFile, onMissingFile = {
      throw new FileNotFoundException(s"${yamlFile} is not found in ${env.configPaths.mkString(":")}")
    })
    config match {
      case Some(x) =>
        this + ConfigHolder(tpe, x)
      case None =>
        throw new IllegalArgumentException(s"No configuration for ${tpe} (${env.env} or ${env.defaultEnv}) is found")
    }
  }

  private def loadFromYaml[ConfigType: ru.TypeTag](
      yamlFile: String,
      onMissingFile: => Option[ConfigType]
  ): Option[ConfigType] = {
    val tpe = Surface.of[ConfigType]
    findConfigFile(yamlFile) match {
      case None =>
        onMissingFile
      case Some(realPath) =>
        val m = loadMapOf[ConfigType](realPath)
        m.get(env.env) match {
          case Some(x) =>
            info(s"Loading ${tpe} from ${realPath}, env:${env.env}")
            Some(x)
          case None =>
            // Load default
            debug(
              s"Configuration for ${env.env} is not found in ${realPath}. Load ${env.defaultEnv} configuration instead"
            )
            m.get(env.defaultEnv).map { x =>
              info(s"Loading ${tpe} from ${realPath}, default env:${env.defaultEnv}")
              x
            }
        }
    }
  }

  def registerFromYamlOrElse[ConfigType: ru.TypeTag](yamlFile: String, defaultValue: => ConfigType): Config = {
    val tpe    = Surface.of[ConfigType]
    val config = loadFromYaml[ConfigType](yamlFile, onMissingFile = Some(defaultValue))
    this + ConfigHolder(tpe, config.get)
  }

  def overrideWith(
      props: Map[String, Any],
      onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
  ): Config = {
    val p = new Properties
    for ((k, v) <- props) {
      Option(v).map { x =>
        p.setProperty(k, x.toString)
      }
    }
    if (p.isEmpty) this else PropertiesConfig.overrideWithProperties(this, p, onUnusedProperties)
  }

  def overrideWithProperties(
      props: Properties,
      onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
  ): Config = {
    if (props.isEmpty) this else PropertiesConfig.overrideWithProperties(this, props, onUnusedProperties)
  }

  def overrideWithPropertiesFile(
      propertiesFile: String,
      onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
  ): Config = {
    findConfigFile(propertiesFile) match {
      case None =>
        throw new FileNotFoundException(s"Properties file ${propertiesFile} is not found")
      case Some(propPath) =>
        val p = new Properties()
        p.load(new StringReader(IOUtil.readAsString(propPath)))
        overrideWithProperties(p, onUnusedProperties)
    }
  }

  private def getDefaultValueOf(tpe: Surface): Any = {
    val v =
      tpe.objectFactory
        .map { x =>
          // Prepare the constructor arguments
          val args = for (p <- tpe.params) yield {
            p.getDefaultValue.getOrElse(Zero.zeroOf(p.surface))
          }
          // Create the default object of this ConfigType
          x.newInstance(args)
        }
        .getOrElse(Zero.zeroOf(tpe))

    trace(s"get default value of ${tpe} = ${v}")
    v
  }

  private def findConfigFile(name: String): Option[String] = {
    env.configPaths.map(p => new File(p, name)).find(_.exists()).map(_.getPath).orElse {
      Resource.find(name).map(x => name)
    }
  }
}
