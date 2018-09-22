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
import wvlet.airframe.Design
import wvlet.airframe.http.finagle.FinagleServer.FinagleService

/**
  *
  */
package object finagle {

  def finagleDefaultDesign: Design =
    httpDefaultDesign
      .bind[ResponseHandler[http.Request, http.Response]].to[FinagleResponseHandler]
      .bind[FinagleRouter].toSingleton
      .bind[FinagleService].toProvider { router: FinagleRouter =>
        FinagleServer.defaultService(router)
      }

  implicit class FinagleHttpRequest(val raw: http.Request) extends HttpRequest[http.Request] {
    def asAirframeHttpRequest: HttpRequest[http.Request] = this
    override def toRaw                                   = raw
    override def method: HttpMethod                      = toHttpMethod(raw.method)
    override def path: String                            = raw.path
    override def query: Map[String, String]              = raw.params
    override def contentString: String                   = raw.contentString
    override def contentBytes: Array[Byte] = {
      val size = raw.content.length
      val b    = new Array[Byte](size)
      raw.content.write(b, 0)
      b
    }
  }

  private[finagle] def toHttpMethod(method: http.Method): HttpMethod = {
    method match {
      case http.Method.Get    => HttpMethod.GET
      case http.Method.Post   => HttpMethod.POST
      case http.Method.Put    => HttpMethod.PUT
      case http.Method.Delete => HttpMethod.DELETE
      case other              => throw new IllegalArgumentException(s"Unsupporeted method: ${method}")
    }
  }
}
