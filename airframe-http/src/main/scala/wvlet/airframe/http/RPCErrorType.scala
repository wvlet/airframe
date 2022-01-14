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

sealed trait RPCErrorType extends PackSupport {
  def name: String = toString()
  override def pack(p: Packer): Unit = {
    p.packString(name)
  }
}

object RPCErrorType {
  // User-input related errors
  case object USER_ERROR extends RPCErrorType
  // Server internal failures, which are usually retryable
  case object INTERNAL_ERROR extends RPCErrorType
  // The resource has been exhausted or a per-user quota has reached
  case object RESOURCE_ERROR extends RPCErrorType

  def all: Seq[RPCErrorType] = Seq(USER_ERROR, INTERNAL_ERROR, RESOURCE_ERROR)

  def unapply(s: String): Option[RPCErrorType] = {
    val name = s.toUpperCase()
    all.find(_.toString == name)
  }
}

/*
object StandardErrorCode {
  import RPCErrorType._

  abstract class UserError     extends ErrorCode(USER_ERROR)
  abstract class InternalError extends ErrorCode(INTERNAL_ERROR)
  abstract class ResourceError extends ErrorCode(RESOURCE_ERROR)

  case object GENERIC_USER_ERROR extends UserError
  // Invalid RPC requests
  case object INVALID_REQUEST  extends UserError
  case object INVALID_ARGUMENT extends UserError
  case object SYNTAX_ERROR     extends UserError

  /**
 * Used for describing index out of bounds error, number overflow, underflow, scaling errors of the user inputs, etc.
 */
  case object OUT_OF_RANGE extends UserError

  // Invalid RPC target
  case object NOT_FOUND      extends UserError
  case object ALREADY_EXISTS extends UserError
  case object NOT_SUPPORTED  extends UserError
  case object UNIMPLEMENTED  extends UserError

  // Invalid service state
  case object UNEXPECTED_STATE   extends UserError
  case object INCONSISTENT_STATE extends UserError

  // Auth
  case object UNAUTHENTICATED   extends UserError
  case object PERMISSION_DENIED extends UserError

  // Request error
  case object CANCELLED   extends UserError
  case object INTERRUPTED extends UserError

  // Internal errors
  case object GENERIC_INTERNAL_ERROR extends InternalError
  /*
 * Unknown error will be categorized as an internal error
 */
  case object UNKNOWN extends InternalError

  /**
 * When the service is currently unavailable, e.g., circuit breaker is open, the service is down, etc.
 */
  case object UNAVAILABLE       extends InternalError
  case object DEADLINE_EXCEEDED extends InternalError
  case object CONFLICT          extends InternalError
  case object ABORTED           extends InternalError

  /**
 * Data is corrupted
 */
  case object DATA_CORRUPTED extends InternalError

  // Resource errors
  case object GENERIC_RESOURCE_ERROR extends ResourceError
  case object TOO_MANY_REQUESTS      extends ResourceError
  case object RESOURCE_EXHAUSTED     extends ResourceError
}
 */
