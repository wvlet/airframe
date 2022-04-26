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
  * Define the standard RPC error code that can be used for generic RPC service implementation.
  *
  * This covers all gRPC error codes and have pre-defined mappings to HTTP status (4xx, 5xx) code.
  *
  * If you need an application-specific error code, it can be defined as an additional argument of the RPCError class.
  */
object RPCErrorCode {

  import RPCErrorType._

  // def ofCode(code: Int): Option[RPCErrorCode] = {}

  def all: Seq[RPCErrorCode] =
    userErrors ++ internalErrors ++ resourceErrors

  def userErrors: Seq[RPCErrorCode] = Seq(
    USER_ERROR_U0,
    INVALID_REQUEST_U1,
    INVALID_ARGUMENT_U2,
    SYNTAX_ERROR_U3,
    OUT_OF_RANGE_U4,
    NOT_FOUND_U5,
    ALREADY_EXISTS_U6,
    NOT_SUPPORTED_U7,
    UNIMPLEMENTED_U8,
    UNEXPECTED_STATE_U9,
    INCONSISTENT_STATE_U10
  )

  def internalErrors: Seq[RPCErrorCode] = Seq(
    INTERNAL_ERROR_I0,
    UNAVAILABLE_I1,
    TIMEOUT_I2,
    DEADLINE_EXCEEDED_I3,
    INTERRUPTED_I4,
    SERVICE_STARTING_UP_I5,
    SERVICE_SHUTTING_DOWN_I6,
    DATA_LOSS_I7
  )

  def resourceErrors: Seq[RPCErrorCode] = Seq(
    RESOURCE_EXHAUSTED_R0,
    OUT_OF_MEMORY_R1,
    EXCEEDED_RATE_LIMIT_R2,
    EXCEEDED_CPU_LIMIT_R3,
    EXCEEDED_MEMORY_LIMIT_R4,
    EXCEEDED_TIME_LIMIT_R5,
    EXCEEDED_DATA_SIZE_LIMIT_R6,
    EXCEEDED_STORAGE_LIMIT_R7,
    EXCEEDED_BUDGET_R8
  )

  case object USER_ERROR_U0 extends RPCErrorCode(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Invalid RPC request. The user should not retry the request in general.
    */
  case object INVALID_REQUEST_U1 extends RPCErrorCode(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * RPC request arguments have invalid values
    */
  case object INVALID_ARGUMENT_U2 extends RPCErrorCode(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Syntax error in an RPC argument
    */
  case object SYNTAX_ERROR_U3 extends RPCErrorCode(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Invalid range data is given to an RPC request argument.
    */
  case object OUT_OF_RANGE_U4 extends RPCErrorCode(USER_ERROR, GrpcStatus.OUT_OF_RANGE_11)

  /**
    * The requested resource or RPC method is not found
    */
  case object NOT_FOUND_U5 extends RPCErrorCode(USER_ERROR, GrpcStatus.NOT_FOUND_5)

  /**
    * The resource creation request failed because it already exists.
    */
  case object ALREADY_EXISTS_U6 extends RPCErrorCode(USER_ERROR, GrpcStatus.ALREADY_EXISTS_6)

  /**
    * The requested RPC method is not supported.
    */
  case object NOT_SUPPORTED_U7 extends RPCErrorCode(USER_ERROR, GrpcStatus.UNIMPLEMENTED_12)

  /**
    * The requested RPC method is not implemented.
    */
  case object UNIMPLEMENTED_U8 extends RPCErrorCode(USER_ERROR, GrpcStatus.UNIMPLEMENTED_12)

  /**
    * Some precondition to succeed this request is not met (e.g., invalid configuration). The client should not retry
    * the request until fixing the state.
    */
  case object UNEXPECTED_STATE_U9 extends RPCErrorCode(USER_ERROR, GrpcStatus.FAILED_PRECONDITION_9)

  /**
    * The service or the use has an inconsistent state and cannot fulfill the request. The client should not retry the
    * request until fixing the state.
    */
  case object INCONSISTENT_STATE_U10 extends RPCErrorCode(USER_ERROR, GrpcStatus.FAILED_PRECONDITION_9)

  /**
    * The request was cancelled, typically by the client. The client should not retry the request unless it's a network
    * issue.
    */
  case object CANCELLED_U11 extends RPCErrorCode(USER_ERROR, GrpcStatus.CANCELLED_1)

  /**
    * The request is aborted (e.g., dead-lock, transaction conflicts, etc.) The client should retry the request it a
    * higher-level.
    */
  case object ABORTED_U12 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.ABORTED_10)

  /**
    * The user has not been authenticated
    */
  case object UNAUTHENTICATED_U13 extends RPCErrorCode(USER_ERROR, GrpcStatus.UNAUTHENTICATED_16)

  /**
    * The user does not have a permission to access the resource
    */
  case object PERMISSION_DENIED_U14 extends RPCErrorCode(USER_ERROR, GrpcStatus.PERMISSION_DENIED_7)

  /**
    * Internal failures where the user can retry the request in general
    */
  case object INTERNAL_ERROR_I0 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.INTERNAL_13)

  /**
    * The service is unavailable.
    */
  case object UNAVAILABLE_I1 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * The service respond the request in time (e.g., circuit breaker is open, timeout exceeded, etc.) For operations
    * that change the system state, this error might be returned even if the operation has completed successfully.
    */
  case object TIMEOUT_I2 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.DEADLINE_EXCEEDED_4)

  /**
    * The request cannot be processed in the user-specified deadline. The client may retry the request
    */
  case object DEADLINE_EXCEEDED_I3 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.DEADLINE_EXCEEDED_4)

