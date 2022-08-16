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
import wvlet.airframe.http.Http.formatInstant
import wvlet.airframe.http.HttpMessage.{Message, StringMessage}
import wvlet.airframe.msgpack.spi.MsgPack

import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.language.experimental.macros

trait HttpMessage[Raw] extends HttpMessageBase[Raw] {
  def header: HttpMultiMap

  // Accessors
  def getHeader(key: String): Option[String] = header.get(key)
  def getAllHeader(key: String): Seq[String] = header.getAll(key)

  def allow: Option[String]           = header.get(HttpHeader.Allow)
  def accept: Seq[String]             = Http.parseAcceptHeader(header.get(HttpHeader.Accept))
  def authorization: Option[String]   = header.get(HttpHeader.Authorization)
  def cacheControl: Option[String]    = header.get(HttpHeader.CacheControl)
  def contentType: Option[String]     = header.get(HttpHeader.ContentType)
  def contentEncoding: Option[String] = header.get(HttpHeader.ContentEncoding)
  def contentLength: Option[Long]     = header.get(HttpHeader.ContentLength).map(_.toLong)
  def date: Option[String]            = header.get(HttpHeader.Date)
  def expires: Option[String]         = header.get(HttpHeader.Expires)
  def host: Option[String]            = header.get(HttpHeader.Host)
  def lastModified: Option[String]    = header.get(HttpHeader.LastModified)
  def referer: Option[String]         = header.get(HttpHeader.Referer)
  def userAgent: Option[String]       = header.get(HttpHeader.UserAgent)
  def xForwardedFor: Option[String]   = header.get(HttpHeader.xForwardedFor)
  def xForwardedProto: Option[String] = header.get(HttpHeader.xForwardedProto)

  def message: Message

  protected def copyWith(newHeader: HttpMultiMap): Raw
  protected def copyWith(newMessage: Message): Raw

  def withHeader(key: String, value: String): Raw = {
    copyWith(header.set(key, value))
  }

  def withHeader(newHeader: HttpMultiMap): Raw = {
    copyWith(newHeader)
  }

  def addHeader(key: String, value: String): Raw = {
    copyWith(header.add(key, value))
  }

  def removeHeader(key: String): Raw = {
    copyWith(header.remove(key))
  }

  def withContent(content: Message): Raw = {
    copyWith(content)
  }
  def withContent(content: String): Raw = {
    copyWith(StringMessage(content))
  }
  def withContent(content: Array[Byte]): Raw = {
    copyWith(HttpMessage.byteArrayMessage(content))
  }
  def withJson(json: String): Raw = {
    copyWith(HttpMessage.stringMessage(json)).asInstanceOf[HttpMessage[Raw]].withContentTypeJson
  }
  def withJson(json: Array[Byte]): Raw = {
    copyWith(HttpMessage.byteArrayMessage(json)).asInstanceOf[HttpMessage[Raw]].withContentTypeJson
  }

  def withMsgPack(msgPack: MsgPack): Raw = {
    copyWith(HttpMessage.byteArrayMessage(msgPack)).asInstanceOf[HttpMessage[Raw]].withContentTypeMsgPack
  }

  // Content reader
  def contentString: String = {
    message.toContentString
  }
  def contentBytes: Array[Byte] = {
    message.toContentBytes
  }

