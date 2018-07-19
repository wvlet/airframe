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
import org.json4s._
import org.json4s.native.JsonMethods._
import wvlet.airframe.codec.{MessageCodec, MessageHolder}

/**
  *
  */
object JSONCodec extends MessageCodec[String] {

  override def pack(p: MessagePacker, json: String): Unit = {
    val j = parse(json, useBigIntForLong = false)
    packJsonValue(p, j)
  }

  private def packJsonValue(p: MessagePacker, v: JValue): Unit = {
    v match {
      case jo: JObject =>
        val map = jo.obj
        p.packMapHeader(map.size)
        for ((k: String, v: JValue) <- map) {
          p.packString(k)
          packJsonValue(p, v)
        }
      case arr: JArray =>
        val len = arr.arr.size
        p.packArrayHeader(len)
        arr.arr.map { packJsonValue(p, _) }
      case s: JString =>
        p.packString(s.values)
      case JNull | JNothing =>
        p.packNil()
      case b: JBool =>
        p.packBoolean(b.values)
      case d: JDouble =>
        p.packDouble(d.values)
      case l: JLong =>
        p.packLong(l.values)
      case other =>
        throw new IllegalArgumentException(s"Unexpected json type: ${other}")
      // These two passes will not be used when
      // parsing with useDecimalForDouble = false and useBigIntForLong = true flags
//      case i: JInt =>
//        if (i.values.isValidLong) {
//          p.packLong(i.values.bigInteger.longValue())
//        } else {
//          p.packDouble(i.values.doubleValue())
//        }
//      case d: JDecimal =>
//        if (d.values.isValidLong) {
//          p.packLong(d.values.longValue())
//        } else {
//          p.packDouble(d.values.doubleValue())
//        }
    }
  }

  override def unpack(u: MessageUnpacker, v: MessageHolder): Unit = {
    val json = u.unpackValue().toJson
    v.setString(json)
  }
}
