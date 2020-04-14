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
  * Exception to report errors to client
  */
case class HttpServerException(private var response: HttpMessage.Response, cause: Throwable)
    extends Exception(response.contentString, cause)
    with HttpMessage[HttpServerException] {
  def this(status: HttpStatus, message: String, cause: Throwable) = this(Http.response(status, message), cause)
  def this(status: HttpStatus, message: String) = this(status, message, null)
  def this(status: HttpStatus) = this(status, status.toString, null)

  def status: HttpStatus = response.status
  def statusCode: Int    = response.statusCode

  override def header: HttpMultiMap         = response.header
  override def message: HttpMessage.Message = response.message

  override protected def copyWith(newHeader: HttpMultiMap): HttpServerException = {
    // Do not create a copy to retain the original stack trace
    response = response.withHeader(newHeader)
    this
  }
  override protected def copyWith(newMessage: HttpMessage.Message): HttpServerException = {
    // Do not create a copy to retain the original stack trace
    response = response.withContent(newMessage)
    this
  }

  def toResponse: HttpMessage.Response = response
}
