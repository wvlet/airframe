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
import wvlet.airframe.msgpack.spi.Value.{IntegerValue, LongValue, StringValue}
import wvlet.airframe.msgpack.spi.{Packer, Value}

import scala.util.Try

/**
  * Define the standard RPC code that can be used for generic RPC service implementation.
  *
  * This covers all gRPC statuses and have pre-defined mappings to HTTP status (4xx, 5xx) code.
  *
  * If you need an application-specific error code, use an additional argument of the RPCError class.
  */
object RPCStatus {

  import RPCStatusType._

  // These variables need to be lazy to avoid NPE during the initialization
  private lazy val codeTable: Map[Int, RPCStatus]        = all.map { x => x.code -> x }.toMap
  private lazy val codeNameTable: Map[String, RPCStatus] = all.map { x => x.name -> x }.toMap
  private lazy val grpcStatusCodeTable: Map[Int, RPCStatus] = grpcStatusCodeMapping.map { case (g, r) =>
    g.code -> r
  }.toMap

  // Default mapping from gRPC status code
  private lazy val grpcStatusCodeMapping = Map(
    GrpcStatus.OK_0                  -> RPCStatus.SUCCESS_S0,
    GrpcStatus.CANCELLED_1           -> RPCStatus.CANCELLED_U11,
    GrpcStatus.UNKNOWN_2             -> RPCStatus.UNKNOWN_I1,
    GrpcStatus.INVALID_ARGUMENT_3    -> RPCStatus.INVALID_ARGUMENT_U2,
    GrpcStatus.DEADLINE_EXCEEDED_4   -> RPCStatus.DEADLINE_EXCEEDED_I4,
    GrpcStatus.NOT_FOUND_5           -> RPCStatus.NOT_FOUND_U5,
    GrpcStatus.ALREADY_EXISTS_6      -> RPCStatus.ALREADY_EXISTS_U6,
    GrpcStatus.PERMISSION_DENIED_7   -> RPCStatus.PERMISSION_DENIED_U14,
    GrpcStatus.RESOURCE_EXHAUSTED_8  -> RPCStatus.RESOURCE_EXHAUSTED_R0,
    GrpcStatus.FAILED_PRECONDITION_9 -> RPCStatus.UNEXPECTED_STATE_U9,
    GrpcStatus.ABORTED_10            -> RPCStatus.ABORTED_U12,
    GrpcStatus.OUT_OF_RANGE_11       -> RPCStatus.OUT_OF_RANGE_U4,
    GrpcStatus.UNIMPLEMENTED_12      -> RPCStatus.UNIMPLEMENTED_U8,
    GrpcStatus.INTERNAL_13           -> RPCStatus.INTERNAL_ERROR_I0,
    GrpcStatus.UNAVAILABLE_14        -> RPCStatus.UNAVAILABLE_I2,
    GrpcStatus.DATA_LOSS_15          -> RPCStatus.DATA_LOSS_I8,
    GrpcStatus.UNAUTHENTICATED_16    -> RPCStatus.UNAUTHENTICATED_U13
  )

