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

import wvlet.airframe.codec.PrimitiveCodec.ValueCodec
import wvlet.airframe.msgpack.spi.Value.StringValue
import wvlet.airframe.msgpack.spi.{MessagePack, MsgPack, Value, ValueFactory}

object ValueCodecTest {
  case class ValueTest(v: Value)
  case class RawMsgpackTest(msgpack: Array[Byte])
  case class RawMsgpackTest2(msgpack: MsgPack)
}

import ValueCodecTest._

/**
  *
  */
class ValueCodecTest extends CodecSpec {

  def `support MessagePack values`: Unit = {
    roundtrip(ValueCodec, ValueFactory.newInteger(1), DataType.ANY)
    roundtrip(ValueCodec, ValueFactory.newString("hello msgpack"), DataType.ANY)
    roundtrip(ValueCodec, ValueFactory.newBoolean(true), DataType.ANY)
    roundtrip(ValueCodec, ValueFactory.newFloat(0.1234d), DataType.ANY)
  }

  def `accept value`: Unit = {
    val codec = MessageCodec.of[ValueTest]
    codec.unpackJson("""{"v":"hello msgpack"}""") shouldBe Some(ValueTest(StringValue("hello msgpack")))
  }

  def `accept raw msgpack`: Unit = {
    val codec = MessageCodec.of[RawMsgpackTest]
    codec.unpackJson("""{"msgpack":"hello msgpack"}""") match {
      case Some(x) =>
        MessagePack.newUnpacker(x.msgpack).unpackValue shouldBe StringValue("hello msgpack")
      case _ =>
        fail("failed to parse msgpack")
    }
  }

  def `accept MsgPack type`: Unit = {
    val codec = MessageCodec.of[RawMsgpackTest2]
    codec.unpackJson("""{"msgpack":"hello msgpack"}""") match {
      case Some(x) =>
        MessagePack.newUnpacker(x.msgpack).unpackValue shouldBe StringValue("hello msgpack")
      case _ =>
        fail("failed to parse msgpack")
    }
  }

}
