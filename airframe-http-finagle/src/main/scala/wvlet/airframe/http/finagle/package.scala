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
import com.twitter.finagle.http.{Request, Response}
import com.twitter.io.Buf.ByteArray
import com.twitter.util.Future
import wvlet.airframe.{Design, Session}
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.log.io.IOUtil

/**
  *
  */
package object finagle {
  type FinagleContext    = HttpContext[Request, Response, Future]
  type FinagleSyncClient = HttpSyncClient[Request, Response]

  /**
    * A design for setting up airframe-http-finagle.
    * If you create your own FinagleServers, use this design.
    */
  def finagleBaseDesign: Design =
    Design.newDesign
    // Define binding here to avoid this will be initialized in a child session
      .bind[FinagleServerFactory].toSingleton
      .bind[FinagleService].toProvider { (config: FinagleServerConfig, session: Session) => config.newService(session) }

  /**
    * The default design for using FinagleServer
    */
  def finagleDefaultDesign: Design =
    finagleBaseDesign
      .bind[FinagleServer].toProvider { (factory: FinagleServerFactory, config: FinagleServerConfig) =>
        factory.newFinagleServer(config)
      }

  def newFinagleServerDesign(config: FinagleServerConfig): Design = {
    finagleDefaultDesign
      .bind[FinagleServerConfig].toInstance(config)
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
      .bind[FinagleClient].toProvider { server: FinagleServer => Finagle.client.newClient(server.localAddress) }
  }

  def finagleSyncClientDesign: Design = {
    Design.newDesign
      .bind[FinagleSyncClient].toProvider { server: FinagleServer => Finagle.client.newSyncClient(server.localAddress) }
  }

  implicit class FinagleHttpRequest(val raw: http.Request) extends HttpRequest[http.Request] {
    override protected def adapter: HttpRequestAdapter[Request] = FinagleHttpRequestAdapter
    override def toRaw: Request                                 = raw
  }

  implicit object FinagleHttpRequestAdapter extends HttpRequestAdapter[http.Request] {
    override def methodOf(request: Request): HttpMethod = toHttpMethod(request.method)
    override def pathOf(request: Request): String       = request.path
    override def headerOf(request: Request): HttpMultiMap = {
      val h = request.headerMap
      var m = HttpMultiMap.empty
      for (k <- h.keys) {
        h.getAll(k).map { v => m = m.add(k, v) }
      }
      m
    }
    override def queryOf(request: Request): HttpMultiMap = {
      val p = request.params
      var m = HttpMultiMap.empty
      for (k <- p.keys) {
        p.getAll(k).map { v => m = m.add(k, v) }
      }
      m
    }
    override def contentStringOf(request: Request): String = request.contentString
    override def contentBytesOf(request: Request): Array[Byte] = {
      val content = request.content
      val size    = content.length
      val b       = new Array[Byte](size)
      content.write(b, 0)
      b
    }
    override def contentTypeOf(request: Request): Option[String]       = request.contentType
    override def httpRequestOf(request: Request): HttpRequest[Request] = FinagleHttpRequest(request)
    override def requestType: Class[Request]                           = classOf[Request]
  }

  implicit class FinagleHttpResponse(val raw: http.Response) extends HttpResponse[http.Response] {
    override protected def adapter: HttpResponseAdapter[Response] = FinagleHttpResponseAdapter
    override def toRaw: Response                                  = raw
  }

  implicit object FinagleHttpResponseAdapter extends HttpResponseAdapter[http.Response] {
    override def statusCodeOf(res: http.Response): Int       = res.statusCode
    override def contentStringOf(res: http.Response): String = res.contentString
    override def contentBytesOf(res: http.Response): Array[Byte] = {
      val c = res.content
      c match {
        case b: ByteArray =>
          ByteArray.Owned.extract(b)
        case _ =>
          val buf = new Array[Byte](c.length)
          c.write(buf, 0)
          buf
      }
    }
    override def contentTypeOf(res: http.Response): Option[String]      = res.contentType
    override def httpResponseOf(resp: Response): HttpResponse[Response] = FinagleHttpResponse(resp)
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
  private val httpMethodMappingReverse = httpMethodMapping.map(x => x._2 -> x._1).toMap

  private[finagle] def toHttpMethod(method: http.Method): HttpMethod = {
    httpMethodMappingReverse.getOrElse(method, throw new IllegalArgumentException(s"Unsupported method: ${method}"))
  }

  private[finagle] def toFinagleHttpMethod(method: HttpMethod): http.Method = {
    httpMethodMapping.getOrElse(method, throw new IllegalArgumentException(s"Unsupported method: ${method}"))
  }
}
