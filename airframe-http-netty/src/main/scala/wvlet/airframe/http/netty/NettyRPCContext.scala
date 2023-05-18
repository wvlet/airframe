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
package wvlet.airframe.http.netty

import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.RPCContext

class NettyRPCContext(val httpRequest: Request) extends RPCContext {
  override def setThreadLocal[A](key: String, value: A): Unit = {
    NettyBackend.setThreadLocal(key, value)
  }
  override def getThreadLocal[A](key: String): Option[A] = {
    NettyBackend.getThreadLocal(key)
  }
}
