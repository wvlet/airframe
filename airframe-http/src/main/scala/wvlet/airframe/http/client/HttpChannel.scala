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

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.ServerAddress
import wvlet.airframe.rx.Rx

import scala.concurrent.duration.Duration

/**
  * Contains only http channel related configurations in HttpClientConfig
  */
trait HttpChannelConfig {
  def connectTimeout: Duration
  def readTimeout: Duration
}

/**
  * A low-level interface for sending HTTP requests without managing retries nor filters. This interface abstracts away
  * the backend implementation (e.g., Java Http client, Ajax client, OkHttp client, etc)
  */
trait HttpChannel extends AutoCloseable {

  /**
    * The default destination address to send requests
    * @return
    */
  def destination: ServerAddress

  /**
    * Send the request as is to the destination
    * @param req
    * @param channelConfig
    * @return
    */
  def send(req: Request, channelConfig: HttpChannelConfig): Response

  /**
    * Send an async request as is to the destination. Until the returned Rx is evaluated (e.g., by calling Rx.run), the
    * request is not sent.
    *
    * For SSE (Server-Sent Events) requests, the returned [[Response.events]] will have an Rx stream of
    * [[ServerSentEvent]]
    * @param req
    * @param channelConfig
    * @return
    */
  def sendAsync(req: Request, channelConfig: HttpChannelConfig): Rx[Response]

}
