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

import wvlet.airframe.surface.Surface

import scala.language.experimental.macros
import scala.reflect.runtime.universe._

object ScalaCompat {

  trait MessageCodecBase {
    def of[A]: MessageCodec[A] = macro CodecMacros.codecOf[A]

    def fromJson[A: TypeTag](json: String): A = {
      MessageCodecFactory.defaultFactory.fromJson[A](json)
    }

    def toJson[A: TypeTag](obj: A): String = {
      MessageCodecFactory.defaultFactory.toJson[A](obj)
    }
  }

  trait MessageCodecFactoryBase { self: MessageCodecFactory =>
    def of[A: TypeTag]: MessageCodec[A]       = ofSurface(Surface.of[A]).asInstanceOf[MessageCodec[A]]
    def fromJson[A: TypeTag](json: String): A = {
      val codec = of[A]
      codec.fromJson(json)
    }

    def toJson[A: TypeTag](obj: A): String = {
      val codec = of[A]
      codec.toJson(obj)
    }
  }

}
