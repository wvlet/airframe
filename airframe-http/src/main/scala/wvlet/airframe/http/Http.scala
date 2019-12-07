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

import wvlet.airframe.http.SimpleHttpRequest.SimpleHttpRequestAdapter
import wvlet.airframe.http.SimpleHttpResponse.SimpleHttpResponseAdapter

/**
  * Type class to bridge the original requests
  *
  * @tparam Req
  */
trait HttpRequestAdapter[Req] {
  def requestType: Class[Req]

  def methodOf(request: Req): HttpMethod
  def pathOf(request: Req): String
  def queryOf(request: Req): Map[String, String]
  def headerOf(request: Req): Map[String, String]
  def contentStringOf(request: Req): String
  def contentBytesOf(request: Req): Array[Byte]
  def contentTypeOf(request: Req): Option[String]
  def pathComponentsOf(request: Req): IndexedSeq[String] = {
    pathOf(request).replaceFirst("/", "").split("/").toIndexedSeq
  }
  def httpRequestOf(request: Req): HttpRequest[Req]
}

trait HttpRequest[Req] {
  protected def adapter: HttpRequestAdapter[Req]

  def method: HttpMethod         = adapter.methodOf(toRaw)
  def path: String               = adapter.pathOf(toRaw)
  def query: Map[String, String] = adapter.queryOf(toRaw)
  // TODO Use multi-map
  def header: Map[String, String]        = adapter.headerOf(toRaw)
  def contentString: String              = adapter.contentStringOf(toRaw)
  def contentBytes: Array[Byte]          = adapter.contentBytesOf(toRaw)
  def contentType: Option[String]        = adapter.contentTypeOf(toRaw)
  def pathComponents: IndexedSeq[String] = adapter.pathComponentsOf(toRaw)
  def toHttpRequest: HttpRequest[Req]    = adapter.httpRequestOf(toRaw)
  def toRaw: Req
}

/**
  * Type class to bridge the original response type and HttpResponse
  *
  * @tparam Resp
  */
trait HttpResponseAdapter[Resp] {
  def statusOf(resp: Resp): HttpStatus = HttpStatus.ofCode(statusCodeOf(resp))
  def statusCodeOf(resp: Resp): Int
  def contentStringOf(resp: Resp): String
  def contentBytesOf(resp: Resp): Array[Byte]
  def contentTypeOf(resp: Resp): Option[String]
  def httpResponseOf(resp: Resp): HttpResponse[Resp]
}

trait HttpResponse[Resp] {
  protected def adapter: HttpResponseAdapter[Resp]

  def status: HttpStatus          = adapter.statusOf(toRaw)
  def statusCode: Int             = adapter.statusCodeOf(toRaw)
  def contentString: String       = adapter.contentStringOf(toRaw)
  def contentBytes: Array[Byte]   = adapter.contentBytesOf(toRaw)
  def contentType: Option[String] = adapter.contentTypeOf(toRaw)

  def toHttpResponse: HttpResponse[Resp] = adapter.httpResponseOf(toRaw)
  def toRaw: Resp
}

case class SimpleHttpRequest(
    override val method: HttpMethod,
    override val path: String,
    override val header: Map[String, String] = Map.empty,
    override val query: Map[String, String] = Map.empty,
    override val contentString: String = ""
) extends HttpRequest[SimpleHttpRequest] {
  override protected def adapter: HttpRequestAdapter[SimpleHttpRequest] = SimpleHttpRequestAdapter
  override def contentBytes: Array[Byte]                                = contentString.getBytes(StandardCharsets.UTF_8)
  override def contentType                                              = None
  override def toRaw: SimpleHttpRequest                                 = this
}

object SimpleHttpRequest {
  implicit object SimpleHttpRequestAdapter extends HttpRequestAdapter[SimpleHttpRequest] {
    override def methodOf(request: SimpleHttpRequest): HttpMethod          = request.method
    override def pathOf(request: SimpleHttpRequest): String                = request.path
    override def queryOf(request: SimpleHttpRequest): Map[String, String]  = request.query
    override def headerOf(request: SimpleHttpRequest): Map[String, String] = request.header
    override def contentStringOf(request: SimpleHttpRequest): String       = request.contentString
    override def contentBytesOf(request: SimpleHttpRequest): Array[Byte]   = request.contentBytes
    override def contentTypeOf(request: SimpleHttpRequest): Option[String] = request.contentType
    override def httpRequestOf(request: SimpleHttpRequest): HttpRequest[SimpleHttpRequest] = {
      request
    }
    override def requestType: Class[SimpleHttpRequest] = classOf[SimpleHttpRequest]
  }
}

case class SimpleHttpResponse(
    override val status: HttpStatus,
    private val contentStr: String = "",
    private val content: Array[Byte] = Array.empty,
    override val contentType: Option[String] = None
) extends HttpResponse[SimpleHttpResponse] {

  override def contentString: String = {
    if(contentStr.nonEmpty) {
      contentStr
    }
    else {
      if(content.nonEmpty) {
        new String(content, StandardCharsets.UTF_8)
      }
      else {
        ""
      }
    }
  }

  def getContentBytes: Array[Byte] = {
      if(contentStr.nonEmpty) {
        contentStr.getBytes(StandardCharsets.UTF_8)
      }
      else if(content.nonEmpty) {
        content
      }
      else {
        Array.emptyByteArray
      }
  }

  override protected def adapter: HttpResponseAdapter[SimpleHttpResponse] = SimpleHttpResponseAdapter
  override def toRaw: SimpleHttpResponse                                  = this
}

object SimpleHttpResponse {
  implicit object SimpleHttpResponseAdapter extends HttpResponseAdapter[SimpleHttpResponse] {
    override def statusCodeOf(resp: SimpleHttpResponse): Int       = resp.status.code
    override def contentStringOf(resp: SimpleHttpResponse): String = resp.contentString
    override def contentBytesOf(resp: SimpleHttpResponse): Array[Byte] = resp.getContentBytes
    override def contentTypeOf(resp: SimpleHttpResponse): Option[String]                    = resp.contentType
    override def httpResponseOf(resp: SimpleHttpResponse): HttpResponse[SimpleHttpResponse] = resp
  }
}
