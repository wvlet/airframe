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
package wvlet.airframe.launcher
import org.msgpack.core.{MessagePacker, MessageUnpacker}
import wvlet.airframe.codec.{MessageCodec, MessageHolder}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}

/**
  *
  */
object ValueHolderCodec extends MessageCodec[ValueHolder[_]] {
  override def pack(p: Packer, v: ValueHolder[_]): Unit = {
    v match {
      case ValueHolder.Empty =>
        // For nested objects, we should use an empty Map to use default values
        p.packMapHeader(0)
      case ValueHolder.Node(child) => {
        p.packMapHeader(child.size)
        for ((key, x) <- child) {
          p.packString(key.toString)
          pack(p, x)
        }
      }
      case ValueHolder.Leaf(v) =>
        p.packString(v.toString)
      case ValueHolder.SeqLeaf(elems) =>
        p.packArrayHeader(elems.length)
        for (x <- elems) {
          pack(p, x)
        }
    }
  }

  override def unpack(u: Unpacker, v: MessageHolder): Unit = ???
}
