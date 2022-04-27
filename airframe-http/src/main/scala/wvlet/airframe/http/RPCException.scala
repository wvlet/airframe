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

/**
  * RPCException provides a backend-independent (e.g., Finagle or gRPC) RPC error reporting mechanism.
  *
  * @param rpcError
  */
class RPCException(
    rpcError: RPCError
) extends Exception(rpcError.toString, rpcError.cause.getOrElse(null))

case class RPCError(
    // RPC status
    status: RPCStatus,
    // Error message
    message: String,
    // Cause of the exception
    cause: Option[Throwable] = None,
    // Application-specific status code
    applicationStatusCode: Option[Int] = None,
    // Custom data
    metadata: Map[String, Any] = Map.empty
) {
  override def toString: String                             = s"[${status}] ${message}"
  def toException: RPCException                             = new RPCException(this)
  def withMessage(newMessage: String): RPCError             = this.copy(message = newMessage)
  def withMetadata(newMetadata: Map[String, Any]): RPCError = this.copy(metadata = newMetadata)
}
