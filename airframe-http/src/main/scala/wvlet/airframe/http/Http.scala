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

object Http {

  def request(method: String, uri: String)      = HttpMessage.Request.empty.withMethod(method).withUri(uri)
  def request(uri: String): HttpMessage.Request = request(HttpMethod.GET, uri)
  def GET(uri: String)                          = request(HttpMethod.GET, uri)
  def POST(uri: String)                         = request(HttpMethod.POST, uri)
  def DELETE(uri: String)                       = request(HttpMethod.DELETE, uri)
  def PUT(uri: String)                          = request(HttpMethod.PUT, uri)
  def PATCH(uri: String)                        = request(HttpMethod.PATCH, uri)

  def response(status: HttpStatus = HttpStatus.Ok_200) = HttpMessage.Response.empty.withStatus(status)
}

/**
  * Type class to bridge the original requests
  *
  * @tparam Req
  */
trait HttpRequestAdapter[Req] {
  def requestType: Class[Req]

  def methodOf(request: Req): String
  def pathOf(request: Req): String
  def queryOf(request: Req): HttpMultiMap
  def headerOf(request: Req): HttpMultiMap
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

  def method: String        = adapter.methodOf(toRaw)
  def path: String          = adapter.pathOf(toRaw)
  def query: HttpMultiMap   = adapter.queryOf(toRaw)
  def header: HttpMultiMap  = adapter.headerOf(toRaw)
  def contentString: String = adapter.contentStringOf(toRaw)
  // TODO Support streams
  def contentBytes: Array[Byte]          = adapter.contentBytesOf(toRaw)
  def contentType: Option[String]        = adapter.contentTypeOf(toRaw)
  def pathComponents: IndexedSeq[String] = adapter.pathComponentsOf(toRaw)
  def toHttpRequest: HttpRequest[Req]    = adapter.httpRequestOf(toRaw)
  def toRaw: Req
}

/**
  * A type class to bridge the original response type and HttpResponse
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
