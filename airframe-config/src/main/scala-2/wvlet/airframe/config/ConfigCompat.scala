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

import wvlet.airframe.{Design, SourceCode}
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.{universe => ru}

trait ConfigPackageCompat { self: ConfigurableDesign =>
  def bindConfig[A: ru.TypeTag](config: A)(implicit sourceCode: SourceCode): Design = {
    bindConfigInternal[A](Surface.of[A], config)(sourceCode)
  }

  def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String)(implicit sourceCode: SourceCode): Design = {
    bindConfigFromYamlInternal[A](Surface.of[A], yamlFile)(sourceCode)
  }
  def bindConfigFromYaml[A: ru.TypeTag](yamlFile: String, defaultValue: => A)(implicit
      sourceCode: SourceCode
  ): Design = {
    bindConfigFromYamlInternal[A](Surface.of[A], yamlFile, defaultValue)(sourceCode)
  }
}

trait ConfigCompat {
  self: Config =>

  def of[ConfigType: ru.TypeTag]: ConfigType = {
    self.ofSurface[ConfigType](Surface.of[ConfigType])
  }

  def getOrElse[ConfigType: ru.TypeTag](default: => ConfigType): ConfigType = {
    self.getOrElseOfSurface[ConfigType](Surface.of[ConfigType], default)
  }

  def defaultValueOf[ConfigType: ru.TypeTag]: ConfigType = {
    self.defaultValueOfSurface[ConfigType](Surface.of[ConfigType])
  }

  def register[ConfigType: ru.TypeTag](config: ConfigType): Config = {
    self.registerOfSurface[ConfigType](Surface.of[ConfigType], config)
  }

  /**
    * Register the default value of the object as configuration
    *
    * @tparam ConfigType
    * @return
    */
  def registerDefault[ConfigType: ru.TypeTag]: Config = {
    self.registerDefaultOfSurface[ConfigType](Surface.of[ConfigType])
  }

  def registerFromYaml[ConfigType: ru.TypeTag](yamlFile: String): Config = {
    self.registerFromYaml[ConfigType](Surface.of[ConfigType], yamlFile)
  }

  def registerFromYamlOrElse[ConfigType: ru.TypeTag](yamlFile: String, defaultValue: => ConfigType): Config = {
    self.registerFromYamlOrElse[ConfigType](Surface.of[ConfigType], yamlFile, defaultValue)
  }
}

trait YamlReaderCompat {
  def load[A: ru.TypeTag](resourcePath: String, env: String): A = {
    YamlReader.load[A](Surface.of[A], resourcePath, env)
  }

  def loadMapOf[A: ru.TypeTag](resourcePath: String): Map[String, A] = {
    YamlReader.loadMapOf[A](Surface.of[A], resourcePath)
  }

  def bind[A: ru.TypeTag](prop: Map[AnyRef, AnyRef]): A = {
    YamlReader.bind[A](Surface.of[A], prop)
  }
}
