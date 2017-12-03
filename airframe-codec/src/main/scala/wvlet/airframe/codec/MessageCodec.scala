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

import wvlet.airframe.codec.MessageCodec.ErrorCode
import wvlet.airframe.codec.MessagePackApi.{Packer, Unpacker}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

trait MessageCodec[A] extends LogSupport {
  def pack(p: Packer, v: A)
  def unpack(u: Unpacker, v: MessageHolder)
//  // TODO add specialized methods for primitive values
//  // def unpackInt(u:MessageUnpacker) : Int
//  def packToBytes(v: A): Array[Byte]
//
//  def unpackBytes(data: Array[Byte]): Option[A] = unpackBytes(data, 0, data.length)
//  def unpackBytes(data: Array[Byte], offset: Int, len: Int): Option[A]
}

class MessageCodecException[A](val errorCode: ErrorCode, val codec: MessageCodec[A], val message: String) extends Exception(message) {
  override def getMessage = s"[${errorCode}] coded:${codec} ${message}"
}

object MessageCodec {
  trait ErrorCode
  case object INVALID_DATA extends ErrorCode

  def default                            = new MessageCodecFactory(StandardCodec.standardCodec)
  def of[A: ru.TypeTag]: MessageCodec[A] = default.of[A]
}
