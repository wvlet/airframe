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

import com.twitter.finagle.http
import com.twitter.finagle.http.{HeaderMap, Request, Response}
import com.twitter.io.Buf
import com.twitter.io.Buf.ByteArray
import com.twitter.util.Future
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, StringMessage}
import wvlet.airframe.{Design, Session}
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.log.io.IOUtil

/**
  */
package object finagle {
  type FinagleContext    = HttpContext[Request, Response, Future]
  type FinagleSyncClient = HttpSyncClient[Request, Response]

  /**
    * A design for setting up airframe-http-finagle. If you create your own FinagleServers, use this design.
    */
  def finagleBaseDesign: Design =
    Design.newDesign
      // Define binding here to avoid this will be initialized in a child session
      .bind[FinagleServerFactory]
      .toSingleton
      .bind[FinagleService]
      .toProvider { (config: FinagleServerConfig, session: Session) =>
        config.newService(session)
      }

  /**
    * The default design for using FinagleServer
    */
  def finagleDefaultDesign: Design =
    finagleBaseDesign
      .bind[FinagleServer]
      .toProvider { (factory: FinagleServerFactory, config: FinagleServerConfig) =>
        factory.newFinagleServer(config)
      }

  def newFinagleServerDesign(config: FinagleServerConfig): Design = {
    finagleDefaultDesign
      .bind[FinagleServerConfig]
      .toInstance(config)
  }

  /**
    * Create a new design for FinagleServer using a random port (if not given)
    */
  def newFinagleServerDesign(name: String = "default", port: Int = IOUtil.randomPort, router: Router): Design = {
    newFinagleServerDesign(
      FinagleServerConfig()
        .withName(name)
        .withPort(port)
        .withRouter(router)
    )
  }

  def finagleClientDesign: Design = {
    Design.newDesign
      .bind[FinagleClient]
      .toProvider { (server: FinagleServer) =>
        Finagle.client.newClient(server.localAddress)
      }
  }

  def finagleSyncClientDesign: Design = {
    Design.newDesign
      .bind[FinagleSyncClient]
      .toProvider { (server: FinagleServer) =>
        Finagle.client.newSyncClient(server.localAddress)
      }
  }

  implicit class FinagleHttpRequestWrapper(val raw: http.Request) extends HttpRequest[http.Request] {
    override protected def adapter: HttpRequestAdapter[Request] =
      FinagleHttpRequestAdapter
    override def toRaw: Request = raw
  }

  implicit object FinagleHttpRequestAdapter extends HttpRequestAdapter[http.Request] {
    override def methodOf(request: Request): String =
      toHttpMethod(request.method)
    override def pathOf(request: Request): String = request.path
    override def headerOf(request: Request): HttpMultiMap =
      toHttpMultiMap(request.headerMap)
    override def queryOf(request: Request): HttpMultiMap =
      HttpMessage.extractQueryFromUri(request.uri)
    override def messageOf(request: Request): HttpMessage.Message =
      toMessage(request.content)
    override def contentTypeOf(request: Request): Option[String] =
      request.contentType
    override def requestType: Class[Request]     = classOf[Request]
    override def uriOf(request: Request): String = request.uri
    override def wrap(request: Request): HttpRequest[Request] =
      new FinagleHttpRequestWrapper(request)
  }

  private def toMessage(buf: Buf): HttpMessage.Message = {
    buf match {
      case b: ByteArray =>
        new HttpMessage.LazyByteArrayMessage(ByteArray.Owned.extract(b))
      case c =>
        new HttpMessage.LazyByteArrayMessage({
          val buf = new Array[Byte](c.length)
          c.write(buf, 0)
          buf
        })
    }
  }

  implicit class FinagleHttpResponseWrapper(val raw: http.Response) extends HttpResponse[http.Response] {
    override protected def adapter: HttpResponseAdapter[Response] =
      FinagleHttpResponseAdapter
    override def toRaw: Response = raw
  }

  implicit object FinagleHttpResponseAdapter extends HttpResponseAdapter[http.Response] {
    override def statusCodeOf(res: http.Response): Int = res.statusCode
    override def messageOf(resp: http.Response): HttpMessage.Message =
      toMessage(resp.content)
    override def contentTypeOf(res: http.Response): Option[String] =
      res.contentType
    override def headerOf(resp: Response): HttpMultiMap =
      toHttpMultiMap(resp.headerMap)
    override def wrap(resp: Response): HttpResponse[Response] =
      new FinagleHttpResponseWrapper(resp)
  }

  private def toHttpMultiMap(headerMap: HeaderMap): HttpMultiMap = {
    val m = HttpMultiMap.newBuilder
    for (k <- headerMap.keys) {
      headerMap.getAll(k).map { v =>
        m += k -> v
      }
    }
    m.result()
  }

  private val httpMethodMapping = Map(
    HttpMethod.GET     -> http.Method.Get,
    HttpMethod.POST    -> http.Method.Post,
    HttpMethod.PUT     -> http.Method.Put,
    HttpMethod.PATCH   -> http.Method.Patch,
    HttpMethod.DELETE  -> http.Method.Delete,
    HttpMethod.OPTIONS -> http.Method.Options,
    HttpMethod.HEAD    -> http.Method.Head,
    HttpMethod.TRACE   -> http.Method.Trace
  )
  private val httpMethodMappingReverse =
    httpMethodMapping.map(x => x._2 -> x._1).toMap

  private[finagle] def toHttpMethod(method: http.Method): String = {
    httpMethodMappingReverse.getOrElse(method, throw new IllegalArgumentException(s"Unsupported method: ${method}"))
  }

  private[finagle] def toFinagleHttpMethod(method: String): http.Method = {
    httpMethodMapping.getOrElse(method, throw new IllegalArgumentException(s"Unsupported method: ${method}"))
  }

  def convertToFinagleRequest(request: HttpMessage.Request): Request = {
    val req = http.Request()
    for (h <- request.header.entries) {
      req.headerMap.add(h.key, h.value)
    }
    request.message match {
      case StringMessage(content) =>
        req.contentString = content
      case ByteArrayMessage(content) =>
        req.content = Buf.ByteArray.Owned(content)
      case other =>
        req.content = Buf.ByteArray.Owned(other.toContentBytes)
    }
    req
  }

  def convertToFinagleResponse(response: HttpMessage.Response): Response = {
    val resp = http.Response()
    resp.statusCode = response.statusCode
    for (h <- response.header.entries) {
      resp.headerMap.add(h.key, h.value)
    }
    response.message match {
      case StringMessage(content) =>
        resp.contentString = content
      case ByteArrayMessage(content) =>
        resp.content = Buf.ByteArray.Owned(content)
      case other =>
        resp.content = Buf.ByteArray.Owned(other.toContentBytes)
    }
    resp
  }
}
