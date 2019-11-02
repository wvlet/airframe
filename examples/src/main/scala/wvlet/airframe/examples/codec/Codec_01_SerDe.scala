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
package wvlet.airframe.examples.codec
import wvlet.airframe.codec.MessageCodec
import wvlet.log.LogSupport

/**
  * Serialization/Deserialization example
  */
object Codec_01_SerDe extends App with LogSupport {
  case class Person(id: Int, name: String)

  // Creating a codec for Person class
  val personCodec = MessageCodec.of[Person]

  // [Serialization] Converting the Person object into MessagePack
  val msgpack = personCodec.pack(Person(1, "leo"))

  // [Deserialization] Unpack MessagePack into Person object
  val person = personCodec.unpack(msgpack)
  info(person)
}
