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
package wvlet.airframe.http.internal

import wvlet.airframe.http.{RPCContext, EmptyRPCContext}

object LocalRPCContext {
  private val localContext = new ThreadLocal[RPCContext]()
  private val rootContext  = EmptyRPCContext

  def current: RPCContext = {
    Option(localContext.get()).getOrElse(rootContext)
  }

  /**
    * Attach a new RPC context and return the previous context
    */
  def attach(newContext: RPCContext): RPCContext = {
    val prev = current
    localContext.set(newContext)
    prev
  }
  def detach(previousContext: RPCContext): Unit = {
    if (previousContext != rootContext) {
      localContext.set(previousContext)
    } else {
      // Avoid preserving the root thread information in the TLS
      localContext.set(null)
    }
  }
}
