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

import wvlet.airframe.json.{JSON, Json}
import wvlet.airframe.json.JSON._
import wvlet.airframe.msgpack.spi._

/**
  * Codec for JSON String
  */
object JSONCodec extends MessageCodec[String] {
  override def pack(p: Packer, json: String): Unit = {
    val msgpack = MessagePack.fromJSON(json)
    p.writePayload(msgpack)
  }

  def toMsgPack(jsonBytes: Array[Byte]): Array[Byte] = {
    MessagePack.fromJSON(jsonBytes)
  }

  def toMsgPack(jsonValue: JSONValue): Array[Byte] = {
    val packer = MessagePack.newBufferPacker
    packJsonValue(packer, jsonValue)
    packer.toByteArray
  }

  def packJsonValue(p: Packer, v: JSONValue): Unit = {
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

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    val json = u.unpackValue.toJson
    v.setString(json)
  }

  def toJson(msgpack: Array[Byte]): String = {
    unpackBytes(msgpack).getOrElse {
      throw new IllegalArgumentException("Failed to read as json")
    }
  }
}

object RawJsonCodec extends MessageCodec[Json] {
  override def pack(p: Packer, v: Json): Unit = {
    JSONCodec.pack(p, v)
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    JSONCodec.unpack(u, v)
  }
}

/**
  * Codec for JSONValue
  */
object JSONValueCodec extends MessageCodec[JSONValue] {
  override def pack(p: Packer, v: JSONValue): Unit = {
    JSONCodec.packJsonValue(p, v)
  }

  def unpackJson(u: Unpacker): JSONValue = {
    u.getNextValueType match {
      case ValueType.NIL =>
        u.unpackNil
        JSONNull
      case ValueType.STRING =>
        JSONString(u.unpackString)
      case ValueType.FLOAT =>
        JSONDouble(u.unpackDouble)
      case ValueType.INTEGER =>
        JSONLong(u.unpackLong)
      case ValueType.BOOLEAN =>
        JSONBoolean(u.unpackBoolean)
      case ValueType.EXTENSION =>
        JSONString(u.unpackValue.toString)
      case ValueType.BINARY =>
        JSONString(u.unpackValue.toJson)
      case ValueType.ARRAY =>
        val len = u.unpackArrayHeader
        val b   = IndexedSeq.newBuilder[JSONValue]
        for (i <- 0 until len) {
          b += unpackJson(u)
        }
        JSONArray(b.result())
      case ValueType.MAP =>
        val len = u.unpackMapHeader
        val b   = Seq.newBuilder[(String, JSONValue)]
        for (i <- 0 until len) yield {
          val key = u.unpackString
          b += key -> unpackJson(u)
        }
        JSONObject(b.result())
    }
  }

  override def unpack(u: Unpacker, v: MessageContext): Unit = {
    val jsonValue = unpackJson(u)
    v.setObject(jsonValue)
  }
}
