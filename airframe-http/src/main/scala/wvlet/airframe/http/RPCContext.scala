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

import wvlet.airframe.http.Compat
import wvlet.airframe.http.internal.RPCCallContext

object RPCContext {

  /**
    * * Get the current thread-local RPCContext
    * @since 22.8.0
    * @return
    */
  def current: RPCContext = {
    Compat.asInstanceOf[CompatApi].currentRPCContext
  }
}

trait RPCContext {

  /**
    * Return the original http request RPC server received. This request may not contain the full ruquest body for the
    * performance reason.
    */
  def httpRequest: HttpMessage.Request

  def rpcCallContext: Option[RPCCallContext] = {
    getThreadLocal(HttpBackend.TLS_KEY_RPC) match {
      case Some(c: RPCCallContext) => Some(c)
      case _                       => None
    }
  }

  /**
    * Set a thread-local variable that is available only within the request scope.
    * @param key
    * @param value
    * @tparam A
    */
  def setThreadLocal[A](key: String, value: A): Unit

  /**
    * Get a thread-local variable that is available only within the request scope. The type must be specified
    * explicitly.
    * @param key
    * @return
    */
  @deprecated("Use getThreadLocal(key: String): Any instead", "24.5.0")
  def getThreadLocalUnsafe[A](key: String): Option[A] = {
    getThreadLocal(key).map(_.asInstanceOf[A])
  }

  /**
    * Get a thread-local variable that is available only within the request scope.
    * @param key
    * @return
    */
  def getThreadLocal(key: String): Option[Any]
}

/**
  * An empty RPCContext
  */
object EmptyRPCContext extends RPCContext {
  override def setThreadLocal[A](key: String, value: A): Unit = {
    // no-op
  }
  override def getThreadLocal(key: String): Option[Any] = {
    // no-op
    None
  }
  override def httpRequest: HttpMessage.Request = {
    throw RPCStatus.UNIMPLEMENTED_U8.newException(
      "RPCContext.httpRequest is not available outside the context of RPC call"
    )
  }
}
