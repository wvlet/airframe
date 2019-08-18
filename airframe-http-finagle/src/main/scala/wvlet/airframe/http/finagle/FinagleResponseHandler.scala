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
package wvlet.airframe.http.finagle

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.Buf.ByteArray
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.{ResponseHandler, SimpleHttpResponse}
import wvlet.airframe.surface.Surface

/**
  * Converting controller results into finagle http responses.
  */
trait FinagleResponseHandler extends ResponseHandler[Request, Response] {

  // Use Map codecs to create natural JSON responses
  private[this] val mapCodecFactory =
    MessageCodecFactory.defaultFactory.withObjectMapCodec

  // TODO: Extract this logic into airframe-http
  def toHttpResponse[A](request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case r: Response =>
        // Return the response as is
        r
      case r: SimpleHttpResponse =>
        val resp = Response(request)
        resp.statusCode = r.statusCode
        resp.contentString = r.contentString
        r.contentType.map { c =>
          resp.contentType = c
        }
        resp
      case s: String =>
        val r = Response()
        r.contentString = s
        r
      case _ =>
        // TODO Return large responses with streams

        // Convert the response object into JSON
        val rs = mapCodecFactory.of(responseSurface)
        val msgpack: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // Return application/msgpack content type
        if (request.accept.contains("application/x-msgpack")) {
          val res = Response(Status.Ok)
          res.contentType = "application/x-msgpack"
          res.content = ByteArray.Owned(msgpack)
          res
        } else {
          val json = JSONCodec.unpackMsgPack(msgpack)
          json match {
            case Some(j) =>
              val res = Response(Status.Ok)
              res.setContentTypeJson()
              res.setContentString(json.get)
              res
            case None =>
              Response(Status.InternalServerError)
          }
        }
    }
  }
}
