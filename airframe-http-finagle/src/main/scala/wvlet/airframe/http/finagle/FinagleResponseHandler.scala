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
import com.twitter.finagle.http._
import com.twitter.io.Buf.ByteArray
import com.twitter.io.{Buf, Reader}
import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.router.{ResponseHandler, Route}
import wvlet.airframe.http.{HttpMessage, HttpStatus}
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport

/**
  * Converting controller results into finagle http responses.
  */
class FinagleResponseHandler(customCodec: PartialFunction[Surface, MessageCodec[_]])
    extends ResponseHandler[Request, Response]
    with LogSupport {
  private[this] val mapCodecFactory = {
    MessageCodecFactory
      // Enable JSON support to convert objects into Maps
      .defaultFactoryForJSON
      // Add custom codecs
      .orElse(MessageCodecFactory.newFactory(customCodec))
  }

  private val xMsgPack = "application/x-msgpack"

  private def isMsgPackRequest(request: Request): Boolean = {
    request.accept.contains(xMsgPack)
  }

  private def isOctetStreamRequest(request: Request): Boolean = {
    request.accept.contains(MediaType.OctetStream) || (
      request.contentType.isDefined && request.contentType.get.contains(MediaType.OctetStream)
    )
  }

  private def newResponse(route: Route, request: Request, responseSurface: Surface): Response = {
    val r = Response(request)
    if (responseSurface == Primitive.Unit) {
      request.method match {
        case Method.Post if route.isRPC =>
          // For RPC, return 200 even for POST
          r.statusCode = HttpStatus.Ok_200.code
        case Method.Post | Method.Put =>
          r.statusCode = HttpStatus.Created_201.code
        case Method.Delete =>
          r.statusCode = HttpStatus.NoContent_204.code
        case _ =>
          r.statusCode = HttpStatus.Ok_200.code
      }
    }
    r
  }

  private def newStreamResponse(request: Request, reader: Reader[Buf]): Response = {
    val resp = Response(request.version, Status.Ok, reader)
    if (isMsgPackRequest(request)) {
      resp.contentType = xMsgPack
    } else if (isOctetStreamRequest(request)) {
      resp.contentType = MediaType.OctetStream
    } else {
      // TODO Support other content types
      resp.setContentTypeJson()
    }
    resp
  }

  // TODO: Extract this logic into airframe-http
  def toHttpResponse[A](route: Route, request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case null =>
        // Empty response
        val r = newResponse(route, request, responseSurface)
        r
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
            val r     = reader.asInstanceOf[Reader[Any]]
            val codec = mapCodecFactory.of(elemType).asInstanceOf[MessageCodec[Any]]

            if (isMsgPackRequest(request)) {
              // Return a stream sequence of MessagePack bytes
              newStreamResponse(request, FinagleResponseHandler.toMsgPackStream(reader, codec))
            } else {
              val bufReader = FinagleResponseHandler.toJsonBufStream(reader, codec)
              newStreamResponse(request, bufReader)
            }
          case None =>
            throw new IllegalArgumentException(s"Unknown Reader[X] type: ${responseSurface}")
        }
      case r: HttpMessage.Response =>
        convertToFinagleResponse(r)
      case b: Array[Byte] =>
        val r = newResponse(route, request, responseSurface)
        r.content = Buf.ByteArray.Owned(b)
        r
      case s: String =>
        val r = newResponse(route, request, responseSurface)
        r.contentString = s
        r
      case _ =>
        // To return large responses with streams, the interface should return Reader[X] response

        // Convert the response object into JSON
        val rs = mapCodecFactory.of(responseSurface)
        val msgpack: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // Return application/x-msgpack content type
        if (isMsgPackRequest(request)) {
          val res = newResponse(route, request, responseSurface)
          res.contentType = xMsgPack
          res.content = ByteArray.Owned(msgpack)
          res
        } else {
          val json = JSONCodec.unpackMsgPack(msgpack)
          json match {
            case Some(j) =>
              val res = newResponse(route, request, responseSurface)
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

  def toMsgPackStream(reader: Reader[_], codec: MessageCodec[Any]): Reader[Buf] = {
    reader.map { x =>
      val msgpack = codec.toMsgPack(x)
      Buf.ByteArray.Owned(msgpack)
    }
  }
}