  /**
    * The request is interrupted at the service
    */
  case object INTERRUPTED_I4 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.INTERNAL_13)

  /**
    * The service is starting now. The client can retry the request after a while
    */
  case object SERVICE_STARTING_UP_I5 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * The service is shutting down now.
    */
  case object SERVICE_SHUTTING_DOWN_I6 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * Data loss or corrupted data
    */
  case object DATA_LOSS_I7 extends RPCErrorCode(INTERNAL_ERROR, GrpcStatus.DATA_LOSS_15)

  /**
    * The resource for completing the request is insufficient.
    */
  case object RESOURCE_EXHAUSTED_R0 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The service is experiencing insufficient memory
    */
  case object OUT_OF_MEMORY_R1 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * There are too many requests. The user needs to retry the request after a while
    */
  case object EXCEEDED_RATE_LIMIT_R2 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its CPU usage limit
    */
  case object EXCEEDED_CPU_LIMIT_R3 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its memory usage limit
    */
  case object EXCEEDED_MEMORY_LIMIT_R4 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its running time limit
    */
  case object EXCEEDED_TIME_LIMIT_R5 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its data size limit
    */
  case object EXCEEDED_DATA_SIZE_LIMIT_R6 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its storage size limit
    */
  case object EXCEEDED_STORAGE_LIMIT_R7 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has exhausted the budget for processing the request.
    */
  case object EXCEEDED_BUDGET_R8 extends RPCErrorCode(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * Extracting the error code from the name
    */
  private def extractErrorCode(name: String): Int = {
    val separatorPos = name.lastIndexOf("_")
    separatorPos match {
      case -1 =>
        throw new AssertionError(s"Invalid error code name ${name}. Error codes must end with (U/I/R)[0-9]+")
      case pos =>
        val suffix = name.substring(pos + 1)
        if (suffix.length < 2) {
          throw new AssertionError(
            s"Invalid error code suffix ${name}. Error codes must have a suffix (U/I/R)[0-9]+"
          )
        }

        try {
          val errorCode = suffix.substring(1).toInt
          suffix.charAt(0) match {
            case 'U' =>
              // user errors
              RPCErrorType.USER_ERROR.errorCodeMin + errorCode
            case 'I' =>
              // internal errors
              RPCErrorType.INTERNAL_ERROR.errorCodeMin + errorCode
            case 'R' =>
              // resource errors
              RPCErrorType.RESOURCE_EXHAUSTED.errorCodeMin + errorCode
            case other =>
              throw new AssertionError(s"The error code: ${name} has an invalid suffix: ${other}")
          }
        } catch {
          case e: NumberFormatException =>
            throw new AssertionError(
              s"Invalid error code suffix ${name}. Error codes must have integer suffixes: ${e.getMessage}"
            )
        }
    }
  }
}

/**
  * A base class for defining standard RPC error codes
  */
sealed abstract class RPCErrorCode(
    // Error type (user, internal, or resource)
    val errorType: RPCErrorType,
    // Mapping to an gRPC status code
    val grpcStatus: GrpcStatus
) extends PackSupport {
  assert(errorType.isValidErrorCode(code), s"Error code ${code} is invalid for ${errorType} error type")
  assert(errorType.isValidHttpStatus(httpStatus), s"Unexpected http status ${httpStatus} for the error code: ${name}")

  import RPCErrorCode._

  /**
    * Integer-based error code
    */
  lazy val code: Int = extractErrorCode(name)

  /**
    * The error code name.
    */
  def name: String = this.toString()

  /**
    * http status code derived from grpc status code
    */
  def httpStatus: HttpStatus = grpcStatus.httpStatus

  override def pack(p: Packer): Unit = {
    p.packInt(code)
  }
}
