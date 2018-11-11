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
package wvlet.airframe.tablet.obj

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory, MessageHolder}
import wvlet.airframe.tablet.{Record, TabletWriter}
import wvlet.log.LogSupport
import wvlet.airframe.surface
import wvlet.airframe.surface.{Surface, Zero}

import scala.reflect.runtime.{universe => ru}

/**
  *
  */
class ObjectTabletWriter[A: ru.TypeTag](codec: Map[Surface, MessageCodec[_]] = Map.empty)
    extends TabletWriter[A]
    with LogSupport {

  private val elementCodec = MessageCodecFactory.defaultFactory.withCodecs(codec).of[A]

  private val h         = new MessageHolder
  private val s         = surface.of[A]
  private lazy val zero = Zero.zeroOf(s)

  def write(record: Record): A = {
    elementCodec.unpack(record.unpacker, h)

    val v = if (h.isNull) {
      zero
    } else {
      h.getLastValue
    }
    v.asInstanceOf[A]
  }

}
