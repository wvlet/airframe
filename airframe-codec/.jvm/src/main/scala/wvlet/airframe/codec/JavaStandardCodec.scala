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
package wvlet.airframe.codec
import java.io.File

import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.{CName, Surface}
import wvlet.log.LogSupport

import scala.util.{Success, Try}

/**
  *
  */
object JavaStandardCodec {
  val javaStandardCodecs: Map[Surface, MessageCodec[_]] = Map(
    Surface.of[File] -> FileCodec
  )

  object FileCodec extends MessageCodec[File] {
    override def pack(p: Packer, v: File): Unit = {
      p.packString(v.getPath)
    }
    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val path = u.unpackString
      v.setObject(new File(path))
    }
  }

  case class EnumCodec[A](enumType: Class[A]) extends MessageCodec[A] with LogSupport {
    private lazy val enumTable: Map[String, A] = {
      // Canonical Enum names -> Enum[A]
      Try {
        val m      = enumType.getDeclaredMethod("values")
        val values = m.invoke(null).asInstanceOf[Array[A]]
        values.map(x => CName.toCanonicalName(x.toString) -> x).toMap
      }.getOrElse {
        Map.empty
      }
    }

    override def pack(p: Packer, v: A): Unit = {
      p.packString(v.asInstanceOf[Enum[_]].name())
    }

    override def unpack(u: Unpacker, v: MessageContext): Unit = {
      val name = u.unpackString
      enumTable.get(CName.toCanonicalName(name)) match {
        case Some(enum) => v.setObject(enum)
        case _ =>
          v.setIncompatibleFormatException(this, s"${name} is not a value of ${enumType}")
      }
    }
  }
}
