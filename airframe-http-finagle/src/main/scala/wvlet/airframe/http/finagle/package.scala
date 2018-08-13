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
import wvlet.airframe.http.{HttpMethod, HttpRequest}

/**
  *
  */
package object finagle {
  implicit class FinagleHttpRequest(request: http.Request) extends HttpRequest {
    override def method: HttpMethod         = toHttpMethod(request.method)
    override def path: String               = request.path
    override def query: Map[String, String] = request.params
    override def contentString: String      = request.contentString
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
