package wvlet.airframe.http

import okhttp3.{MediaType, Request, Response}
import okio.{Buffer, BufferedSink}

import scala.jdk.CollectionConverters._

package object okhttp {

  private[okhttp] val ContentTypeJson = MediaType.get("application/json;charset=utf-8")

  implicit class OkHttpRequest(val raw: Request) extends HttpRequest[Request] {
    override protected def adapter: HttpRequestAdapter[Request] = OkHttpRequestAdapter
    override def toRaw: Request                                 = raw
  }

  implicit object OkHttpRequestAdapter extends HttpRequestAdapter[Request] {
    override def methodOf(request: Request): String = toHttpMethod(request.method())
    override def pathOf(request: Request): String   = request.url().encodedPath()
    override def headerOf(request: Request): HttpMultiMap = {
      val m = HttpMultiMap.newBuilder
      for ((k, lst) <- request.headers().toMultimap.asScala; v <- lst.asScala) {
        m += k -> v
      }
      m.result()
    }
    override def queryOf(request: Request): HttpMultiMap = {
      val m = HttpMultiMap.newBuilder
      (0 until request.url().querySize()).map { i =>
        m += request.url().queryParameterName(i) -> request.url().queryParameterValue(i)
      }
      m.result()
    }
    override def contentStringOf(request: Request): String = {
      val sink: BufferedSink = new Buffer()
      Option(request.body()).foreach(_.writeTo(sink))
      sink.buffer().readUtf8()
    }
    override def contentBytesOf(request: Request): Array[Byte] = {
      val sink: BufferedSink = new Buffer()
      Option(request.body()).foreach(_.writeTo(sink))
      sink.buffer().readByteArray()
    }
    override def contentTypeOf(request: Request): Option[String]       = Option(request.body()).map(_.contentType().toString)
    override def httpRequestOf(request: Request): HttpRequest[Request] = OkHttpRequest(request)
    override def requestType: Class[Request]                           = classOf[Request]
  }

  implicit class OkHttpResponse(val raw: Response) extends HttpResponse[Response] {
    override protected def adapter: HttpResponseAdapter[Response] = OkHttpResponseAdapter
    override def toRaw: Response                                  = raw
  }

  implicit object OkHttpResponseAdapter extends HttpResponseAdapter[Response] {
    override def statusCodeOf(res: Response): Int                       = res.code()
    override def contentStringOf(res: Response): String                 = Option(res.body()).map(_.string()).getOrElse("")
    override def contentBytesOf(res: Response): Array[Byte]             = Option(res.body()).map(_.bytes()).getOrElse(Array.empty)
    override def contentTypeOf(res: Response): Option[String]           = Option(res.body()).map(_.contentType().toString)
    override def httpResponseOf(resp: Response): HttpResponse[Response] = OkHttpResponse(resp)
  }

  private[okhttp] def toHttpMethod(method: String): String = method match {
    case "GET"     => HttpMethod.GET
    case "POST"    => HttpMethod.POST
    case "PUT"     => HttpMethod.PUT
    case "PATCH"   => HttpMethod.PATCH
    case "DELETE"  => HttpMethod.DELETE
    case "OPTIONS" => HttpMethod.OPTIONS
    case "HEAD"    => HttpMethod.HEAD
    case "TRACE"   => HttpMethod.TRACE
    case _         => throw new IllegalArgumentException(s"Unsupported method: ${method}")
  }

}
