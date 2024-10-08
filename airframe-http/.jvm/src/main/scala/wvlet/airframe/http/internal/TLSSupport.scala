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

import scala.collection.mutable

/**
  * Thread-local storage support
  */
private[http] trait TLSSupport {
  private lazy val tls = ThreadLocal.withInitial[mutable.Map[String, Any]](() => mutable.Map.empty[String, Any])
  private def tlsStorage(): mutable.Map[String, Any] = tls.get()

  def setTLS(key: String, value: Any): Unit = tlsStorage().put(key, value)
  def getTLS(key: String): Option[Any]      = tlsStorage().get(key)
  def clearTLS(): Unit                      = tlsStorage().clear()
}
