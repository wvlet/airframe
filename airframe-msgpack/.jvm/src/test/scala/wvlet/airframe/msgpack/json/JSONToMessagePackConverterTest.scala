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
package wvlet.airframe.msgpack.json

import wvlet.airframe.json.JSON
import wvlet.airframe.json.JSON.{JSONArray, JSONBoolean, JSONNull, JSONNumber, JSONObject, JSONString, JSONValue}
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
class JSONToMessagePackConverterTest extends AirSpec {
  protected def msgpackToJson(msgpack: Array[Byte]): JSONValue = {
    val unpacker = MessagePack.newUnpacker(msgpack)
    try {
      val v = unpacker.unpackValue
      JSON.parse(v.toJson)
    } finally {
      unpacker.close()
    }
  }

  protected def deepEqual(a: JSONValue, b: JSONValue): Unit = {
    (a, b) match {
      case (JSONNull, JSONNull) =>
      // ok
      case (a: JSONString, b: JSONString) =>
        if (a.v != b.v) {
          warn(s"match failure:\n${a}\n-----\n${b}")
        }
        a shouldBe b
      case (a: JSONNumber, b: JSONNumber) =>
        a shouldBe b
      case (a: JSONBoolean, b: JSONBoolean) =>
        a shouldBe b
      case (a: JSONArray, b: JSONArray) =>
        a.size shouldBe b.size
        for (i <- 0 until a.size) {
          deepEqual(a(i), b(i))
        }
      case (a: JSONObject, b: JSONObject) =>
        a.size shouldBe b.size
        for (i <- 0 until a.size) {
          val ai = a.v(i)
          val bi = b.v(i)
          ai._1 shouldBe bi._1
          deepEqual(ai._2, bi._2)
        }
      case _ =>
        warn(s"${a} and ${b} doesn't match")
    }
  }

  test("convert JSON to msgpack") {
    val json    = "[0, 1, 2]"
    val msgpack = MessagePack.fromJSON(json)
    msgpackToJson(msgpack) shouldBe JSON.parse(json)
  }

  test("convert twitter-single.json to MsgPack") {
    val json      = IOUtil.readAsString("airframe-benchmark/src/main/resources/twitter-single.json")
    val jsonValue = JSON.parse(json)
    val msgpack   = MessagePack.fromJSON(json)
    val result    = msgpackToJson(msgpack)
    trace(result)
    trace(jsonValue.toJSON)
    deepEqual(result, jsonValue)
  }

  test("convert twitter.json to MsgPack") {
    val json      = IOUtil.readAsString("airframe-json/src/test/resources/twitter.json")
    val jsonValue = JSON.parse(json)
    val msgpack   = MessagePack.fromJSON(json)
    val result    = msgpackToJson(msgpack)
    deepEqual(result, jsonValue)
  }
}
