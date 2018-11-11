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

import java.io.{File, PrintWriter, StringWriter}

import wvlet.airframe.msgpack.spi._
import wvlet.airframe.surface
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.util.{Success, Try}

/**
  * Standard codec collection
  */
object StandardCodec {

  val javaClassCodec = Map(
    surface.of[File]      -> FileCodec,
    surface.of[Throwable] -> ThrowableCodec,
    surface.of[Exception] -> ThrowableCodec
  )

  val standardCodec
    : Map[Surface, MessageCodec[_]] = PrimitiveCodec.primitiveCodec ++ PrimitiveCodec.primitiveArrayCodec ++ javaClassCodec

  object ThrowableCodec extends MessageCodec[Throwable] {
    override def pack(p: Packer, v: Throwable): Unit = {
      p.packMapHeader(4)
      // param 1
      p.packString("type")
      p.packString(v.getClass.getName)

      // param 2
      p.packString("message")
      val msg = v.getMessage
      if (msg == null) {
        p.packNil
      } else {
        p.packString(msg)
      }

      // param 3
      val s = new StringWriter
      val w = new PrintWriter(s)
      v.printStackTrace(w)
      w.flush()
      w.close()
      p.packString("stackTrace")
      p.packString(s.toString)

      // param 4
      p.packString("cause")
      val cause = v.getCause
      if (cause == null || cause == v) {
        p.packNil
      } else {
        ThrowableCodec.pack(p, cause)
      }
    }
    // We do not support deserialization of generic Throwable classes
    override def unpack(u: Unpacker, v: MessageHolder): Unit = ???
  }

  object FileCodec extends MessageCodec[File] {
    override def pack(p: Packer, v: File): Unit = {
      p.packString(v.getPath)
    }
    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val path = u.unpackString
      v.setObject(new File(path))
    }
  }

  case class EnumCodec[A](enumType: Class[A]) extends MessageCodec[A] with LogSupport {
    private val enumValueOfMethod = classOf[Enum[_]].getDeclaredMethod("valueOf", classOf[Class[_]], classOf[String])

    override def pack(p: Packer, v: A): Unit = {
      p.packString(v.asInstanceOf[Enum[_]].name())
    }

    override def unpack(u: Unpacker, v: MessageHolder): Unit = {
      val name = u.unpackString

      Try(enumValueOfMethod.invoke(null, enumType, name)) match {
        case Success(enum) => v.setObject(enum)
        case _ =>
          v.setIncompatibleFormatException(this, s"${name} is not a value of ${enumType}")
      }
    }
  }

}