  // HTTP header setting utility methods
  def withAccept(acceptType: String): Raw = withHeader(HttpHeader.Accept, acceptType)
  def withAcceptMsgPack: Raw              = withHeader(HttpHeader.Accept, HttpHeader.MediaType.ApplicationMsgPack)
  def withAcceptJson: Raw                 = withHeader(HttpHeader.Accept, HttpHeader.MediaType.ApplicationJson)
  def withAllow(allow: String): Raw       = withHeader(HttpHeader.Allow, allow)
  def withAuthorization(authorization: String): Raw     = withHeader(HttpHeader.Authorization, authorization)
  def withCacheControl(cacheControl: String): Raw       = withHeader(HttpHeader.CacheControl, cacheControl)
  def withContentType(contentType: String): Raw         = withHeader(HttpHeader.ContentType, contentType)
  def withContentTypeJson: Raw                          = withContentType(HttpHeader.MediaType.ApplicationJson)
  def withContentTypeMsgPack: Raw                       = withContentType(HttpHeader.MediaType.ApplicationMsgPack)
  def withContentLength(length: Long): Raw              = withHeader(HttpHeader.ContentLength, length.toString)
  def withDate(date: String): Raw                       = withHeader(HttpHeader.Date, date)
  def withDate(date: Instant)                           = withHeader(HttpHeader.Date, formatInstant(date))
  def withExpires(expires: String): Raw                 = withHeader(HttpHeader.Expires, expires)
  def withHost(host: String): Raw                       = withHeader(HttpHeader.Host, host)
  def withLastModified(lastModified: String): Raw       = withHeader(HttpHeader.LastModified, lastModified)
  def withReferer(referer: String): Raw                 = withHeader(HttpHeader.Referer, referer)
  def withUserAgent(userAgent: String): Raw             = withHeader(HttpHeader.UserAgent, userAgent)
  def withXForwardedFor(xForwardedFor: String): Raw     = withHeader(HttpHeader.xForwardedFor, xForwardedFor)
  def withXForwardedProto(xForwardedProto: String): Raw = withHeader(HttpHeader.xForwardedProto, xForwardedProto)

  def isContentTypeJson: Boolean = {
    contentType.exists(_.startsWith("application/json"))
  }
  def isContentTypeMsgPack: Boolean = {
    contentType.exists(x => x == HttpHeader.MediaType.ApplicationMsgPack || x == "application/x-msgpack")
  }
  def acceptsJson: Boolean = {
    accept.exists(x => x == HttpHeader.MediaType.ApplicationJson || x.startsWith("application/json"))
  }
  def acceptsMsgPack: Boolean = {
    accept.exists(x => x == HttpHeader.MediaType.ApplicationMsgPack || x == "application/x-msgpack")
  }
}

/**
  * Http request/response data type definitions
  */
object HttpMessage {

  trait Message {
    def isEmpty: Boolean  = false
    def nonEmpty: Boolean = !isEmpty
    def toContentString: String
    def toContentBytes: Array[Byte]
  }

  object Message {
    def unapply(s: String): Option[Message] = {
      if (s.isEmpty) {
        Some(EmptyMessage)
      } else {
        Some(StringMessage(s))
      }
    }
  }

  case object EmptyMessage extends Message {
    override def isEmpty: Boolean            = true
    override def toContentString: String     = ""
    override def toContentBytes: Array[Byte] = Array.empty
  }

  case class StringMessage(content: String) extends Message {
    override def toString: String            = content
    override def toContentString: String     = content
    override def toContentBytes: Array[Byte] = content.getBytes(StandardCharsets.UTF_8)
  }
  case class ByteArrayMessage(content: Array[Byte]) extends Message {
    override def toString: String = toContentString
    override def toContentString: String = {
      new String(content, StandardCharsets.UTF_8)
    }
    override def toContentBytes: Array[Byte] = content
  }

  class LazyByteArrayMessage(contentReader: => Array[Byte]) extends Message {
    // Use lazy evaluation of content body to avoid unnecessary data copy
    private lazy val content: Array[Byte] = contentReader
    override def toString: String         = toContentString
    override def toContentString: String = {
      new String(content, StandardCharsets.UTF_8)
    }
    override def toContentBytes: Array[Byte] = content
  }

  def stringMessage(content: String): Message = {
    if (content == null || content.isEmpty) {
      EmptyMessage
    } else {
      StringMessage(content)
    }
  }
  def byteArrayMessage(content: Array[Byte]): Message = {
    if (content == null || content.isEmpty)
      EmptyMessage
    else
      ByteArrayMessage(content)
  }

