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

/**
  * Http request/response data type definitions
  */
object HttpMessage {

  case class Request(
      method: HttpMethod = HttpMethod.GET,
      uri: String = "/",
      header: HttpHeader = HttpHeader.empty
  ) {
    def path: String = {
      val u = uri
      u.indexOf("?") match {
        case -1  => u
        case pos => u.substring(0, pos)
      }
    }

    def withMethod(method: HttpMethod): Request = {
      this.copy(method = method)
    }

  }

  object Request {
    val empty: Request = Request()
  }

}
