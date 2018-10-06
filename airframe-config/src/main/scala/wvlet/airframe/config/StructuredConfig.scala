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
import org.msgpack.core.{MessagePacker, MessageUnpacker}
import org.msgpack.value.{MapValue, Value, ValueType}
import org.yaml.snakeyaml.Yaml
import wvlet.airframe.codec.{MessageCodec, MessageHolder}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil.readAsString

object ConfigTarget {
  // Standard tag names
  val Site    = "site"
  val Cloud   = "cloud"
  val Region  = "region"
  val Stage   = "stage"
  val Cluster = "cluster"

  val All = "*"

  val defaultHierarchy = Seq(Site, Cloud, Region, Stage, Cluster)
}

case class ConfigTag(tag: String, value: String) {
  override def toString: String = s"${tag}:${value}"
}

case class Tags(tags: List[ConfigTag]) {
  override def toString: String = s"${tags.mkString(",")}"

  def isEmpty: Boolean       = tags.isEmpty
  def nonEmpty: Boolean      = !isEmpty
  def ::(t: ConfigTag): Tags = Tags(t :: tags)

  def matchesWith(targetTags: Tags): Boolean = {
    targetTags.tags.forall { p =>
      (p.tag, targetTags.tags.map(_.tag).find(_ == p.tag)) match {
        case ("*", _)       => true
        case (_, Some("*")) => true
        case (_, None)      => true
        case (t1, Some(t2)) => t1 == t2
      }
    }
  }
}
object Tags {
  val empty                          = Tags(List.empty)
  def apply(s: Seq[ConfigTag]): Tags = Tags(s.toList)
}

case class ConfigValue(path: Seq[String], value: Value, tags: Tags) {
  override def toString: String = {
    val s = new StringBuilder
    s.append(s"${path.mkString(".")}")
    if (tags.nonEmpty) {
      s.append(s"[${tags}]")
    }
    s.append(":")
    s.append(value)
    s.toString()
  }
}

case class ConfigMatch(matches: Seq[ConfigValue])

object StructuredConfig extends LogSupport {

  def loadYaml(resourcePath: String) = {
    val m       = YamlReader.loadYaml(resourcePath)
    val msgPack = YamlReader.toMsgPack(m)
    StructuredConfigCodec.unpackBytes(msgPack).get
  }

}

/**
  *
  */
case class StructuredConfig(configList: Seq[ConfigValue], hierarchy: Seq[String] = ConfigTarget.defaultHierarchy)
    extends LogSupport {

  def get(tags: Tags): Option[String] = {
    info(s"get: ${tags}")
    val matches = configList.filter(x => x.tags.matchesWith(tags))
    info(s"matches:\n${matches.mkString("\n")}")
    None
  }

}

object StructuredConfigCodec extends MessageCodec[StructuredConfig] with LogSupport {
  override def pack(p: MessagePacker, v: StructuredConfig): Unit = {
    // TODO
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val f  = u.getNextFormat
    val vt = f.getValueType
    vt match {
      case ValueType.MAP =>
        val m          = u.unpackValue()
        val configList = parse(Seq.empty, m.asMapValue(), Tags.empty)
        val sc         = StructuredConfig(configList)
        v.setObject(sc)
      case ohter =>
        v.setIncompatibleFormatException(this, s"Unexpected value type: ${vt}")
    }
  }

  /**
    * Recursively parse MapValue. If the key name is xxxx[yyyy] format, ConfigTag(tag:xxxx, value:yyyy) will be added to the scope
    * @param path
    * @param m
    * @param tags
    * @return
    */
  private def parse(path: Seq[String], m: MapValue, tags: Tags): Seq[ConfigValue] = {
    import scala.collection.JavaConverters._
    val b = Seq.newBuilder[ConfigValue]
    for ((k, v) <- m.map().asScala) {
      def parseValue(nextPath: Seq[String], nextTags: Tags): Unit = {
        if (v.isMapValue) {
          b ++= parse(nextPath, v.asMapValue(), nextTags)
        } else {
          b += ConfigValue(nextPath, v, nextTags)
        }
      }

      val key = k.toString
      findTag(key) match {
        case Some(c) =>
          // Add a new tag
          parseValue(path, c :: tags)
        case None =>
          // regular path
          if (tags.isEmpty && key == "default") {
            // global default values (empty tags)
            parseValue(path, Tags.empty)
          } else {
            // Nested path
            parseValue(path :+ key, tags)
          }
      }
    }
    b.result()
  }

  private val tagPattern = """(.+)\[(.+)\]$""".r

  private def findTag(keyName: String): Option[ConfigTag] = {
    tagPattern.findFirstMatchIn(keyName) match {
      case Some(x) =>
        Some(ConfigTag(x.group(1), x.group(2)))
      case None =>
        None
    }
  }

}
