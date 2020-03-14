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
  def request(uri: String): HttpMessage.Request = HttpMessage.Request.empty.withUri(uri)
  def request(method: HttpMethod, uri: String)  = HttpMessage.Request.empty.withMethod(method).withUri(uri)
}

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
  def header: Map[String, String] = adapter.headerOf(toRaw)
  def contentString: String       = adapter.contentStringOf(toRaw)
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
