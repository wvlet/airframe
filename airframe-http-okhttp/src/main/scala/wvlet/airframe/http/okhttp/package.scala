package wvlet.airframe.http

import okhttp3.{Headers, MediaType, Request, Response}
import okio.{Buffer, BufferedSink}
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, EmptyMessage, LazyByteArrayMessage, Message}
import wvlet.log.LogSupport

import java.io.EOFException
import scala.jdk.CollectionConverters._

package object okhttp {

  private[okhttp] val ContentTypeJson = MediaType.get("application/json;charset=utf-8")

  implicit class OkHttpRequestWrapper(val raw: Request) extends HttpRequest[Request] {
    override protected def adapter: HttpRequestAdapter[Request] = OkHttpRequestAdapter
    override def toRaw: Request                                 = raw
  }

  implicit object OkHttpRequestAdapter extends HttpRequestAdapter[Request] {
    override def methodOf(request: Request): String = toHttpMethod(request.method())
    override def uriOf(request: Request): String = {
      val url  = request.url()
      val path = url.encodedPath()
      Option(url.encodedQuery()).map(query => s"${path}?${query}").getOrElse(path)
    }
    override def pathOf(request: Request): String = request.url().encodedPath()
    override def headerOf(request: Request): HttpMultiMap = {
      var h = toHttpMultiMap(request.headers())
      // OkHttp may place Content-Type and Content-Length headers separately from headers()
      for (b <- Option(request.body)) {
        Option(b.contentType()).foreach { x => h += HttpHeader.ContentType -> x.toString }
        Option(b.contentLength()).foreach { x => h += HttpHeader.ContentLength -> x.toString }
      }
      h
    }
    override def queryOf(request: Request): HttpMultiMap = {
      val m = HttpMultiMap.newBuilder
      (0 until request.url().querySize()).map { i =>
        m += request.url().queryParameterName(i) -> request.url().queryParameterValue(i)
      }
      m.result()
    }
    override def messageOf(request: Request): Message = {
      new LazyByteArrayMessage({
        val sink: BufferedSink = new Buffer()
        Option(request.body()).foreach(_.writeTo(sink))
        sink.getBuffer().readByteArray()
      })
    }
    override def contentTypeOf(request: Request): Option[String] = Option(request.body()).map(_.contentType().toString)
    override def wrap(request: Request): HttpRequest[Request]    = OkHttpRequestWrapper(request)
    override def requestType: Class[Request]                     = classOf[Request]
    override def remoteAddressOf(request: Request): Option[ServerAddress] = None
  }

  implicit class OkHttpResponseWrapper(val raw: okhttp3.Response) extends HttpResponse[okhttp3.Response] {
    override protected def adapter: HttpResponseAdapter[Response] = OkHttpResponseAdapter
    override def toRaw: Response                                  = raw
  }

  implicit object OkHttpResponseAdapter extends HttpResponseAdapter[okhttp3.Response] with LogSupport {
    override def statusCodeOf(res: okhttp3.Response): Int = {
      res.code()
    }
    override def messageOf(resp: okhttp3.Response): Message = {
      try {
        Option(resp.body()).map(_.bytes()) match {
          case Some(bytes) => ByteArrayMessage(bytes)
          case _           => EmptyMessage
        }
      } catch {
        case e: EOFException =>
          // Reading okhttp response may fail with EOFException
          EmptyMessage
      }
    }
    override def contentTypeOf(res: okhttp3.Response): Option[String] = Option(res.body()).map(_.contentType().toString)
    override def wrap(resp: okhttp3.Response): HttpResponse[okhttp3.Response] = OkHttpResponseWrapper(resp)
    override def headerOf(resp: okhttp3.Response): HttpMultiMap = {
      var h = toHttpMultiMap(resp.headers())
      // OkHttp may place Content-Type and Content-Length headers separately from headers()
      for (b <- Option(resp.body)) {
        Option(b.contentType()).foreach { x => h += HttpHeader.ContentType -> x.toString }
        Option(b.contentLength()).foreach { x => h += HttpHeader.ContentLength -> x.toString }
      }
      h
    }
  }

  private def toHttpMultiMap(h: Headers): HttpMultiMap = {
    val m = HttpMultiMap.newBuilder
    for ((k, lst) <- h.toMultimap.asScala; v <- lst.asScala) {
      m += k -> v
    }
    m.result()
  }

  private[okhttp] def toHttpMethod(method: String): String =
    method match {
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
