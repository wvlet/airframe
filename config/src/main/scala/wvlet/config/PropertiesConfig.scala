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

import java.util.Properties

import wvlet.config.Config.CanonicalNameFormatter
import wvlet.log.LogSupport
import wvlet.surface.{Surface, TaggedSurface}
import wvlet.surface.reflect.ObjectBuilder

import scala.util.{Failure, Success, Try}

/**
  * Helper class to overwrite config objects using Java Properties
  */
object PropertiesConfig extends LogSupport {

  case class Prefix(prefix:String, tag: Option[String]) {
    override def toString = tag match {
      case Some(t) => s"${prefix}@${t}"
      case None => prefix
    }
  }
  case class ConfigKey(prefix: Prefix, param: String) {
    override def toString = s"${prefix}.${param}"
  }
  case class ConfigProperty(key: ConfigKey, v: Any)

  private[config] def extractPrefix(t: Surface): Prefix = {
    def canonicalize(s: String): String = {
      val name = s.replaceAll("Config$", "")
      CanonicalNameFormatter.format(name)
    }
    t match {
      case TaggedSurface(base, tag) =>
        Prefix(canonicalize(base.name), Some(CanonicalNameFormatter.format(tag.name)))
      case _ =>
        Prefix(canonicalize(t.name), None)
    }
  }

  private[config] def configKeyOf(propKey: String): ConfigKey = {
    val c = propKey.split("\\.")
    c.length match {
      case l if l >= 2 =>
        val prefixSplit = c(0).split("@+")
        if(prefixSplit.length > 1) {
          val param = CanonicalNameFormatter.format(c(1).mkString)
          ConfigKey(Prefix(prefixSplit(0), Some(prefixSplit(1))), param)
        }
        else {
          val prefix = c(0)
          val param = CanonicalNameFormatter.format(c(1))
          ConfigKey(Prefix(prefix, None), param)
        }
      case other =>
        throw new IllegalArgumentException(s"${propKey} should have [prefix](@[tag])?.[param] format")
    }
  }

  private[config] def toConfigProperties(tpe:Surface, config: Any): Seq[ConfigProperty] = {
    val prefix = extractPrefix(tpe)
    val b = Seq.newBuilder[ConfigProperty]
    for (p <- tpe.params) {
      val key = ConfigKey(prefix, CanonicalNameFormatter.format(p.name))
      Try(p.get(config)) match {
        case Success(v) =>
          b += ConfigProperty(key, v)
        case Failure(e) =>
          warn(s"Failed to read parameter ${p} from ${config}")
      }
    }
    b.result()
  }

  def overrideWithProperties(config:Config, props: Properties, onUnusedProperties: Properties => Unit): Config = {
    val overrides : Seq[ConfigProperty] = {
      import scala.collection.JavaConverters._
      val b = Seq.newBuilder[ConfigProperty]
      for ((k, v) <- props.asScala) yield {
        val key = configKeyOf(k)
        val p = ConfigProperty(key, v)
        b += p
      }
      b.result
    }
    val unusedProperties = Seq.newBuilder[ConfigProperty]

    // Check properties for unknown config objects
    val knownPrefixes = config.map(x => extractPrefix(x.tpe)).toSet

    unusedProperties ++= overrides.filterNot(x => knownPrefixes.contains(x.key.prefix))

    val newConfigs = for (ConfigHolder(tpe, value) <- config) yield {
      val configBuilder = ObjectBuilder.fromObject(tpe, value)
      val prefix = extractPrefix(tpe)
      val schema = tpe

      val (overrideParams, unused) =
        overrides
        .filter(_.key.prefix == prefix)
        .partition(p => schema.params.exists(_.name == p.key.param))

      unusedProperties ++= unused
      for (p <- overrideParams) {
        trace(s"override: ${p}")
        configBuilder.set(p.key.param, p.v)
      }
      tpe -> ConfigHolder(tpe, configBuilder.build)
    }

    val unused = unusedProperties.result
    if(unused.size > 0) {
      val unusedProps = new Properties
      unused.map(p => unusedProps.put(p.key.toString, p.v.asInstanceOf[AnyRef]))
      onUnusedProperties(unusedProps)
    }

    Config(config.env, newConfigs.toMap)
  }

}
