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

import wvlet.airframe.codec.PackSupport
import wvlet.airframe.msgpack.spi.Packer

/**
  * RPCException provides a backend-independent (e.g., Finagle or gRPC) RPC error reporting mechanism.
  *
  * @param rpcError
  */
class RPCException(
    rpcError: RPCError
) extends Exception(rpcError.toString, rpcError.cause.getOrElse(null))

case class RPCError(
    // Error message
    message: String,
    // Cause of the exception
    cause: Option[Throwable] = None,
    // Application-specific error code
    errorCode: Option[RPCErrorCode],
    // HTTP status code
    httpStatus: Option[HttpStatus] = None,
    // gRPC specific error code
    grpcStatus: Option[GrpcStatus] = None,
    // Custom data
    metadata: Map[String, Any] = Map.empty
) {
  def statusCodeString: String = {
    errorCode
      .map(c => s"${c.name}")
      .orElse(httpStatus.map(s => s"${s.code}:${s.reason}"))
      .orElse(grpcStatus.map(s => s"${s.code}:${s.name}"))
      .getOrElse("unknown")
  }

  override def toString: String                             = s"[${statusCodeString}] ${message}"
  def asException: RPCException                             = new RPCException(this)
  def withMessage(newMessage: String): RPCError             = this.copy(message = newMessage)
  def withMetadata(newMetadata: Map[String, Any]): RPCError = this.copy(metadata = newMetadata)
}

/**
  * A base class for defining application-specific error code
  */
trait RPCErrorCode extends PackSupport {
  // Error type (user, internal, or resource)
  def errorType: RPCErrorType
  // Unique error code name. This name will be used for serde
  def name: String
  // Mapping to an HTTP status code (required)
  def httpStatus: HttpStatus

  // Description of the error
  def description: String

  override def pack(p: Packer): Unit = {
    p.packString(name)
  }
}
