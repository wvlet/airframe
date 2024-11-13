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

import wvlet.airframe.http.client.HttpClientBackend

import scala.concurrent.ExecutionContext

/**
  * Scala Native specific implementation
  */
private object Compat extends CompatApi {
  override def urlEncode(s: String): String                = ???
  override def defaultHttpClientBackend: HttpClientBackend = ???

  override def defaultExecutionContext: ExecutionContext                      = ???
  override def defaultHttpClientLoggerFactory: HttpLoggerConfig => HttpLogger = ???
  override def currentRPCContext: RPCContext                                  = ???
  override def attachRPCContext(context: RPCContext): RPCContext              = ???
  override def detachRPCContext(previous: RPCContext): Unit                   = ???
}
