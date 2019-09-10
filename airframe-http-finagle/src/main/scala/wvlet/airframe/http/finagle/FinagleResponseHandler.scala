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

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.Buf.ByteArray
import com.twitter.io.{Buf, Reader}
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.{ResponseHandler, SimpleHttpResponse}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

/**
  * Converting controller results into finagle http responses.
  */
trait FinagleResponseHandler extends ResponseHandler[Request, Response] with LogSupport {

  // Use Map codecs to create natural JSON responses
  private[this] val mapCodecFactory =
    MessageCodecFactory.defaultFactory.withObjectMapCodec

  private def isMsgPackRequest(request: Request): Boolean = {
    request.accept.contains("application/x-msgpack")
  }

  private def newStreamResponse(request: Request, reader: Reader[Buf]): Response = {
    val resp = Response(request.version, Status.Ok, reader)
    if (isMsgPackRequest(request)) {
      resp.contentType = "application/x-msgpack"
    } else {
      // TODO Support other content types
      resp.setContentTypeJson()
    }
    resp
  }

  // TODO: Extract this logic into airframe-http
  def toHttpResponse[A](request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case r: Response =>
        // Return the response as is
        r
      case reader: Reader[_] =>
        // Return the response using streaming
        responseSurface.typeArgs.headOption match {
          case Some(bufType) if classOf[Buf].isAssignableFrom(bufType.rawType) =>
            // Reader[Buf] can be returned directly
            newStreamResponse(request, reader.asInstanceOf[Reader[Buf]])
          case Some(elemType) =>
            // For Reader[X], convert Seq[X] into JSON array
            val r         = reader.asInstanceOf[Reader[Any]]
            val codec     = mapCodecFactory.of(elemType).asInstanceOf[MessageCodec[Any]]
            val bufReader = FinagleResponseHandler.toJsonBufStream(reader, codec)
            newStreamResponse(request, bufReader)
          case None =>
            throw new IllegalArgumentException(s"Unknown Reader[X] type: ${responseSurface}")
        }
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
        if (isMsgPackRequest(request)) {
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

object FinagleResponseHandler {

  def toJsonBufStream(reader: Reader[_], codec: MessageCodec[Any]): Reader[Buf] = {
    val reported = new AtomicBoolean(false)

    val jsonArrayElementReader = reader.map { x =>
      val json = codec.toJson(x)
      if (reported.compareAndSet(false, true)) {
        // The first element
        Buf.Utf8(json)
      } else {
        Buf.Utf8(s",${json}")
      }
    }

    val jsonReader: AsyncStream[Reader[Buf]] = AsyncStream.fromSeq(
      Seq(
        Reader.fromBuf(Buf.Utf8("[")),
        jsonArrayElementReader,
        Reader.fromBuf(Buf.Utf8("]"))
      )
    )

    Reader.concat(jsonReader)
  }

}
