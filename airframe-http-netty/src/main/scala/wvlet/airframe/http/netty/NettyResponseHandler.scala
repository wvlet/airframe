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
package wvlet.airframe.http.netty

import wvlet.airframe.codec.{JSONCodec, MessageCodec, MessageCodecFactory}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.router.{ResponseHandler, Route}
import wvlet.airframe.http.{Http, HttpStatus}
import wvlet.airframe.msgpack.spi.MsgPack
import wvlet.airframe.surface.{Primitive, Surface}
import wvlet.log.LogSupport

class NettyResponseHandler extends ResponseHandler[Request, Response] with LogSupport {
  private def codecFactory = MessageCodecFactory.defaultFactoryForJSON

  override def toHttpResponse[A](route: Route, request: Request, responseSurface: Surface, a: A): Response = {
    a match {
      case null =>
        newResponse(route, request, responseSurface)
      case r: Response =>
        r
      case b: MsgPack if request.acceptsMsgPack =>
        newResponse(route, request, responseSurface).withContentTypeMsgPack
          .withContent(b)
      case s: String if !request.acceptsMsgPack =>
        newResponse(route, request, responseSurface)
          .withContent(s)
      case _ =>
        val rs = codecFactory.of(responseSurface)
        val msgpack: Array[Byte] = rs match {
          case m: MessageCodec[_] =>
            m.asInstanceOf[MessageCodec[A]].toMsgPack(a)
          case _ =>
            throw new IllegalArgumentException(s"Unknown codec: ${rs}")
        }

        // Return application/x-msgpack content type
        if (request.acceptsMsgPack) {
          newResponse(route, request, responseSurface).withContentTypeMsgPack
            .withContent(msgpack)
        } else {
          val json = JSONCodec.unpackMsgPack(msgpack)
          json match {
            case Some(j) =>
              newResponse(route, request, responseSurface)
                .withJson(json.get)
            case None =>
              Http.response(HttpStatus.InternalServerError_500)
          }
        }
    }
  }

  private def newResponse(route: Route, request: Request, responseSurface: Surface): Response = {
    if (responseSurface == Primitive.Unit) {
      request.method match {
        case wvlet.airframe.http.HttpMethod.POST if route.isRPC =>
          // For RPC, return 200 even for POST
          Http.response(HttpStatus.Ok_200)
        case wvlet.airframe.http.HttpMethod.POST | wvlet.airframe.http.HttpMethod.PUT =>
          Http.response(HttpStatus.Created_201)
        case wvlet.airframe.http.HttpMethod.DELETE =>
          Http.response(HttpStatus.NoContent_204)
        case _ =>
          Http.response(HttpStatus.Ok_200)
      }
    } else {
      Http.response(HttpStatus.Ok_200)
    }
  }
}
