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

case class RPCException(
    // Application-specific error code
    errorCode: ErrorCode,
    // Error message
    message: String,
    // Cause of the exception
    cause: Option[Throwable] = None,
    // Custom data
    metadata: Map[String, Any] = Map.empty
) extends Exception(s"[${errorCode}] ${message}", cause.getOrElse(null)) {
  def withMessage(newMessage: String): RPCException = this.copy(message = newMessage)
}

/**
  * A base class for defining custom ErrorCode.
  */
sealed abstract class ErrorCode(
    val errorType: ErrorType
) {
  def name: String = toString()
}

sealed trait ErrorType {
  def name: String = toString()
}

object ErrorType {
  // User-input related errors
  case object USER_ERROR extends ErrorType
  // Server internal failures, which are usually retryable
  case object INTERNAL_ERROR extends ErrorType
  // The resource has been exhausted or a per-user quota has reached
  case object RESOURCE_ERROR extends ErrorType

  def all: Seq[ErrorType] = Seq(USER_ERROR, INTERNAL_ERROR, RESOURCE_ERROR)

  def unapply(s: String): Option[ErrorType] = {
    val name = s.toUpperCase()
    all.find(_.toString == name)
  }
}

object StandardErrorCode {
  import ErrorType._

  abstract class UserError     extends ErrorCode(USER_ERROR)
  abstract class InternalError extends ErrorCode(INTERNAL_ERROR)
  abstract class ResourceError extends ErrorCode(RESOURCE_ERROR)

  case object GENERIC_USER_ERROR extends UserError
  // Invalid RPC requests
  case object INVALID_REQUEST  extends UserError
  case object INVALID_ARGUMENT extends UserError
  case object SYNTAX_ERROR     extends UserError
  case object NOT_SUPPORTED    extends UserError

  // Invalid RPC target
  case object NOT_FOUND      extends UserError
  case object ALREADY_EXISTS extends UserError
  case object OUT_OF_RANGE   extends UserError

  // Invalid service state
  case object INCONSISTENT_STATE extends UserError

  // Auth
  case object UNAUTHENTICATED   extends UserError
  case object PERMISSION_DENIED extends UserError

  // Request error
  case object CANCELLED extends UserError

  // Internal errors
  case object GENERIC_INTERNAL_ERROR extends InternalError
  case object UNKNOWN_INTERNAL_ERROR extends InternalError
  case object UNAVAILABLE            extends InternalError
  case object DEADLINE_EXCEEDED      extends InternalError
  case object CONFLICT               extends InternalError
  case object ABORTED                extends InternalError

  // Resource errors
  case object GENERIC_RESOURCE_ERROR extends ResourceError
  case object TOO_MANY_REQUESTS      extends ResourceError
  case object RESOURCE_EXHAUSTED     extends ResourceError
  case object REACHED_QUOTA          extends ResourceError

}
