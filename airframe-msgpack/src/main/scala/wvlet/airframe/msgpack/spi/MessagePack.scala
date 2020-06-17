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
package wvlet.airframe.msgpack.spi
import java.io.{InputStream, OutputStream}

import wvlet.airframe.json.{JSON, JSONScanner, JSONSource}
import wvlet.airframe.msgpack.json.{NestedMessagePackBuilder, StreamMessagePackBuilder}

/**
  */
object MessagePack {
  def newBufferPacker: BufferPacker                                      = Compat.newBufferPacker
  def newPacker(out: OutputStream): Packer                               = Compat.newPacker(out)
  def newUnpacker(in: InputStream): Unpacker                             = Compat.newUnpacker(in)
  def newUnpacker(msgpack: Array[Byte]): Unpacker                        = Compat.newUnpacker(msgpack)
  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker = Compat.newUnpacker(msgpack, offset, len)

  def fromJSON(json: String): MsgPack           = fromJSON(JSONSource.fromString(json))
  def fromJSON(jsonBytes: Array[Byte]): MsgPack = fromJSON(JSONSource.fromBytes(jsonBytes))
  def fromJSON(json: JSONSource): MsgPack = {
    val context = new StreamMessagePackBuilder()
    JSONScanner.scanAny(json, context)
    context.result
  }
}
