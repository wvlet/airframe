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

import wvlet.airframe.codec.MessageCodecFactory

/**
  */
trait HttpBase {

  /**
    * Create a new HttpServerException with a custom content-body in JSON or MsgPack format.
    * The content type will be determined by the Accept header in the request
    */
  inline def serverException[A](request: HttpRequest[_], status: HttpStatus, content: A): HttpServerException = {
    {
      val _e = Http.serverException(status)
      if (request.acceptsMsgPack) {
        _e.withContentTypeMsgPack
      }
      else {
        _e
      }
      _e.withContentOf[A](content)
    }
  }

  /**
    * Create a new HttpServerException with a custom content-body in JSON or MsgPack format.
    * The content type will be determined by the Accept header in the request
    */
  def serverException[A](
          request: HttpRequest[_],
          status: HttpStatus,
          content: A,
          codecFactory: MessageCodecFactory
  ): HttpServerException = {
    {
      val _e = Http.serverException(status)
      if (request.acceptsMsgPack) {
        _e.withContentTypeMsgPack
      }
      else {
        _e
      }
      _e.withContentOf[A](content, codecFactory)
    }
  }
}
