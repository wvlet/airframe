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
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.impl.HttpMacros

import scala.concurrent.Future

object Http {

  // Aliases for http clients using standard HttpMessage.Request/Response
  type SyncClient  = HttpSyncClient[Request, Response]
  type AsyncClient = HttpClient[Future, Request, Response]

  /**
    * An entry point for building a new HttpClient
    */
  def client: HttpClientConfig = HttpClientConfig()

  /**
    * Create a new request
    */
  def request(method: String, uri: String) = HttpMessage.Request.empty.withMethod(method).withUri(uri)

  /**
    * Create a new request
    */
  def request(uri: String): HttpMessage.Request = request(HttpMethod.GET, uri)
  def GET(uri: String)                          = request(HttpMethod.GET, uri)
  def POST(uri: String)                         = request(HttpMethod.POST, uri)
  def DELETE(uri: String)                       = request(HttpMethod.DELETE, uri)
  def PUT(uri: String)                          = request(HttpMethod.PUT, uri)
  def PATCH(uri: String)                        = request(HttpMethod.PATCH, uri)

  def response(status: HttpStatus = HttpStatus.Ok_200): HttpMessage.Response = {
    HttpMessage.Response.empty.withStatus(status)
  }

  def response(status: HttpStatus, content: String): HttpMessage.Response = {
    response(status).withContent(content)
  }

  /**
    * Create an exception to redirect (status code = 302) the request to the target locationUrl
    * @param locationUrl
    * @param status
    * @return
    */
  def redirectException(
      locationUrl: String,
      status: HttpStatus = HttpStatus.Found_302
  ): HttpServerException = {
    new HttpServerException(status).withHeader(HttpHeader.Location, locationUrl)
  }

  /**
    * Create a new server exception that can be used to exit the Endpoint or RPC process.
    */
  def serverException(status: HttpStatus): HttpServerException = {
    new HttpServerException(status)
  }

  /**
    * Create a new server exception with an explicit cause
    */
  def serverException(status: HttpStatus, cause: Throwable): HttpServerException = {
    new HttpServerException(status, cause.getMessage, cause)
  }

  import scala.language.experimental.macros

  /**
    * Create a new HttpServerException with a custom content-body in JSON or MsgPack format.
    * The content type will be determined by the Accept header in the request
    */
  def serverException[A](request: HttpRequest[_], status: HttpStatus, content: A): HttpServerException =
    macro HttpMacros.newServerException[A]

  /**
    * Create a new HttpServerException with a custom content-body in JSON or MsgPack format.
    * The content type will be determined by the Accept header in the request
    */
  def serverException[A](
      request: HttpRequest[_],
      status: HttpStatus,
      content: A,
      codecFactory: MessageCodecFactory
  ): HttpServerException =
    macro HttpMacros.newServerExceptionWithCodecFactory[A]

  private[http] def parseAcceptHeader(value: Option[String]): Seq[String] = {
    value.map(_.split(",").map(_.trim).filter(_.nonEmpty).toSeq).getOrElse(Seq.empty)
  }
}

/**
  * HttpRequest[Req] wraps native request classes (e.g., okhttp's Response, finagle Response, etc.) so that we can
  * implement common logic for various backends.
  * @tparam Req
  */
trait HttpRequest[Req] {
  protected def adapter: HttpRequestAdapter[Req]
  def toRaw: Req
  def toHttpRequest: HttpMessage.Request = adapter.httpRequestOf(toRaw)

  def header: HttpMultiMap = adapter.headerOf(toRaw)

  def message: HttpMessage.Message = adapter.messageOf(toRaw)
  def contentType: Option[String]  = adapter.contentTypeOf(toRaw)
  def contentBytes: Array[Byte]    = adapter.contentBytesOf(toRaw)
  def contentString: String        = adapter.contentStringOf(toRaw)
  def accept: Seq[String]          = Http.parseAcceptHeader(header.get(HttpHeader.Accept))
  def acceptsMsgPack: Boolean      = accept.contains(HttpHeader.MediaType.ApplicationMsgPack)
}

/**
  * HttpResponse[Resp] wraps native response classes (e.g., okhttp's Response, finagle Response, etc.) so that we can
  * implement common logic for various backends.
  *
  * @tparam Resp
  */
trait HttpResponse[Resp] {
  protected def adapter: HttpResponseAdapter[Resp]
  def toRaw: Resp
  def toHttpResponse: HttpMessage.Response = adapter.httpResponseOf(toRaw)

  def status: HttpStatus   = adapter.statusOf(toRaw)
  def header: HttpMultiMap = adapter.headerOf(toRaw)

  def message: HttpMessage.Message = adapter.messageOf(toRaw)
  def contentType: Option[String]  = adapter.contentTypeOf(toRaw)
  def contentBytes: Array[Byte]    = adapter.contentBytesOf(toRaw)
  def contentString: String        = adapter.contentStringOf(toRaw)
}

/**
  * A type class to bridge the original requests and backend-specific request types (e.g., finagle, okhttp, etc.)
  *
  * @tparam Req
  */
trait HttpRequestAdapter[Req] {
  def requestType: Class[Req]

  def methodOf(request: Req): String

  /**
    * [/path](?[query params...])
    *
    * @param request
    * @return
    */
  def uriOf(request: Req): String
  def pathOf(request: Req): String
  def queryOf(request: Req): HttpMultiMap
  def headerOf(request: Req): HttpMultiMap
  def messageOf(request: Req): HttpMessage.Message
  def contentStringOf(request: Req): String     = messageOf(request).toContentString
  def contentBytesOf(request: Req): Array[Byte] = messageOf(request).toContentBytes
  def contentTypeOf(request: Req): Option[String]
  def pathComponentsOf(request: Req): IndexedSeq[String] = {
    pathOf(request).replaceFirst("/", "").split("/").toIndexedSeq
  }
  def httpRequestOf(request: Req): HttpMessage.Request = {
    Http
      .request(methodOf(request), uriOf(request))
      .withHeader(headerOf(request))
      .withContent(messageOf(request))
  }
  def wrap(request: Req): HttpRequest[Req]
}

/**
  * A type class to bridge the original response type and HttpResponse
  *
  * @tparam Resp
  */
trait HttpResponseAdapter[Resp] {
  def statusOf(resp: Resp): HttpStatus = HttpStatus.ofCode(statusCodeOf(resp))
  def statusCodeOf(resp: Resp): Int

  def messageOf(resp: Resp): HttpMessage.Message
  def contentStringOf(resp: Resp): String     = messageOf(resp).toContentString
  def contentBytesOf(resp: Resp): Array[Byte] = messageOf(resp).toContentBytes
  def contentTypeOf(resp: Resp): Option[String]
  def headerOf(resp: Resp): HttpMultiMap

  def httpResponseOf(resp: Resp): HttpMessage.Response = {
    Http
      .response(statusOf(resp))
      .withHeader(headerOf(resp))
      .withContent(messageOf(resp))
  }
  def wrap(resp: Resp): HttpResponse[Resp]
}
