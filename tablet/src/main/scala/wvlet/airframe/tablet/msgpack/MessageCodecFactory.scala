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
package wvlet.airframe.tablet.msgpack

import wvlet.airframe.tablet.msgpack.ScalaStandardCodec.{OptionCodec, TupleCodec}
import wvlet.surface
import wvlet.surface.Surface
import wvlet.surface.reflect.ReflectTypeUtil

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class MessageCodecFactory(knownCodecs: Map[Surface, MessageCodec[_]]) {

  def withCodecs(additionalCodecs: Map[Surface, MessageCodec[_]]): MessageCodecFactory = {
    new MessageCodecFactory(knownCodecs ++ additionalCodecs)
  }

  private var cache = Map.empty[Surface, MessageCodec[_]]

  private def ofSurface(surface: Surface, seen: Set[Surface] = Set.empty): MessageCodec[_] = {
    if (knownCodecs.contains(surface)) {
      knownCodecs(surface)
    } else if (cache.contains(surface)) {
      cache(surface)
    } else if (seen.contains(surface)) {
      throw new IllegalArgumentException(s"Codec for recursive types is not supported: ${surface}")
    } else {
      val codec = if (surface.isOption) {
        // Option type
        val elementSurface = surface.typeArgs(0)
        OptionCodec(ofSurface(elementSurface, seen + surface))
      } else if (ReflectTypeUtil.isTuple(surface.rawType)) {
        // Tuple
        TupleCodec(surface.typeArgs.map(x => ofSurface(x)))
      } else {
        val codecs = for (p <- surface.params) yield {
          ofSurface(p.surface, seen + surface)
        }
        ObjectCodec(surface, codecs.toIndexedSeq)
      }
      cache += surface -> codec
      codec
    }
  }

  def of[A: ru.TypeTag]: MessageCodec[A] = ofSurface(surface.of[A]).asInstanceOf[MessageCodec[A]]
}