  import HttpStatus._
  private lazy val httpStatusMapping: Map[HttpStatus, RPCStatus] = Map(
    Unknown_000                       -> UNKNOWN_I1,
    Continue_100                      -> UNKNOWN_I1,
    SwitchingProtocols_101            -> UNKNOWN_I1,
    Processing_102                    -> UNKNOWN_I1,
    Ok_200                            -> SUCCESS_S0,
    Created_201                       -> SUCCESS_S0,
    Accepted_202                      -> SUCCESS_S0,
    NonAuthoritativeInformation_203   -> SUCCESS_S0,
    NoContent_204                     -> SUCCESS_S0,
    ResetContent_205                  -> SUCCESS_S0,
    PartialContent_206                -> SUCCESS_S0,
    MultiStatus_207                   -> SUCCESS_S0,
    MultipleChoices_300               -> NOT_FOUND_U5,
    MovedPermanently_301              -> NOT_FOUND_U5,
    Found_302                         -> NOT_FOUND_U5,
    SeeOther_303                      -> NOT_FOUND_U5,
    NotModified_304                   -> NOT_FOUND_U5,
    UseProxy_305                      -> NOT_FOUND_U5,
    TemporaryRedirect_307             -> NOT_FOUND_U5,
    PermanentRedirect_308             -> NOT_FOUND_U5,
    BadRequest_400                    -> INVALID_REQUEST_U1,
    Unauthorized_401                  -> UNAUTHENTICATED_U13,
    PaymentRequired_402               -> EXCEEDED_BUDGET_R8,
    Forbidden_403                     -> PERMISSION_DENIED_U14,
    NotFound_404                      -> NOT_FOUND_U5,
    MethodNotAllowed_405              -> NOT_SUPPORTED_U7,
    NotAcceptable_406                 -> NOT_SUPPORTED_U7,
    ProxyAuthenticationRequired_407   -> UNAUTHENTICATED_U13,
    RequestTimeout_408                -> DEADLINE_EXCEEDED_I4,
    Conflict_409                      -> ABORTED_U12,
    Gone_410                          -> NOT_FOUND_U5,
    LengthRequired_411                -> INVALID_ARGUMENT_U2,
    PreconditionFailed_412            -> UNEXPECTED_STATE_U9,
    RequestEntityTooLarge_413         -> INVALID_ARGUMENT_U2,
    RequestURITooLong_414             -> INVALID_ARGUMENT_U2,
    UnsupportedMediaType_415          -> NOT_SUPPORTED_U7,
    RequestedRangeNotSatisfiable_416  -> OUT_OF_RANGE_U4,
    ExpectationFailed_417             -> UNEXPECTED_STATE_U9,
    EnhanceYourCalm_420               -> RESOURCE_EXHAUSTED_R0,
    UnprocessableEntity_422           -> INVALID_REQUEST_U1,
    Locked_423                        -> UNEXPECTED_STATE_U9,
    FailedDependency_424              -> UNEXPECTED_STATE_U9,
    UnorderedCollection_425           -> INVALID_ARGUMENT_U2,
    UpgradeRequired_426               -> UNEXPECTED_STATE_U9,
    PreconditionRequired_428          -> UNEXPECTED_STATE_U9,
    TooManyRequests_429               -> RESOURCE_EXHAUSTED_R0,
    RequestHeaderFieldsTooLarge_431   -> INVALID_ARGUMENT_U2,
    UnavailableForLegalReasons_451    -> PERMISSION_DENIED_U14,
    ClientClosedRequest_499           -> CANCELLED_U11,
    InternalServerError_500           -> INTERNAL_ERROR_I0,
    NotImplemented_501                -> UNIMPLEMENTED_U8,
    BadGateway_502                    -> UNAVAILABLE_I2,
    ServiceUnavailable_503            -> UNAVAILABLE_I2,
    GatewayTimeout_504                -> DEADLINE_EXCEEDED_I4,
    HttpVersionNotSupported_505       -> NOT_SUPPORTED_U7,
    VariantAlsoNegotiates_506         -> INTERNAL_ERROR_I0,
    InsufficientStorage_507           -> EXCEEDED_STORAGE_LIMIT_R7,
    NotExtended_510                   -> INTERNAL_ERROR_I0,
    NetworkAuthenticationRequired_511 -> UNAUTHENTICATED_U13
  )

  def unapply(s: String): Option[RPCStatus] = {
    Try(ofCode(s.toInt)).toOption
  }

  def unapply(v: Value): Option[RPCStatus] = {
    v match {
      case l: LongValue =>
        Try(ofCode(l.asInt)).toOption
      case s: StringValue =>
        Try(ofCodeName(s.toString))
          .orElse(Try(ofCode(s.toString.toInt)))
          .toOption
      case _ =>
        None
    }
  }

  def ofCode(code: Int): RPCStatus = {
    codeTable.getOrElse(code, throw new IllegalArgumentException(s"Invalid RPCStatus code: ${code}"))
  }

  def ofCodeName(name: String): RPCStatus = {
    codeNameTable.getOrElse(name, throw new IllegalArgumentException(s"Invalid RPCStatus name: ${name}"))
  }

  def fromGrpcStatusCode(grpcStatusCode: Int): RPCStatus = {
    grpcStatusCodeTable.getOrElse(
      grpcStatusCode,
      throw new IllegalArgumentException(s"Invalid gRPC status code: ${grpcStatusCode}")
    )
  }

  /**
    * Mapping HttpStatus to RPCStatus. This is useful when mapping third-party API statuses into RPCStatus
    */
  def fromHttpStatus(httpStatus: HttpStatus): RPCStatus = {
    httpStatusMapping.get(httpStatus) match {
      case Some(status) => status
      case _ =>
        if (httpStatus.isSuccessful) {
          RPCStatus.SUCCESS_S0
        } else if (httpStatus.isClientError) {
          RPCStatus.USER_ERROR_U0
        } else if (httpStatus.isServerError) {
          RPCStatus.INTERNAL_ERROR_I0
        } else {
          RPCStatus.UNKNOWN_I1
        }
    }
  }

