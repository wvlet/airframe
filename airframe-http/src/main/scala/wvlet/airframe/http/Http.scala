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

import java.nio.charset.StandardCharsets

trait HttpRequest[Req] {
  def method: HttpMethod
  def path: String
  def query: Map[String, String]
  def contentString: String
  def contentBytes: Array[Byte]
  def contentType: Option[String]
  lazy val pathComponents: IndexedSeq[String] = {
    path.replaceFirst("/", "").split("/").toIndexedSeq
  }
  def toRaw: Req
}

case class SimpleHttpRequest(method: HttpMethod,
                             path: String,
                             query: Map[String, String] = Map.empty,
                             contentString: String = "")
    extends HttpRequest[SimpleHttpRequest] {
  override def contentBytes: Array[Byte] = {
    contentString.getBytes(StandardCharsets.UTF_8)
  }
  def contentType: Option[String]       = None
  override def toRaw: SimpleHttpRequest = this
}

trait HttpResponse[Resp] {
  def statusCode: Int
  def contentString: String
  def contentBytes: Array[Byte]
  def contentType: Option[String]
  def toRaw: Resp
}
