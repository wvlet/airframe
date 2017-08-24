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
package wvlet.config

import java.{util => ju}

import org.yaml.snakeyaml.Yaml
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil._
import wvlet.surface
import wvlet.surface.Surface
import wvlet.surface.reflect.ObjectBuilder

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.reflect.runtime.{universe => ru}

object YamlReader extends LogSupport {

  def load[A: ru.TypeTag](resourcePath: String, env: String): A = {
    val map = loadMapOf[A](resourcePath)
    if (!map.contains(env)) {
      throw new IllegalArgumentException(s"Env $env is not found in $resourcePath")
    }
    map(env)
  }

  def loadMapOf[A: ru.TypeTag](resourcePath: String): Map[String, A] = {
    val yaml             = loadYaml(resourcePath)
    val surface: Surface = wvlet.surface.of[A]
    val map              = ListMap.newBuilder[String, A]
    for ((k, v) <- yaml) yield {
      map += k.toString -> bind[A](surface, v.asInstanceOf[java.util.Map[AnyRef, AnyRef]])
    }
    map.result
  }

  def loadYaml(resourcePath: String): Map[AnyRef, AnyRef] = {
    new Yaml().load(readAsString(resourcePath)).asInstanceOf[ju.Map[AnyRef, AnyRef]].asScala.toMap
  }

  def loadYamlList(resourcePath: String): Seq[Map[AnyRef, AnyRef]] = {
    new Yaml().load(readAsString(resourcePath)).asInstanceOf[ju.List[ju.Map[AnyRef, AnyRef]]].asScala.map(_.asScala.toMap).toSeq
  }

  def bind[A: ru.TypeTag](prop: Map[AnyRef, AnyRef]): A = {
    bind(surface.of[A], prop.asJava).asInstanceOf[A]
  }

  def bind[A](surface: Surface, prop: java.util.Map[AnyRef, AnyRef]): A = {
    trace(s"bind ${surface}, prop:${prop.asScala}")
    val builder = ObjectBuilder(surface)
    if (prop != null) {
      for ((k, v) <- prop.asScala) {
        builder.set(k.toString, v)
      }
    }
    builder.build.asInstanceOf[A]
  }
}