  def all: Seq[RPCStatus] =
    successes ++ userErrors ++ internalErrors ++ resourceErrors

  private def successes: Seq[RPCStatus] = Seq(
    SUCCESS_S0
  )

  private def userErrors: Seq[RPCStatus] = Seq(
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
    INCONSISTENT_STATE_U10,
    CANCELLED_U11,
    ABORTED_U12,
    UNAUTHENTICATED_U13,
    PERMISSION_DENIED_U14
  )

  private def internalErrors: Seq[RPCStatus] = Seq(
    INTERNAL_ERROR_I0,
    UNKNOWN_I1,
    UNAVAILABLE_I2,
    TIMEOUT_I3,
    DEADLINE_EXCEEDED_I4,
    INTERRUPTED_I5,
    SERVICE_STARTING_UP_I6,
    SERVICE_SHUTTING_DOWN_I7,
    DATA_LOSS_I8
  )

  private def resourceErrors: Seq[RPCStatus] = Seq(
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

  case object SUCCESS_S0 extends RPCStatus(SUCCESS, GrpcStatus.OK_0)

  case object USER_ERROR_U0 extends RPCStatus(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Invalid RPC request. The user should not retry the request in general.
    */
  case object INVALID_REQUEST_U1 extends RPCStatus(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * RPC request arguments have invalid values
    */
  case object INVALID_ARGUMENT_U2 extends RPCStatus(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Syntax error in an RPC argument
    */
  case object SYNTAX_ERROR_U3 extends RPCStatus(USER_ERROR, GrpcStatus.INVALID_ARGUMENT_3)

  /**
    * Invalid range data is given to an RPC request argument.
    */
  case object OUT_OF_RANGE_U4 extends RPCStatus(USER_ERROR, GrpcStatus.OUT_OF_RANGE_11)

  /**
    * The requested resource or RPC method is not found
    */
  case object NOT_FOUND_U5 extends RPCStatus(USER_ERROR, GrpcStatus.NOT_FOUND_5)

  /**
    * The resource creation request failed because it already exists.
    */
  case object ALREADY_EXISTS_U6 extends RPCStatus(USER_ERROR, GrpcStatus.ALREADY_EXISTS_6)

  /**
    * The requested RPC method is not supported.
    */
  case object NOT_SUPPORTED_U7 extends RPCStatus(USER_ERROR, GrpcStatus.UNIMPLEMENTED_12)

  /**
    * The requested RPC method is not implemented.
    */
  case object UNIMPLEMENTED_U8 extends RPCStatus(USER_ERROR, GrpcStatus.UNIMPLEMENTED_12)

  /**
    * Some precondition to succeed this request is not met (e.g., invalid configuration). The client should not retry
    * the request until fixing the state.
    */
  case object UNEXPECTED_STATE_U9 extends RPCStatus(USER_ERROR, GrpcStatus.FAILED_PRECONDITION_9)

  /**
    * The service or the use has an inconsistent state and cannot fulfill the request. The client should not retry the
    * request until fixing the state.
    */
  case object INCONSISTENT_STATE_U10 extends RPCStatus(USER_ERROR, GrpcStatus.FAILED_PRECONDITION_9)

  /**
    * The request was cancelled, typically by the client. The client should not retry the request unless it's a network
    * issue.
    */
  case object CANCELLED_U11 extends RPCStatus(USER_ERROR, GrpcStatus.CANCELLED_1)

  /**
    * The request is aborted (e.g., dead-lock, transaction conflicts, etc.) The client should retry the request it a
    * higher-level.
    */
  case object ABORTED_U12 extends RPCStatus(USER_ERROR, GrpcStatus.ABORTED_10)

  /**
    * The user has not been authenticated
    */
  case object UNAUTHENTICATED_U13 extends RPCStatus(USER_ERROR, GrpcStatus.UNAUTHENTICATED_16)

  /**
    * The user does not have a permission to access the resource
    */
  case object PERMISSION_DENIED_U14 extends RPCStatus(USER_ERROR, GrpcStatus.PERMISSION_DENIED_7)

  /**
    * Internal failures where the user can retry the request in general
    */
  case object INTERNAL_ERROR_I0 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.INTERNAL_13)

  /**
    * An unknown internal error
    */
  case object UNKNOWN_I1 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.UNKNOWN_2)

  /**
    * The service is unavailable.
    */
  case object UNAVAILABLE_I2 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * The service respond the request in time (e.g., circuit breaker is open, timeout exceeded, etc.) For operations
    * that change the system state, this error might be returned even if the operation has completed successfully.
    */
  case object TIMEOUT_I3 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.DEADLINE_EXCEEDED_4)

  /**
    * The request cannot be processed in the user-specified deadline. The client may retry the request
    */
  case object DEADLINE_EXCEEDED_I4 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.DEADLINE_EXCEEDED_4)

