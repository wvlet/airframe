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

import wvlet.airframe.http.HttpClientConfig
import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}

/**
  * An wrapper of JavaHttpSyncClient for supporting async response
  * @param syncClient
  */
class JavaAsyncClient(protected val channel: JavaClientChannel, protected val config: HttpClientConfig) extends AsyncClient {
  override private[client] implicit val executionContext: ExecutionContext = channel.executionContext

  override def send(req: Request): Future[Response] = {
    channel.sendAsync(req, config)
  }

  override def close(): Unit = {
    channel.close()
  }
  override protected def build(config: HttpClientConfig): AsyncClient = {
    new JavaAsyncClient(channel, config)
  }
}
