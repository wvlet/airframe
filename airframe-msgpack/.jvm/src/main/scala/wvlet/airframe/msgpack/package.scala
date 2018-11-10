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
package wvlet.airframe
import java.io.{InputStream, OutputStream}

import org.msgpack.core.MessagePack
import wvlet.airframe.msgpack.impl.{BufferPackerImpl, PackerImpl, UnpackerImpl}
import wvlet.airframe.msgpack.spi.{BufferPacker, Packer, Unpacker}

/**
  *
  */
package object msgpack {

  def newBufferPacker: BufferPacker = {
    new BufferPackerImpl(MessagePack.newDefaultBufferPacker())
  }

  def newPacker(out: OutputStream): Packer = {
    new PackerImpl(MessagePack.newDefaultPacker(out))
  }

  def newUnpacker(in: InputStream): Unpacker = {
    new UnpackerImpl(MessagePack.newDefaultUnpacker(in))
  }

  def newUnpacker(msgpack: Array[Byte]): Unpacker = {
    newUnpacker(msgpack, 0, msgpack.length)
  }

  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker = {
    new UnpackerImpl(MessagePack.newDefaultUnpacker(msgpack, offset, len))
  }
}
