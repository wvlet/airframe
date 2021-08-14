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

import wvlet.airframe.msgpack.impl.PureScalaBufferUnpacker
import wvlet.airframe.msgpack.io.ByteArrayBuffer
import wvlet.airspec.AirSpec

/**
  */
class UnpackerTest extends AirSpec {
  test("hasNext at EOL") {
    val unpacker = MessagePack.newUnpacker(Array.empty[Byte])
    unpacker.hasNext shouldBe false
  }

  test("tryUnpackNil at EOL") {
    val unpacker = MessagePack.newUnpacker(Array.empty[Byte])
    unpacker.hasNext shouldBe false // false
    intercept[InsufficientBufferException] {
      unpacker.tryUnpackNil // InsufficientBufferException
    }
  }

  test("tryUnpackNil at EOL with pure-scala unpacker") {
    val unpacker = new PureScalaBufferUnpacker(ByteArrayBuffer(Array.empty[Byte]))
    unpacker.hasNext shouldBe false // false
    intercept[InsufficientBufferException] {
      unpacker.tryUnpackNil // InsufficientBufferException
    }
  }
}
