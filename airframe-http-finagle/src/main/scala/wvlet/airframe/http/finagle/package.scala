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
import wvlet.airframe.Design
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.log.io.IOUtil

/**
  *
  */
package object finagle {

  private def finagleBaseDesign: Design =
    httpDefaultDesign
      .bind[ResponseHandler[http.Request, http.Response]].to[FinagleResponseHandler]

  def finagleDefaultDesign: Design =
    finagleBaseDesign
      .bind[FinagleService].toProvider { router: FinagleRouter =>
        FinagleServer.defaultService(router)
      }
      .bind[FinagleServer].toProvider { (factory: FinagleServerFactory, config: FinagleServerConfig) =>
        factory.newFinagleServer(config)
      }

  /**
    * Create a new design for FinagleServer using a random port (if not given)
    */
  def newFinagleServerDesign(router: Router, port: Int = IOUtil.randomPort): Design = {
    finagleDefaultDesign
      .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = port, router = router))
  }

  implicit class FinagleHttpRequest(val raw: http.Request) extends HttpRequest[http.Request] {
    override protected def adapter: HttpRequestAdapter[Request] = FinagleHttpRequestAdapter
    override def toRaw: Request                                 = raw
  }

  implicit object FinagleHttpRequestAdapter extends HttpRequestAdapter[http.Request] {
    override def methodOf(request: Request): HttpMethod         = toHttpMethod(request.method)
    override def pathOf(request: Request): String               = request.path
    override def queryOf(request: Request): Map[String, String] = request.params
    override def contentStringOf(request: Request): String      = request.contentString
    override def contentBytesOf(request: Request): Array[Byte] = {
      val content = request.content
      val size    = content.length
      val b       = new Array[Byte](size)
      content.write(b, 0)
      b
    }
    override def contentTypeOf(request: Request): Option[String]       = request.contentType
    override def httpRequestOf(request: Request): HttpRequest[Request] = FinagleHttpRequest(request)
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

  private[finagle] def toHttpMethod(method: http.Method): HttpMethod = {
    method match {
      case http.Method.Get     => HttpMethod.GET
      case http.Method.Post    => HttpMethod.POST
      case http.Method.Put     => HttpMethod.PUT
      case http.Method.Patch   => HttpMethod.PATCH
      case http.Method.Delete  => HttpMethod.DELETE
      case http.Method.Options => HttpMethod.OPTIONS
      case http.Method.Head    => HttpMethod.HEAD
      case http.Method.Trace   => HttpMethod.TRACE
      case other               => throw new IllegalArgumentException(s"Unsupported method: ${method}")
    }
  }
}
