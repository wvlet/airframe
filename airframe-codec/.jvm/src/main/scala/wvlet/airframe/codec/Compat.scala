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
import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airframe.surface.{JavaEnumSurface, Surface}

import scala.util.Try

/**
  */
object Compat extends CompatBase {
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

  def codecOfClass(
      cl: Class[_],
      codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON
  ): Option[MessageCodec[_]] = {
    // Finding the surface using reflection
    val surface = surfaceOfClass(cl)
    codecFactory.of(surface) match {
      case o: ObjectCodecBase if o.paramCodec.isEmpty =>
        // If the codec is an ObjectCodec without any parameters,
        // it will produce empty json object ({}), so we cannot use it
        None
      case codec =>
        Some(codec)
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
