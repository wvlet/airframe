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
  */
case class RPCException(
    // RPC status
    status: RPCStatus,
    // Error message
    message: String = "",
    // Cause of the exception
    cause: Option[Throwable] = None,
    // [optional] Application-specific status code
    appErrorCode: Option[Int] = None,
    // [optional] Application-specific metadata
    metadata: Map[String, Any] = Map.empty
) extends Exception(s"[${status}] ${message}", cause.getOrElse(null))
