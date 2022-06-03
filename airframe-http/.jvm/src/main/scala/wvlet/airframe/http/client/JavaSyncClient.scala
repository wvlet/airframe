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

import wvlet.airframe.http._
import wvlet.log.LogSupport

/**
  * Http client implementation using a new Java Http Client since Java 11.
  * @param serverAddress
  * @param config
  */
class JavaSyncClient(protected val channel: JavaClientChannel, protected val config: HttpClientConfig) extends client.SyncClient with LogSupport { self =>

  override protected def build(config: HttpClientConfig): SyncClient = {
    new JavaSyncClient(channel, config)
  }

  override def close(): Unit = {
    channel.close()
  }
}
