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

import wvlet.airframe.DesignOptions
import wvlet.airframe.config.PropertiesConfig.ConfigKey
import wvlet.airframe.config.YamlReader.loadMapOf
import wvlet.airframe.surface.{Surface, Zero}
import wvlet.log.LogSupport
import wvlet.log.io.{IOUtil, Resource}

import scala.collection.immutable.ListMap

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

  def REPORT_UNUSED_PROPERTIES: Properties => Unit = { (unused: Properties) =>
    warn(s"There are unused properties: ${unused}")
  }
  def REPORT_ERROR_FOR_UNUSED_PROPERTIES: Properties => Unit = { (unused: Properties) =>
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
    extends ConfigCompat
    with Iterable[ConfigHolder]
    with DesignOptions.AdditiveDesignOption[Config]
    with LogSupport {
  override def toString: String = printConfig

  import wvlet.airframe.surface.reflect._

  /**
    * Create a map representation of this config for display purpose. Parameters with @secret annotation will be hidden.
    */
  def toPrintableMap: Map[String, Any] = {
    def traverse(s: Surface, v: Any, secret: Option[wvlet.airframe.surface.secret]): Any = {
      if (s.params.isEmpty) {
        val value = v match {
          case null => ""
          case Some(x) if x != null =>
            x.toString // unwrap Option
          case _ =>
            v.toString
        }
        // Hide secrete values
        secret match {
          case Some(h) if h.mask() =>
            value.replaceAll(".", "x")
          case Some(h) =>
            val trimLen = h.trim().min(value.length)
            s"${value.substring(0, trimLen)}..."
          case _ =>
            value
        }
      } else {
        // Use ListMap to preserve the parameter order
        val m = ListMap.newBuilder[String, Any]
        for (p <- s.params) {
          m += p.name -> traverse(p.surface, p.get(v), p.findAnnotationOf[wvlet.airframe.surface.secret])
        }
        m.result()
      }
    }

    val m = ListMap.newBuilder[String, Any]
    for (c <- getAll) yield {
      m += c.tpe.name -> traverse(c.tpe, c.value, secret = None)
    }
    m.result()
  }

  def printConfig: String = {
    val s = Seq.newBuilder[String]
    s += "Configurations:"

    def traverse(m: Map[String, Any], indent: Int): Unit = {
      val paramWidth = m.keys.map(_.length).max
      for ((paramName, v) <- m) {
        val prefix = if (indent == 0) {
          s += s"[${paramName}]"
          ""
        } else {
          val ws = " " * (indent * 2)
          s"${ws}- ${paramName}"
        }
        val gap = " " * (paramWidth - paramName.length).max(0)
        v match {
          case mm: Map[String @unchecked, Any @unchecked] =>
            if (indent > 0) {
              s += prefix
            }
            traverse(mm, indent + 1)
          case _ =>
            s += s"${prefix}${gap}: ${v}"
        }
      }
    }

    val m = toPrintableMap
    traverse(m, 0)

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

      for (
        (k, props) <- defaultProps.groupBy(_.key); defaultValue <- props;
        current    <- currentProps.filter(x => x.key == k)
      ) {
        b += ConfigChange(c.tpe, k, defaultValue.v, current.v)
      }
    }
    b.result()
  }

  protected def find[A](tpe: Surface): Option[Any] = {
    holder.get(tpe).map(_.value)
  }

  def ofSurface[ConfigType](surface: Surface): ConfigType = {
    find(surface) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        throw new IllegalArgumentException(s"No config value for ${surface} is found")
    }
  }

  protected def getOrElseOfSurface[ConfigType](surface: Surface, default: => ConfigType): ConfigType = {
    find(surface) match {
      case Some(x) =>
        x.asInstanceOf[ConfigType]
      case None =>
        default
    }
  }

  protected def defaultValueOfSurface[ConfigType](surface: Surface): ConfigType = {
    getDefaultValueOf(surface).asInstanceOf[ConfigType]
  }

  def +(h: ConfigHolder): Config = Config(env, this.holder + (h.tpe -> h))
  def +(other: Config): Config = {
    Config(env, this.holder ++ other.holder)
  }
  override private[airframe] def addAsDesignOption[Config1 >: Config](other: Config1): Config1 = {
    Config(env, this.holder ++ other.asInstanceOf[Config].holder)
  }

  def registerOfSurface[ConfigType](surface: Surface, config: ConfigType): Config = {
    this + ConfigHolder(surface, config)
  }

  protected def registerDefaultOfSurface[ConfigType](surface: Surface): Config = {
    this + ConfigHolder(surface, defaultValueOfSurface[ConfigType](surface))
  }

  def registerFromYaml[ConfigType](surface: Surface, yamlFile: String): Config = {
    val config: Option[ConfigType] = loadFromYaml[ConfigType](
      surface,
      yamlFile,
      onMissingFile = {
        throw new FileNotFoundException(s"${yamlFile} is not found in ${env.configPaths.mkString(":")}")
      }
    )
    config match {
      case Some(x) =>
        this + ConfigHolder(surface, x)
      case None =>
        throw new IllegalArgumentException(
          s"No configuration for ${surface} (${env.env} or ${env.defaultEnv}) is found"
        )
    }
  }

  private def loadFromYaml[ConfigType](
      surface: Surface,
      yamlFile: String,
      onMissingFile: => Option[ConfigType]
  ): Option[ConfigType] = {
    findConfigFile(yamlFile) match {
      case None =>
        onMissingFile
      case Some(realPath) =>
        val m = loadMapOf[ConfigType](surface, realPath)
        m.get(env.env) match {
          case Some(x) =>
            info(s"Loading ${surface} from ${realPath}, env:${env.env}")
            Some(x)
          case None =>
            // Load default
            debug(
              s"Configuration for ${env.env} is not found in ${realPath}. Load ${env.defaultEnv} configuration instead"
            )
            m.get(env.defaultEnv).map { x =>
              info(s"Loading ${surface} from ${realPath}, default env:${env.defaultEnv}")
              x
            }
        }
    }
  }

  def registerFromYamlOrElse[ConfigType](
      surface: Surface,
      yamlFile: String,
      defaultValue: => ConfigType
  ): Config = {
    val config = loadFromYaml[ConfigType](surface, yamlFile, onMissingFile = Some(defaultValue))
    this + ConfigHolder(surface, config.get)
  }

  def overrideWith(
      props: Map[String, Any],
      onUnusedProperties: Properties => Unit = REPORT_UNUSED_PROPERTIES
  ): Config = {
    val p = new Properties
    for ((k, v) <- props) {
      Option(v).map { x => p.setProperty(k, x.toString) }
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
