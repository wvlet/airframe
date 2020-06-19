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
package wvlet.airframe.http.router

import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageContext}
import wvlet.airframe.http.{HttpResponse, HttpResponseAdapter}
import wvlet.airframe.msgpack.spi.{Packer, Unpacker}

/**
  */
class HttpResponseCodec[Resp: HttpResponseAdapter] extends MessageCodec[HttpResponse[_]] {
  override def pack(p: Packer, v: HttpResponse[_]): Unit = {
    v.contentType match {
      case Some("application/x-msgpack") =>
        // Raw msgpack response
        val b = v.contentBytes
        p.writePayload(b)
      case Some(x) if x.startsWith("application/json") =>
        // JSON -> MsgPack
        val json = v.contentString
        trace(s"response: ${json}")
        JSONCodec.pack(p, json)
      case _ =>
        val content = v.contentString
        if (content.startsWith("{") || content.startsWith("[")) {
          // JSON -> MsgPack
          trace(s"response: ${content}")
          JSONCodec.pack(p, content)
        } else {
          p.packString(content)
        }
    }
  }
  override def unpack(u: Unpacker, v: MessageContext): Unit = ???
}