  case class Request(
      method: String = HttpMethod.GET,
      // Path and query string beginning from "/"
      uri: String = "/",
      header: HttpMultiMap = HttpMultiMap.empty,
      message: Message = EmptyMessage
  ) extends HttpMessage[Request] {
    override def toString: String = s"Request(${method},${uri},${header})"

    /**
      * URI without query string (e.g., /v1/info)
      */
    def path: String = {
      val u = uri
      u.indexOf("?") match {
        case -1  => u
        case pos => u.substring(0, pos)
      }
    }

    /**
      * Extract the query string parameters as HttpMultiMap
      */
    def query: HttpMultiMap = extractQueryFromUri(uri)

    def withFilter(f: Request => Request): Request = f(this)
    def withMethod(method: String): Request        = this.copy(method = method)
    def withUri(uri: String): Request              = this.copy(uri = uri)

    override protected def copyWith(newHeader: HttpMultiMap): Request = this.copy(header = newHeader)
    override protected def copyWith(newMessage: Message): Request     = this.copy(message = newMessage)
  }

  private[http] def extractQueryFromUri(uri: String): HttpMultiMap = {
    uri.indexOf("?") match {
      case -1 =>
        HttpMultiMap.empty
      case pos =>
        var m = HttpMultiMap.newBuilder
        if (pos + 1 < uri.length) {
          val queryString = uri.substring(pos + 1)
          queryString
            .split("&").map { x =>
              x.split("=") match {
                case Array(key, value) =>
                  m = m.add(key, value)
                case _ =>
                  m = m.add(x, "")
              }
            }
        }
        m.result()
    }
  }

  object Request {
    val empty: Request = Request()
  }

  case class Response(
      status: HttpStatus = HttpStatus.Ok_200,
      header: HttpMultiMap = HttpMultiMap.empty,
      message: Message = EmptyMessage
  ) extends HttpMessage[Response] {
    override def toString: String = s"Response(${status},${header})"

    override protected def copyWith(newHeader: HttpMultiMap): Response = this.copy(header = newHeader)
    override protected def copyWith(newMessage: Message): Response     = this.copy(message = newMessage)

    def statusCode: Int                             = status.code
    def withStatus(newStatus: HttpStatus): Response = this.copy(status = newStatus)
  }

  object Response {
    val empty: Response = Response()
  }

  implicit object HttpMessageRequestAdapter extends HttpRequestAdapter[Request] { self =>
    override def requestType: Class[Request]             = classOf[Request]
    override def methodOf(request: Request): String      = request.method
    override def uriOf(request: Request): String         = request.uri
    override def pathOf(request: Request): String        = request.path
    override def queryOf(request: Request): HttpMultiMap = request.query

    override def headerOf(request: Request): HttpMultiMap        = request.header
    override def messageOf(request: Request): Message            = request.message
    override def contentTypeOf(request: Request): Option[String] = request.contentType
    override def httpRequestOf(request: Request): Request        = request
    override def wrap(request: Request): HttpRequest[Request]    = new HttpMessageRequestWrapper(request)
  }

  implicit object HttpMessageResponseAdapter extends HttpResponseAdapter[Response] { self =>
    override def statusCodeOf(resp: Response): Int             = resp.status.code
    override def contentTypeOf(resp: Response): Option[String] = resp.contentType
    override def httpResponseOf(resp: Response): Response      = resp
    override def messageOf(resp: Response): Message            = resp.message
    override def headerOf(resp: Response): HttpMultiMap        = resp.header
    override def wrap(resp: Response): HttpResponse[Response]  = new HttpMessageResponseWrapper(resp)
  }

  implicit class HttpMessageRequestWrapper(val raw: Request) extends HttpRequest[Request] {
    override protected def adapter: HttpRequestAdapter[Request] = HttpMessageRequestAdapter
    override def toRaw: Request                                 = raw
  }

  implicit class HttpMessageResponseWrapper(val raw: Response) extends HttpResponse[Response] {
    override protected def adapter: HttpResponseAdapter[Response] = HttpMessageResponseAdapter
    override def toRaw: Response                                  = raw
  }
}
