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
package wvlet.airframe.tablet.text

import org.msgpack.core.{MessagePacker, MessageUnpacker}
import play.api.libs.json._
import wvlet.airframe.codec.{MessageCodec, MessageHolder}

/**
  *
  */
object JSONCodec extends MessageCodec[String] {

  override def pack(p: MessagePacker, json: String): Unit = {
    val j = Json.parse(json)
    packJsonValue(p, j)
  }

  private def packJsonValue(p: MessagePacker, v: JsValue) {
    v match {
      case obj: JsObject =>
        val map = obj.value
        p.packMapHeader(map.size)
        for ((k, v) <- map) {
          p.packString(k)
          packJsonValue(p, v)
        }
      case arr: JsArray =>
        val len = arr.value.size
        p.packArrayHeader(len)
        arr.value.map { packJsonValue(p, _) }
      case s: JsString =>
        p.packString(s.value)
      case JsNull =>
        p.packNil()
      case b: JsBoolean =>
        p.packBoolean(b.value)
      case i: JsNumber =>
        if (i.value.isValidLong) {
          p.packLong(i.value.longValue())
        } else {
          p.packDouble(i.value.doubleValue())
        }
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val json = u.unpackValue().toJson
    v.setString(json)
  }
}
