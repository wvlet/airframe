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

import wvlet.log.LogSupport

case class ServerSentEvent(
    id: Option[String] = None,
    event: Option[String] = None,
    retry: Option[Long] = None,
    // event data string. If multiple data entries are reported, concatenated with newline
    data: String
)

object ServerSentEventHandler {
  def empty: ServerSentEventHandler = new ServerSentEventHandler {
    override def onEvent(e: ServerSentEvent): Unit = {}
  }
}

trait ServerSentEventHandler extends LogSupport {
  def onConnect(response: HttpMessage.Response): Unit = {}
  def onEvent(e: ServerSentEvent): Unit
  def onError(e: Throwable): Unit = {
    error(e)
  }
  def onCompletion(): Unit = {}
}
