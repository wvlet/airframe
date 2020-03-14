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
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, Message, StringMessage}

trait HttpMessage {
  protected def header: HttpHeader
  protected def message: Message

  protected def copyWith(newHeader: HttpHeader): this.type
  protected def copyWith(newMessage: Message): this.type

  def withHeader(key: String, value: String): this.type = {
    copyWith(header.set(key, value))
  }

  def addHeader(key: String, value: String): this.type = {
    copyWith(header.add(key, value))
  }

  def removeHeader(key: String): this.type = {
    copyWith(header.remove(key))
  }

  def withContent(content: String): this.type = {
    copyWith(StringMessage(content))
  }

  def withContent(content: Array[Byte]): this.type = {
    copyWith(ByteArrayMessage(content))
  }

  // HTTP header setting utility methods
  def withAccept(acceptType: String): this.type = {
    withHeader("Accept", acceptType)
  }
  def withAllow(allow: String): this.type = {
    withHeader("Allow", allow)
  }

  def withAuthorization(authorization: String): this.type = {
    withHeader("Authorization", authorization)
  }

  def withCacheControl(cacheControl: String): this.type = {
    withHeader("Cache-Control", cacheControl)
  }

  def withContentType(contentType: String): this.type = {
    withHeader("Content-Type", contentType)
  }

  def withContentTypeJson: this.type = {
    withContentType("application/json;charset=utf-8")
  }

  def withContentTypeMsgPack: this.type = {
    withContentType("application/x-msgpack")
  }

  def withContentLength(length: Long): this.type = {
    withHeader("Content-Length", s"${length}")
  }

  def withDate(date: String): this.type = {
    withHeader("Date", date)
  }

  def withExpires(expires: String): this.type = {
    withHeader("Expires", expires)
  }

  def withHost(host: String): this.type = {
    withHeader("Host", host)
  }

  def withLastModified(lastModified: String): this.type = {
    withHeader("Last-Modified", lastModified)
  }

  def withUserAgent(userAgenet: String): this.type = {
    withHeader("User-Agent", userAgenet)
  }

  def withXForwardedFor(xForwardedFor: String): this.type = {
    withHeader("X-Forwarded-For", xForwardedFor)
  }

  def withXForwardedProto(xForwardedProto: String): this.type = {
    withHeader("X-Forwarded-Proto", xForwardedProto)
  }

}

/**
  * Http request/response data type definitions
  */
object HttpMessage {

  trait Message
  case object EmptyMessage                          extends Message
  case class StringMessage(content: String)         extends Message
  case class ByteArrayMessage(content: Array[Byte]) extends Message

  case class Request(
      method: HttpMethod = HttpMethod.GET,
      uri: String = "/",
      protected val header: HttpHeader = HttpHeader.empty,
      protected val message: Message = EmptyMessage
  ) extends HttpMessage {

    override protected def copyWith(newHeader: HttpHeader): Request.this.type = {
      this.copy(header = newHeader)
    }
    override protected def copyWith(
        newMessage: Message
    ): Request.this.type = {
      this.copy(message = newMessage)
    }

    def path: String = {
      val u = uri
      u.indexOf("?") match {
        case -1  => u
        case pos => u.substring(0, pos)
      }
    }
    def withMethod(method: HttpMethod): Request = {
      this.copy(method = method)
    }
    def withUri(uri: String): Request = this.copy(uri = uri)
  }

  object Request {
    val empty: Request = Request()
  }

  case class Response(
      status: HttpStatus,
      protected val header: HttpHeader = HttpHeader.empty,
      protected val message: Message = EmptyMessage
  ) extends HttpMessage {
    override protected def copyWith(newHeader: HttpHeader): Response.this.type = {
      this.copy(header = newHeader)
    }
    override protected def copyWith(newMessage: Message): Response.this.type = {
      this.copy(message = newMessage)
    }
  }

}
