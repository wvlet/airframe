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
package wvlet.airframe.http.client

import wvlet.airframe.http.ChannelConfig
import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A low-level interface for sending HTTP requests without managing retries or filters
  */
trait HttpChannel extends AutoCloseable {

  /**
    * Send the request without modification.
    * @param req
    * @param channelConfig
    * @return
    */
  def send(req: Request, channelConfig: ChannelConfig): Response
  def sendAsync(req: Request, channelConfig: ChannelConfig): Future[Response]

  private[client] implicit def executionContext: ExecutionContext
}
