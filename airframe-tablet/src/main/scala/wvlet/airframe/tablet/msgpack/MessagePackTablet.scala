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

import java.io.{FileInputStream, FileOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import wvlet.airframe.msgpack
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.tablet.{Record, ShallowMessagePackRecord, TabletReader, TabletWriter}
import wvlet.log.LogSupport

/**
  *
  */
object MessagePackTablet {

  def msgpackGzReader(file: String): TabletReader = {
    new MessagePackTabletReader(msgpack.newUnpacker(new GZIPInputStream(new FileInputStream(file))))
  }

  def msgpackGzWriter(file: String): TabletWriter[Unit] = {
    new MessagePackTabletWriter(msgpack.newPacker(new GZIPOutputStream(new FileOutputStream(file))))
  }

}

/**
  *
  */
class MessagePackTabletReader(unpacker: Unpacker) extends TabletReader with LogSupport {

  private var readRows = 0

  private def readNext: Option[Record] = {
    if (!unpacker.hasNext) {
      None
    } else {
      val f = unpacker.getNextFormat
      readRows += 1
      Some(ShallowMessagePackRecord(unpacker))
    }
  }

  override def read: Option[Record] = {
    readNext
  }
}

/**
  *
  */
class MessagePackTabletWriter(packer: Packer) extends TabletWriter[Unit] {
  def write(r: Record): Unit = {
    r.pack(packer)
  }

  override def close(): Unit = {
    packer.close
  }
}
