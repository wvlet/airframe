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

import wvlet.airframe.codec.PrimitiveCodec.StringCodec
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.Surface

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*

/**
  */
class CollectionCodecTest extends CodecSpec {

  test("support Map type") {
    val v = Map("id" -> 1)
    roundtrip(Surface.of[Map[String, Int]], v, DataType.ANY)
  }

  test("read null as empty map") {
    val codec   = MessageCodec.of[Map[String, Int]]
    val msgpack = MessagePack.newBufferPacker.packNil.toByteArray
    codec.unpack(msgpack) shouldBe Map.empty
  }

  test("support ListMap type") {
    val v = ListMap("z" -> 1, "y" -> 2)
    roundtrip(Surface.of[ListMap[String, Int]], v, DataType.ANY)
  }

  test("support Java Map type") {
    if (isScalaJS)
      skip("Scala.js do not support Java Map")
    val v = Map("id" -> 1).asJava
    roundtrip(Surface.of[java.util.Map[String, Int]], v, DataType.ANY)
  }

  test("support Seq/List type") {
    roundtrip(Surface.of[Seq[Int]], Seq(1, 2, 3), DataType.ANY)
    roundtrip(Surface.of[List[Int]], List(1, 2, 3), DataType.ANY)
  }

  test("support JSON Array") {
    val codec   = MessageCodec.of[Seq[Int]]
    val msgpack = MessagePack.newBufferPacker.packString("[1, 2, 3]").toByteArray
    codec.unpackMsgPack(msgpack) shouldBe Some(Seq(1, 2, 3))
  }

  test("support mapping a single string for Seq[X]") {
    val codec   = MessageCodec.of[Seq[Int]]
    val msgpack = StringCodec.toMsgPack("1")
    codec.fromMsgPack(msgpack) shouldBe Seq(1)
  }

  test("support JSON Map") {
    val codec   = MessageCodec.of[Map[String, Int]]
    val msgpack = MessagePack.newBufferPacker.packString("""{"leo":1, "yui":2}""").toByteArray
    codec.unpackMsgPack(msgpack) shouldBe Some(Map("leo" -> 1, "yui" -> 2))
  }

  test("support JSON Map to java.util.Map") {
    if (isScalaJS)
      skip("Scala.js doesn't support Java Map")
    val codec   = MessageCodec.of[java.util.Map[String, Int]]
    val msgpack = MessagePack.newBufferPacker.packString("""{"leo":1, "yui":2}""").toByteArray
    codec.unpackMsgPack(msgpack) shouldBe Some(Map("leo" -> 1, "yui" -> 2).asJava)
  }

  test("support JSON to primitive arrays") {
    MessageCodec.of[Array[Int]].fromMsgPack(StringCodec.toMsgPack("[1, 2, 3]")) shouldBe Array(1, 2, 3)
    MessageCodec.of[Array[Short]].fromMsgPack(StringCodec.toMsgPack("[1, 2, 3]")) shouldBe Array(
      1.toShort,
      2.toShort,
      3.toShort
    )
    MessageCodec.of[Array[Long]].fromMsgPack(StringCodec.toMsgPack("[1, 2, 3]")) shouldBe Array(1L, 2L, 3L)
    MessageCodec.of[Array[Float]].fromMsgPack(StringCodec.toMsgPack("[1.0, 2.0, 3.0]")) shouldBe Array(1f, 2f, 3f)
    MessageCodec.of[Array[Double]].fromMsgPack(StringCodec.toMsgPack("[1.0, 2.0, 3.0]")) shouldBe Array(1.0, 2.0, 3.0)
    MessageCodec.of[Array[Char]].fromMsgPack(StringCodec.toMsgPack("[1, 2, 3]")) shouldBe Array(
      1.toChar,
      2.toChar,
      3.toChar
    )
    MessageCodec.of[Array[Boolean]].fromMsgPack(StringCodec.toMsgPack("[true, false, true]")) shouldBe Array(
      true,
      false,
      true
    )
    MessageCodec.of[Array[String]].fromMsgPack(StringCodec.toMsgPack("""["a","b", "c"]""")) shouldBe Array(
      "a",
      "b",
      "c"
    )
  }

  test("support JSON to Array[Any]") {
    val codec    = MessageCodec.of[Array[Any]]
    val arr      = codec.fromJson("""[1, "a", true]""")
    val expected = Array(1, "a", true)
    for (i <- 0 until arr.size) {
      arr(i) shouldBe expected(i)
    }
  }
}