  /**
    * The request is interrupted at the service
    */
  case object INTERRUPTED_I5 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.INTERNAL_13)

  /**
    * The service is starting now. The client can retry the request after a while
    */
  case object SERVICE_STARTING_UP_I6 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * The service is shutting down now.
    */
  case object SERVICE_SHUTTING_DOWN_I7 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.UNAVAILABLE_14)

  /**
    * Data loss or corrupted data
    */
  case object DATA_LOSS_I8 extends RPCStatus(INTERNAL_ERROR, GrpcStatus.DATA_LOSS_15)

  /**
    * The resource for completing the request is insufficient.
    */
  case object RESOURCE_EXHAUSTED_R0 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The service is experiencing insufficient memory
    */
  case object OUT_OF_MEMORY_R1 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * There are too many requests. The user needs to retry the request after a while
    */
  case object EXCEEDED_RATE_LIMIT_R2 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its CPU usage limit
    */
  case object EXCEEDED_CPU_LIMIT_R3 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its memory usage limit
    */
  case object EXCEEDED_MEMORY_LIMIT_R4 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its running time limit
    */
  case object EXCEEDED_TIME_LIMIT_R5 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its data size limit
    */
  case object EXCEEDED_DATA_SIZE_LIMIT_R6 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has reached its storage size limit
    */
  case object EXCEEDED_STORAGE_LIMIT_R7 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * The user has exhausted the budget for processing the request.
    */
  case object EXCEEDED_BUDGET_R8 extends RPCStatus(RESOURCE_EXHAUSTED, GrpcStatus.RESOURCE_EXHAUSTED_8)

  /**
    * Extracting the error code from the name
    */
  private def extractErrorCode(name: String): Int = {
    val separatorPos = name.lastIndexOf("_")
    separatorPos match {
      case -1 =>
        throw new AssertionError(s"Invalid code name ${name}. It must end with (U/I/R)[0-9]+")
      case pos =>
        val suffix = name.substring(pos + 1)
        if (suffix.length < 2) {
          throw new AssertionError(
            s"Invalid code suffix ${name}. It must have a suffix (U/I/R)[0-9]+"
          )
        }

        try {
          val errorCode  = suffix.substring(1).toInt
          val statusType = RPCStatusType.ofPrefix(suffix.charAt(0))
          statusType.minCode + errorCode
        } catch {
          case e: NumberFormatException =>
            throw new AssertionError(
              s"Invalid code suffix ${name}. It must have an integer suffix: ${e.getMessage}"
            )
        }
    }
  }
}

/**
  * A base class for defining standard RPC error codes
  */
sealed abstract class RPCStatus(
    // Error type (user, internal, or resource)
    val statusType: RPCStatusType,
    // Mapping to an gRPC status code
    val grpcStatus: GrpcStatus
) extends PackSupport {
  assert(statusType.isValidCode(code), s"Status code ${code} is invalid for ${statusType} status type")
  assert(statusType.isValidHttpStatus(httpStatus), s"Unexpected http status ${httpStatus} for the code: ${name}")

  import RPCStatus._

  def isSuccess: Boolean = statusType == RPCStatusType.SUCCESS
  def isFailure: Boolean = !isSuccess

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

  /**
    * Create a new RPCException with this RPCStatus.
    * @param message
    *   the error message (required)
    * @param cause
    *   the cause of the error (optional)
    * @param appErrorCode
    *   application-specific error code. default: -1 (None)
    * @param metadata
    *   application-specific metadata (optional)
    */
  def newException(
      message: String,
      cause: Throwable = null,
      appErrorCode: Int = -1,
      metadata: Map[String, Any] = Map.empty
  ): RPCException = {
    RPCException(
      status = this,
      message = message,
      cause = Option(cause),
      appErrorCode = if (appErrorCode == -1) None else Some(appErrorCode),
      metadata = metadata
    )
  }

}
