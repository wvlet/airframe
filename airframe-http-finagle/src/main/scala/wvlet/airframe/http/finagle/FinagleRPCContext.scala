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
package wvlet.airframe.http.finagle

import com.twitter.finagle.http.Request
import wvlet.airframe.http.{HttpMessage, RPCContext}

/**
  * Finagle-specific RPCContext implementation, which leverages Finagle's own TLS
  */
case class FinagleRPCContext(request: Request) extends RPCContext {
  override def setThreadLocal[A](key: String, value: A): Unit = {
    FinagleBackend.setThreadLocal(key, value)
  }

  override def getThreadLocal[A](key: String): Option[A] = {
    FinagleBackend.getThreadLocal(key)
  }

  override def httpRequest: HttpMessage.Request = {
    request.toHttpRequest
  }
}
