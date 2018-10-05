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

import java.{lang => jl, util => ju}

import org.msgpack.core.{MessagePack, MessagePacker}
import org.yaml.snakeyaml.Yaml
import wvlet.airframe.tablet.MessagePackRecord
import wvlet.airframe.tablet.obj.ObjectTabletWriter
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil._
import wvlet.airframe.surface
import wvlet.airframe.surface.Surface
import wvlet.airframe.surface.reflect.ReflectTypeUtil

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
    val yaml = loadYaml(resourcePath)
    trace(s"yaml data: ${yaml.mkString(", ")}")
    val surface: Surface = wvlet.airframe.surface.of[A]
    val map              = ListMap.newBuilder[String, A]
    for ((k, v) <- yaml) yield {
      map += k.toString -> bindMap[A](surface, v.asInstanceOf[ju.Map[AnyRef, AnyRef]].asScala.toMap)
    }
    map.result
  }

  def loadYaml(resourcePath: String): Map[AnyRef, AnyRef] = {
    new Yaml().load(readAsString(resourcePath)).asInstanceOf[ju.Map[AnyRef, AnyRef]].asScala.toMap
  }

  def loadYamlList(resourcePath: String): Seq[Map[AnyRef, AnyRef]] = {
    new Yaml()
      .load(readAsString(resourcePath)).asInstanceOf[ju.List[ju.Map[AnyRef, AnyRef]]].asScala.map(_.asScala.toMap).toSeq
  }

  def bind[A: ru.TypeTag](prop: Map[AnyRef, AnyRef]): A = {
    bindMap(surface.of[A], prop).asInstanceOf[A]
  }

  def bindMap[A: ru.TypeTag](surface: Surface, prop: Map[AnyRef, AnyRef]): A = {
    val yamlMsgpack = toMsgPack(prop)
    val w           = new ObjectTabletWriter[A]()
    val result      = w.write(new MessagePackRecord(yamlMsgpack))
    trace(result)
    result
  }

  def toMsgPack(prop: Map[AnyRef, AnyRef]): Array[Byte] = {
    new YamlReader(prop).toMsgPack
  }
}

/**
  * Converting Yaml (java Map value generated by SnakeYaml) to MessagePack Map value
  * @param map
  */
class YamlReader(map: Map[AnyRef, AnyRef]) extends LogSupport {

  def toMsgPack: Array[Byte] = {
    val packer = MessagePack.newDefaultBufferPacker()
    packer.packMapHeader(map.size)
    for ((k, v) <- map) yield {
      pack(packer, k)
      pack(packer, v)
    }
    packer.toByteArray
  }

  private def pack(packer: MessagePacker, v: Any): MessagePacker = {
    if (v == null) {
      packer.packNil()
    } else {
      trace(s"pack: ${v} (${v.getClass.getName})")
      v match {
        case s: String =>
          packer.packString(s)
        case i: jl.Integer =>
          packer.packInt(i)
        case l: jl.Long =>
          packer.packLong(l)
        case f: jl.Float =>
          packer.packFloat(f)
        case d: jl.Double =>
          packer.packDouble(d)
        case b: jl.Boolean =>
          packer.packBoolean(b)
        case s: jl.Short =>
          packer.packShort(s)
        case b: jl.Byte =>
          packer.packByte(b)
        case c: jl.Character =>
          packer.packInt(c.toInt)
        case a if ReflectTypeUtil.isArray(a.getClass) =>
          val ar = a.asInstanceOf[Array[_]]
          trace(s"pack array (${ar.size})")
          packer.packArrayHeader(ar.length)
          for (e <- ar) {
            pack(packer, e)
          }
        case m if ReflectTypeUtil.isJavaMap(m.getClass) =>
          val mp = m.asInstanceOf[java.util.Map[AnyRef, AnyRef]].asScala
          trace(s"pack map (${mp.size})")
          packer.packMapHeader(mp.size)
          for ((k, v) <- mp) {
            pack(packer, k)
            pack(packer, v)
          }
        case c if ReflectTypeUtil.isJavaColleciton(c.getClass) =>
          val cl = c.asInstanceOf[java.util.Collection[_]].asScala
          trace(s"pack collection (${cl.size})")
          packer.packArrayHeader(cl.size)
          for (e <- cl) {
            pack(packer, e)
          }
        case other =>
          packer.packString(v.toString)
      }
    }
    packer
  }

}
