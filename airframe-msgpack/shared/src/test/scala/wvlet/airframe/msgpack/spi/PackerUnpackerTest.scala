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

import wvlet.airframe.AirframeSpec

/**
  *
  */
class PackerUnpackerTest extends AirframeSpec {

  def roundtrip[A](v: A, pack: (WriteCursor, A) => Unit, unpack: ReadCursor => A): Unit = {}

  "Packer/Unpacker" should {
    "satisfy round trip" in {
      roundtrip[Int](1, pack = Packer.packInt(_, _), unpack = Unpacker.unpackInt(_))
    }
  }

}
