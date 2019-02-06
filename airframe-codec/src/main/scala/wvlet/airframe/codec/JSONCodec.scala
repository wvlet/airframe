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

import wvlet.airframe.json.JSON
import wvlet.airframe.json.JSON.JSONValue
import wvlet.airframe.msgpack.spi.{MessagePack, Packer, Unpacker}

/**
  *
  */
object JSONCodec extends MessageCodec[String] {

  override def pack(p: Packer, json: String): Unit = {
    val j = JSON.parse(json)
    packJsonValue(p, j)
  }

  def toMsgPack(jsonBytes: Array[Byte]): Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    packJsonValue(packer, JSON.parse(jsonBytes))
    packer.toByteArray
  }

  private def packJsonValue(p: Packer, v: JSONValue): Unit = {
    import wvlet.airframe.json.JSON._
    v match {
      case JSONObject(map) =>
        p.packMapHeader(map.size)
        for ((k: String, v: JSONValue) <- map) {
          p.packString(k)
          packJsonValue(p, v)
        }
      case JSONArray(arr) =>
        val len = arr.size
        p.packArrayHeader(len)
        arr.map { packJsonValue(p, _) }
      case JSONString(s) =>
        p.packString(s)
      case JSONNull =>
        p.packNil
      case JSONBoolean(v) =>
        p.packBoolean(v)
      case JSONDouble(v) =>
        p.packDouble(v)
      case JSONLong(l) =>
        p.packLong(l)
      case other =>
        throw new IllegalArgumentException(s"Unexpected json type: ${other}")
    }
  }

  override def unpack(u: Unpacker, v: MessageHolder): Unit = {
    val json = u.unpackValue.toJson
    v.setString(json)
  }

  def toJson(msgpack: Array[Byte]): String = {
    unpackBytes(msgpack).getOrElse {
      new IllegalArgumentException(s"Failed to read as json")
    }
  }
}
