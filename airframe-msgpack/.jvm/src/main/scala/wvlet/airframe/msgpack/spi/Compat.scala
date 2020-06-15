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

import org.msgpack.{core => mj}
import wvlet.airframe.msgpack.impl.{
  BufferPackerImpl,
  PackerImpl,
  PureScalaBufferPacker,
  PureScalaBufferUnpacker,
  UnpackerImpl
}
import wvlet.airframe.msgpack.io.ByteArrayBuffer

/**
  * For compatibility with Scala, Scala.js
  */
object Compat {
  def isScalaJS = false

  def floatToIntBits(v: Float): Int     = java.lang.Float.floatToRawIntBits(v)
  def doubleToLongBits(v: Double): Long = java.lang.Double.doubleToRawLongBits(v)

  def newBufferPacker: BufferPacker = {
    new PureScalaBufferPacker
    //new BufferPackerImpl(mj.MessagePack.newDefaultBufferPacker())
  }

  def newPacker(out: OutputStream): Packer = {
    // TODO: Use pure-scala packer
    // new PureScalaBufferPacker
    new PackerImpl(mj.MessagePack.newDefaultPacker(out))
  }

  def newUnpacker(in: InputStream): Unpacker = {
    new UnpackerImpl(mj.MessagePack.newDefaultUnpacker(in))
  }

  def newUnpacker(msgpack: Array[Byte]): Unpacker = {
    newUnpacker(msgpack, 0, msgpack.length)
  }

  def newUnpacker(msgpack: Array[Byte], offset: Int, len: Int): Unpacker = {
    //new UnpackerImpl(mj.MessagePack.newDefaultUnpacker(msgpack, offset, len))
    // Use pure-scala unpacker
    new PureScalaBufferUnpacker(ByteArrayBuffer.fromArray(msgpack, offset, len))
  }
}
