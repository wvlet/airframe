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
package wvlet.airframe.http
import java.nio.charset.StandardCharsets

import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, Message, StringMessage}

trait HttpMessage[Raw] {
  def header: HttpMultiMapAccess = headerHolder
  protected def headerHolder: HttpMultiMap
  protected def message: Message

  protected def copyWith(newHeader: HttpMultiMap): Raw
  protected def copyWith(newMessage: Message): Raw

  def withHeader(key: String, value: String): Raw = {
    copyWith(headerHolder.set(key, value))
  }

  def addHeader(key: String, value: String): Raw = {
    copyWith(headerHolder.add(key, value))
  }

  def removeHeader(key: String): Raw = {
    copyWith(headerHolder.remove(key))
  }

  def withContent(content: String): Raw = {
    copyWith(StringMessage(content))
  }

  def withContent(content: Array[Byte]): Raw = {
    copyWith(ByteArrayMessage(content))
  }
  // Content reader
  def contentString: String = {
    message.toContentString
  }
  def contentBytes: Array[Byte] = {
    message.toContentBytes
  }

  // HTTP header setting utility methods
  def withAccept(acceptType: String): Raw = {
    withHeader(HttpHeader.Accept, acceptType)
  }
  def withAllow(allow: String): Raw = {
    withHeader(HttpHeader.Allow, allow)
  }

  def withAuthorization(authorization: String): Raw = {
    withHeader(HttpHeader.Authorization, authorization)
  }

  def withCacheControl(cacheControl: String): Raw = {
    withHeader(HttpHeader.CacheControl, cacheControl)
  }

  def contentType: Option[String] = header.get(HttpHeader.ContentType)
  def withContentType(contentType: String): Raw = {
    withHeader(HttpHeader.ContentType, contentType)
  }

  def withContentTypeJson: Raw = {
    withContentType("application/json;charset=utf-8")
  }

  def withContentTypeMsgPack: Raw = {
    withContentType("application/x-msgpack")
  }

  def withContentLength(length: Long): Raw = {
    withHeader(HttpHeader.ContentLength, s"${length}")
  }

  def withDate(date: String): Raw = {
    withHeader(HttpHeader.Date, date)
  }

  def withExpires(expires: String): Raw = {
    withHeader(HttpHeader.Expires, expires)
  }

  def withHost(host: String): Raw = {
    withHeader(HttpHeader.Host, host)
  }

  def withLastModified(lastModified: String): Raw = {
    withHeader(HttpHeader.LastModified, lastModified)
  }

  def withUserAgent(userAgenet: String): Raw = {
    withHeader(HttpHeader.UserAgent, userAgenet)
  }

  def withXForwardedFor(xForwardedFor: String): Raw = {
    withHeader(HttpHeader.xForwardedFor, xForwardedFor)
  }

  def withXForwardedProto(xForwardedProto: String): Raw = {
    withHeader(HttpHeader.xForwardedProto, xForwardedProto)
  }
}

/**
  * Http request/response data type definitions
  */
object HttpMessage {

  trait Message {
    def toContentString: String
    def toContentBytes: Array[Byte]
  }

  case object EmptyMessage extends Message {
    override def toContentString: String     = ""
    override def toContentBytes: Array[Byte] = Array.empty
  }

  case class StringMessage(content: String) extends Message {
    override def toContentString: String     = content
    override def toContentBytes: Array[Byte] = content.getBytes(StandardCharsets.UTF_8)
  }
  case class ByteArrayMessage(content: Array[Byte]) extends Message {
    override def toContentString: String = {
      new String(content, StandardCharsets.UTF_8)
    }
    override def toContentBytes: Array[Byte] = content
  }

  case class Request(
      method: HttpMethod = HttpMethod.GET,
      uri: String = "/",
      protected val headerHolder: HttpMultiMap = HttpMultiMap.empty,
      protected val message: Message = EmptyMessage
  ) extends HttpMessage[Request] {

    def path: String = {
      val u = uri
      u.indexOf("?") match {
        case -1  => u
        case pos => u.substring(0, pos)
      }
    }

    def query: Map[String, String] = {
      val u = uri
      u.indexOf("?") match {
        case -1 => Map.empty
        case pos =>
          val queryString = u.substring(0, pos)
          queryString
            .split("&").map { x =>
              x.split("=") match {
                case Array(key, value) => key -> value
                case _                 => x   -> ""
              }
            }.toMap
      }
    }

    def withMethod(method: HttpMethod): Request = {
      this.copy(method = method)
    }
    def withUri(uri: String): Request = this.copy(uri = uri)

    override protected def copyWith(newHeader: HttpMultiMap): Request = {
      this.copy(headerHolder = newHeader)
    }
    override protected def copyWith(
        newMessage: Message
    ): Request = {
      this.copy(message = newMessage)
    }
//    override protected def adapter: HttpRequestAdapter[Request] = HttpMessageRequestAdapter
//    override def toRaw: Request                                 = this
  }

  object Request {
    val empty: Request = Request()
  }

  case class Response(
      status: HttpStatus,
      protected val headerHolder: HttpMultiMap = HttpMultiMap.empty,
      protected val message: Message = EmptyMessage
  ) extends HttpMessage[Response] {
    override protected def copyWith(newHeader: HttpMultiMap): Response = {
      this.copy(headerHolder = newHeader)
    }
    override protected def copyWith(newMessage: Message): Response = {
      this.copy(message = newMessage)
    }
  }

//  object HttpMessageRequestAdapter extends HttpRequestAdapter[Request] {
//    override def requestType: Class[Request]                    = classOf[Request]
//    override def methodOf(request: Request): HttpMethod         = request.method
//    override def pathOf(request: Request): String               = request.path
//    override def queryOf(request: Request): Map[String, String] = request.query
//
//    override def headerOf(request: Request): Map[String, String]       = request.header
//    override def contentStringOf(request: Request): String             = request.contentString
//    override def contentBytesOf(request: Request): Array[Byte]         = request.contentBytes
//    override def contentTypeOf(request: Request): Option[String]       = request.contentType
//    override def httpRequestOf(request: Request): HttpRequest[Request] = request
//  }

}
