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
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}
import wvlet.airframe.surface.Surface

/**
  * For generating codec for recursive types.
  *
  * For example, if type X has a recursion like X(name:String, child:Option[X]), LazyCodec will be used to generate a
  * codec instance as MessageCodec[X](StringCodec, OptionCodec(LazyCodec[X])).
  */
case class LazyCodec[A](surface: Surface, codecFactory: MessageCodecFactory) extends MessageCodec[A] {
  private lazy val ref: MessageCodec[A] = codecFactory.ofSurface(surface).asInstanceOf[MessageCodec[A]]

  override def pack(p: Packer, v: A): Unit = {
    ref.pack(p, v)
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    ref.unpack(u, v)
  }
}
