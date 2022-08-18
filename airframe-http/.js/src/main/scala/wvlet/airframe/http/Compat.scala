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

import org.scalajs.dom.window
import wvlet.airframe.http.client.{HttpClientBackend, JSHttpClientBackend}

import scala.concurrent.ExecutionContext

/**
  * Scala.js specific implementation
  */
private object Compat extends CompatApi {
  override def urlEncode(s: String): String = {
    scala.scalajs.js.URIUtils.encodeURI(s)
  }

  override def hostServerAddress: ServerAddress = {
    try {
      // Look up the local browser address
      val protocol = window.location.protocol.stripSuffix(":")
      val hostname = window.location.hostname
      // For localhost, no server address is required
      if (hostname == "localhost" && protocol == "http") {
        ServerAddress.empty
      } else {
        // For web servers, need to provide the server address
        val port = Option(window.location.port)
          .map(x =>
            if (x.isEmpty) ""
            else s":${x}"
          ).getOrElse("")
        ServerAddress(s"${protocol}://${hostname}${port}")
      }
    } catch {
      // dom.window might be null in Node.js environment
      case e: Throwable =>
        ServerAddress.empty
    }
  }

  override def defaultHttpClientBackend: HttpClientBackend = JSHttpClientBackend

  override def defaultExecutionContext: ExecutionContext = {
    org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
  }

  override def currentRPCContext: RPCContext                     = ???
  override def attachRPCContext(context: RPCContext): RPCContext = ???
  override def detachRPCContext(previous: RPCContext): Unit      = ???
}
