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
import java.time.Instant
import java.util.UUID

import wvlet.airframe.codec.JavaStandardCodec.JavaEnumCodec
import wvlet.airframe.metrics.TimeParser
import wvlet.airframe.surface.reflect.{ReflectSurfaceFactory, ReflectTypeUtil}
import wvlet.airframe.surface.{GenericSurface, JavaEnumSurface, Surface}

import scala.reflect.runtime.{universe => ru}
import scala.util.Try

/**
  *
  */
object Compat {
  def messageCodecFinder: MessageCodecFinder = {
    MessageCodecFinder.defaultMessageCodecFinder orElse
      JVMMessageCodecFinder
  }

  object JVMMessageCodecFinder extends MessageCodecFinder {
    override def findCodec(
        factory: MessageCodecFactory,
        seenSet: Set[Surface]
    ): PartialFunction[Surface, MessageCodec[_]] = {
      case JavaEnumSurface(cl) =>
        JavaEnumCodec(cl)
      case s if ReflectTypeUtil.hasStringUnapplyConstructor(s) =>
        new StringUnapplyCodec(s)
    }
  }

  def platformSpecificCodecs: Map[Surface, MessageCodec[_]] =
    JavaTimeCodec.javaTimeCodecs ++ JavaStandardCodec.javaStandardCodecs

  def codecOf[A: ru.TypeTag]: MessageCodec[A] = MessageCodecFactory.defaultFactory.of[A]
  def codecOfClass(
      cl: Class[_],
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ): Option[MessageCodec[_]] = {
    // Finding the surface using reflection
    ReflectSurfaceFactory.ofClass(cl) match {
      case g: GenericSurface if g.params == 0 =>
        // If this type has no parameters, do not use ObjectCodec for JSON output
        None
      case surface =>
        // Otherwise, use the regular codec finder from Surface
        Some(codecFactory.of(surface))
    }
  }

  private[codec] def parseInstant(s: String): Option[Instant] = {
    Try(Instant.parse(s)).toOption
      .orElse(TimeParser.parseAtLocalTimeZone(s).map(_.toInstant))
  }

  def readUUIDFromBytes(data: Array[Byte]): UUID = {
    UUID.nameUUIDFromBytes(data)
  }
}
