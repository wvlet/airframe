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

import java.time.Instant

/**
  * Exception to report errors to client
  */
case class HttpServerException(private var response: HttpMessage.Response, cause: Throwable)
    extends Exception(response.contentString, cause)
    with HttpServerExceptionBase {
  def this(status: HttpStatus, message: String, cause: Throwable) = this(Http.response(status, message), cause)
  def this(status: HttpStatus, message: String) = this(status, message, null)
  def this(status: HttpStatus) = this(status, status.toString, null)

  def status: HttpStatus = response.status
  def statusCode: Int    = response.statusCode

  def toResponse: HttpMessage.Response = response
  def header: HttpMultiMap             = response.header
  def message: HttpMessage.Message     = response.message

  def contentString: String = {
    message.toContentString
  }
  def contentBytes: Array[Byte] = {
    message.toContentBytes
  }

  def getHeader(key: String): Option[String] = header.get(key)
  def getAllHeader(key: String): Seq[String] = header.getAll(key)
  def allow: Option[String]                  = header.get(HttpHeader.Allow)
  def accept: Seq[String]                    = Http.parseAcceptHeader(header.get(HttpHeader.Accept))
  def authorization: Option[String]          = header.get(HttpHeader.Authorization)
  def cacheControl: Option[String]           = header.get(HttpHeader.CacheControl)
  def contentType: Option[String]            = header.get(HttpHeader.ContentType)
  def contentEncoding: Option[String]        = header.get(HttpHeader.ContentEncoding)
  def contentLength: Option[Long]            = header.get(HttpHeader.ContentLength).map(_.toLong)
  def date: Option[String]                   = header.get(HttpHeader.Date)
  def expires: Option[String]                = header.get(HttpHeader.Expires)
  def host: Option[String]                   = header.get(HttpHeader.Host)
  def lastModified: Option[String]           = header.get(HttpHeader.LastModified)
  def referer: Option[String]                = header.get(HttpHeader.Referer)
  def userAgent: Option[String]              = header.get(HttpHeader.UserAgent)
  def xForwardedFor: Option[String]          = header.get(HttpHeader.xForwardedFor)
  def xForwardedProto: Option[String]        = header.get(HttpHeader.xForwardedProto)

  def isContentTypeJson: Boolean = {
    contentType.exists(_.startsWith("application/json"))
  }
  def isContentTypeMsgPack: Boolean = {
    contentType.exists(_ == HttpHeader.MediaType.ApplicationMsgPack)
  }
  def acceptsJson: Boolean = {
    accept.exists(x => x == HttpHeader.MediaType.ApplicationJson || x.startsWith("application/json"))
  }
  def acceptsMsgPack: Boolean = {
    accept.exists(_ == HttpHeader.MediaType.ApplicationMsgPack)
  }

  def withHeader(key: String, value: String): HttpServerException = {
    updateWith(header.set(key, value))
  }

  def withHeader(newHeader: HttpMultiMap): HttpServerException = {
    updateWith(newHeader)
  }

  def addHeader(key: String, value: String): HttpServerException = {
    updateWith(header.add(key, value))
  }

  def removeHeader(key: String): HttpServerException = {
    updateWith(header.remove(key))
  }

  def withContent(content: Message): HttpServerException = {
    updateWith(content)
  }
  def withContent(content: String): HttpServerException = {
    updateWith(StringMessage(content))
  }
  def withContent(content: Array[Byte]): HttpServerException = {
    updateWith(HttpMessage.byteArrayMessage(content))
  }
  def withJson(json: String): HttpServerException = {
    updateWith(HttpMessage.stringMessage(json)).withContentTypeJson
  }
  def withMsgPack(msgPack: MsgPack): HttpServerException = {
    updateWith(HttpMessage.byteArrayMessage(msgPack)).withContentTypeMsgPack
  }

  // HTTP header setting utility methods
  def withAccept(acceptType: String): HttpServerException = withHeader(HttpHeader.Accept, acceptType)
  def withAcceptMsgPack: HttpServerException = withHeader(HttpHeader.Accept, HttpHeader.MediaType.ApplicationMsgPack)
  def withAllow(allow: String): HttpServerException = withHeader(HttpHeader.Allow, allow)
  def withAuthorization(authorization: String): HttpServerException =
    withHeader(HttpHeader.Authorization, authorization)
  def withCacheControl(cacheControl: String): HttpServerException = withHeader(HttpHeader.CacheControl, cacheControl)
  def withContentType(contentType: String): HttpServerException   = withHeader(HttpHeader.ContentType, contentType)
  def withContentTypeJson: HttpServerException             = withContentType(HttpHeader.MediaType.ApplicationJson)
  def withContentTypeMsgPack: HttpServerException          = withContentType(HttpHeader.MediaType.ApplicationMsgPack)
  def withContentLength(length: Long): HttpServerException = withHeader(HttpHeader.ContentLength, length.toString)
  def withDate(date: String): HttpServerException          = withHeader(HttpHeader.Date, date)
  def withDate(date: Instant)                              = withHeader(HttpHeader.Date, formatInstant(date))
  def withExpires(expires: String): HttpServerException    = withHeader(HttpHeader.Expires, expires)
  def withHost(host: String): HttpServerException          = withHeader(HttpHeader.Host, host)
  def withLastModified(lastModified: String): HttpServerException = withHeader(HttpHeader.LastModified, lastModified)
  def withReferer(referer: String): HttpServerException           = withHeader(HttpHeader.Referer, referer)
  def withUserAgent(userAgent: String): HttpServerException       = withHeader(HttpHeader.UserAgent, userAgent)
  def withXForwardedFor(xForwardedFor: String): HttpServerException =
    withHeader(HttpHeader.xForwardedFor, xForwardedFor)
  def withXForwardedProto(xForwardedProto: String): HttpServerException =
    withHeader(HttpHeader.xForwardedProto, xForwardedProto)

  protected def updateWith(newHeader: HttpMultiMap): HttpServerException = {
    // Do not create a copy to retain the original stack trace
    response = response.withHeader(newHeader)
    this
  }

  protected def updateWith(newMessage: HttpMessage.Message): HttpServerException = {
    // Do not create a copy to retain the original stack trace
    response = response.withContent(newMessage)
    this
  }

}
