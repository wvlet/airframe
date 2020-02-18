package wvlet.airframe.http

import okhttp3.{MediaType, Response}

package object okhttp {

  private[okhttp] val ContentTypeJson = MediaType.get("application/json;charset=utf-8")

  implicit class OkHttpResponse(val raw: Response) extends HttpResponse[Response] {
    override protected def adapter: HttpResponseAdapter[Response] = OkHttpResponseAdapter
    override def toRaw: Response                                  = raw
  }

  implicit object OkHttpResponseAdapter extends HttpResponseAdapter[Response] {
    override def statusCodeOf(res: Response): Int = res.statusCode
    override def contentStringOf(res: Response): String = res.contentString
    override def contentBytesOf(res: Response): Array[Byte] = res.contentBytes
    override def contentTypeOf(res: Response): Option[String] = res.contentType
    override def httpResponseOf(resp: Response): HttpResponse[Response] = OkHttpResponse(resp)
  }

}
