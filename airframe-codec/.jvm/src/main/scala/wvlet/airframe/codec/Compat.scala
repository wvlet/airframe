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
import wvlet.airframe.codec.JavaStandardCodec.EnumCodec
import wvlet.airframe.surface.reflect.ReflectTypeUtil
import wvlet.airframe.surface.{EnumSurface, Surface}

import scala.reflect.runtime.{universe => ru}

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
      case EnumSurface(cl) =>
        EnumCodec(cl)
      case s if ReflectTypeUtil.hasStringUnapplyConstructor(s) =>
        new StringUnapplyCodec(s)
    }
  }

  def platformSpecificCodecs: Map[Surface, MessageCodec[_]] =
    JavaTimeCodec.javaTimeCodecs ++ JavaStandardCodec.javaStandardCodecs

  def codecOf[A: ru.TypeTag]: MessageCodec[A] = MessageCodecFactory.defaultFactory.of[A]
}
