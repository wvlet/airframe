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
package wvlet.airframe.tablet.text

import org.msgpack.core.MessageUnpacker
import wvlet.airframe.msgpack.spi.Unpacker
import wvlet.airframe.tablet.{Record, TabletWriter}
import wvlet.log.LogSupport

/**
  *
  */
object JSONObjectPrinter extends TabletWriter[String] with LogSupport {

  def read(unpacker: Unpacker): String = {
    if (!unpacker.hasNext) {
      "{}"
    } else {
      unpacker.unpackValue.toJson
    }
  }

  override def write(record: Record): String = {
    read(record.unpacker)
  }
}
