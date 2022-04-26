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
package wvlet.airframe.http

import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.{MessagePack, Value}
import wvlet.airspec.AirSpec

class RPCStatusTest extends AirSpec {

  test("No duplicates") {
    var knownCodes = Set.empty[Int]

    RPCStatus.all.foreach { x =>
      knownCodes.contains(x.code) shouldBe false
      knownCodes += x.code

      // sanity test
      val errorDetails = s"${x}[${x.code}] ${x.grpcStatus} ${x.httpStatus}"
      debug(errorDetails)
    }
  }

  test("ofCode(code) maps to the right code") {
    RPCStatus.all.foreach { x =>
      RPCStatus.ofCode(x.code) shouldBe x
    }
  }

  test("serialize as integer") {
    RPCStatus.all.foreach { x =>
      val packer = MessagePack.newBufferPacker
      x.pack(packer)
      val msgpack = packer.toByteArray

      val v = ValueCodec.fromMsgPack(msgpack)
      v shouldBe Value.LongValue(x.code)

    // TODO Support unapply(v: Value) is supported in airframe-codec
    // val codec = MessageCodec.of[RPCStatus]
    // val s1    = codec.fromMsgPack(msgpack)
    // info(s1)
    }
  }

}
