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

  def matchesWith(targetTags: List[ConfigTag]): Boolean = {
    targetTags.forall { p =>
      (p.tag, targetTags.map(_.tag).find(_ == p.tag)) match {
        case ("*", _)       => true
        case (_, Some("*")) => true
        case (_, None)      => true
        case (t1, Some(t2)) => t1 == t2
      }
    }
  }
}
object Tags {
  val empty = Tags(List.empty)
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
    StructuredConfigCodec.unpackBytes(msgPack)
  }

}

/**
  *
  */
case class StructuredConfig(configList: Seq[ConfigValue], hierarchy: Seq[String] = ConfigTarget.defaultHierarchy) {

  def add(v: ConfigValue): StructuredConfig = {
    StructuredConfig(v +: configList, hierarchy)
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
        val m = u.unpackValue()
        info(m)
        val sc = parse(Seq.empty, m.asMapValue(), Tags.empty)
        v.setObject(sc)
      case ohter =>
        v.setIncompatibleFormatException(this, s"Unexpected value type: ${vt}")
    }
  }

  private def parse(path: Seq[String], m: MapValue, tags: Tags): Seq[ConfigValue] = {
    import scala.collection.JavaConverters._
    val b = Seq.newBuilder[ConfigValue]
    for ((k, v) <- m.map().asScala) {
      val key = k.toString
      findTag(key) match {
        case Some(c) => // tagged key
          if (v.isMapValue) {
            b ++= parse(path, v.asMapValue(), c :: tags)
          } else {
            b += ConfigValue(path, v, c :: tags)
          }
        case None => // regular path
          val currentTag = if (tags.isEmpty && key == "default") {
            Tags.empty
          } else {
            tags
          }
          // global default values
          if (v.isMapValue) {
            b ++= parse(path :+ key, v.asMapValue(), currentTag)
          } else {
            b += ConfigValue(path :+ key, v, currentTag)
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
