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
import wvlet.airframe.msgpack.spi.{Packer, Unpacker, ValueType}

import scala.util.Try

sealed abstract class GrpcStatus(
    // gRPC status code
    val code: Int,
    // Mapping to an HTTP Status code (2xx - 5xx)
    val httpStatus: HttpStatus
) extends PackSupport {

  /**
    * The string representation of this status code
    */
  val name: String = {
    // Trim _(status code) suffix
    val s = toString()
    s.lastIndexOf('_') match {
      case -1  => s
      case pos => s.substring(0, pos)
    }
  }

  override def pack(packer: Packer): Unit = {
    packer.packInt(code)
  }
}

/**
  * gRPC error code definitions in https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto
  */
object GrpcStatus {

  def all: Seq[GrpcStatus] = Seq(
    GrpcStatus.OK_0,
    GrpcStatus.CANCELLED_1,
    GrpcStatus.UNKNOWN_2,
    GrpcStatus.INVALID_ARGUMENT_3,
    GrpcStatus.DEADLINE_EXCEEDED_4,
    GrpcStatus.NOT_FOUND_5,
    GrpcStatus.ALREADY_EXISTS_6,
    GrpcStatus.PERMISSION_DENIED_7,
    GrpcStatus.RESOURCE_EXHAUSTED_8,
    GrpcStatus.FAILED_PRECONDITION_9,
    GrpcStatus.ABORTED_10,
    GrpcStatus.OUT_OF_RANGE_11,
    GrpcStatus.UNIMPLEMENTED_12,
    GrpcStatus.INTERNAL_13,
    GrpcStatus.UNAVAILABLE_14,
    GrpcStatus.DATA_LOSS_15,
    GrpcStatus.UNAUTHENTICATED_16
  )

  // Mapping from an integer code to GrpcStatus object
  private val codeTable = all.map(x => x.code -> x).toMap

  def ofCode(code: Int): GrpcStatus =
    codeTable.getOrElse(code, throw new IllegalArgumentException(s"Unknown gRPC code: ${code}"))

  def unpack(u: Unpacker): Option[GrpcStatus] = {
    u.getNextValueType match {
      case ValueType.INTEGER =>
        val code = u.unpackInt
        codeTable.get(code)
      case ValueType.FLOAT =>
        Try(u.unpackFloat.toInt).toOption.flatMap(codeTable.get(_))
      case ValueType.STRING =>
        val s = u.unpackString
        Try(s.toInt).toOption.flatMap(codeTable.get(_))
      case _ =>
        None
    }
  }

  // Not an error; returned on success
  case object OK_0 extends GrpcStatus(code = 0, HttpStatus.Ok_200)

  // The operation was cancelled, typically by the caller.
  case object CANCELLED_1 extends GrpcStatus(code = 1, HttpStatus.ClientClosedRequest_499)

  // Unknown error.  For example, this error may be returned when
  // a `Status` value received from another address space belongs to
  // an error space that is not known in this address space.  Also
  // errors raised by APIs that do not return enough error information
  // may be converted to this error.
  case object UNKNOWN_2 extends GrpcStatus(code = 2, HttpStatus.InternalServerError_500)

  // The client specified an invalid argument.  Note that this differs
  // from `FAILED_PRECONDITION`.  `INVALID_ARGUMENT` indicates arguments
  // that are problematic regardless of the state of the system
  // (e.g., a malformed file name).
  case object INVALID_ARGUMENT_3 extends GrpcStatus(code = 3, HttpStatus.BadRequest_400)

  // The deadline expired before the operation could complete. For operations
  // that change the state of the system, this error may be returned
  // even if the operation has completed successfully.  For example, a
  // successful response from a server could have been delayed long
  // enough for the deadline to expire.
  case object DEADLINE_EXCEEDED_4 extends GrpcStatus(code = 4, HttpStatus.GatewayTimeout_504)

  // Some requested entity (e.g., file or directory) was not found.

  // Note to server developers: if a request is denied for an entire class
  // of users, such as gradual feature rollout or undocumented whitelist,
  // `NOT_FOUND` may be used. If a request is denied for some users within
  // a class of users, such as user-based access control, `PERMISSION_DENIED`
  // must be used.

  // HTTP Mapping: 404 Not Found
  case object NOT_FOUND_5 extends GrpcStatus(code = 5, HttpStatus.NotFound_404)

  // The entity that a client attempted to create (e.g., file or directory)
  // already exists.
  case object ALREADY_EXISTS_6 extends GrpcStatus(code = 6, HttpStatus.Conflict_409)

  // The caller does not have permission to execute the specified
  // operation. `PERMISSION_DENIED` must not be used for rejections
  // caused by exhausting some resource (use `RESOURCE_EXHAUSTED`
  // instead for those errors). `PERMISSION_DENIED` must not be
  // used if the caller can not be identified (use `UNAUTHENTICATED`
  // instead for those errors). This error code does not imply the
  // request is valid or the requested entity exists or satisfies
  // other pre-conditions.
  case object PERMISSION_DENIED_7 extends GrpcStatus(code = 7, HttpStatus.Forbidden_403)

  // The request does not have valid authentication credentials for the
  // operation.
  case object UNAUTHENTICATED_16 extends GrpcStatus(code = 16, HttpStatus.Unauthorized_401)

  // Some resource has been exhausted, perhaps a per-user quota, or
  // perhaps the entire file system is out of space.
  case object RESOURCE_EXHAUSTED_8 extends GrpcStatus(code = 8, HttpStatus.TooManyRequests_429)

  // The operation was rejected because the system is not in a state
  // required for the operation's execution.  For example, the directory
  // to be deleted is non-empty, an rmdir operation is applied to
  // a non-directory, etc.

  // Service implementors can use the following guidelines to decide
  // between `FAILED_PRECONDITION`, `ABORTED`, and `UNAVAILABLE`:
  //  (a) Use `UNAVAILABLE` if the client can retry just the failing call.
  //  (b) Use `ABORTED` if the client should retry at a higher level
  //      (e.g., when a client-specified test-and-set fails, indicating the
  //      client should restart a read-modify-write sequence).
  //  (c) Use `FAILED_PRECONDITION` if the client should not retry until
  //      the system state has been explicitly fixed.  E.g., if an "rmdir"
  //      fails because the directory is non-empty, `FAILED_PRECONDITION`
  //      should be returned since the client should not retry unless
  //      the files are deleted from the directory.
  //
  // Note: This is intentionally mapped to BadRequest_400 https://github.com/grpc-ecosystem/grpc-gateway/issues/972
  //
  case object FAILED_PRECONDITION_9 extends GrpcStatus(code = 9, HttpStatus.BadRequest_400)

  // The operation was aborted, typically due to a concurrency issue such as
  // a sequencer check failure or transaction abort.

  // See the guidelines above for deciding between `FAILED_PRECONDITION`,
  // `ABORTED`, and `UNAVAILABLE`.
  case object ABORTED_10 extends GrpcStatus(code = 10, HttpStatus.Conflict_409)

  // The operation was attempted past the valid range.  E.g., seeking or
  // reading past end-of-file.

  // Unlike `INVALID_ARGUMENT`, this error indicates a problem that may
  // be fixed if the system state changes. For example, a 32-bit file
  // system will generate `INVALID_ARGUMENT` if asked to read at an
  // offset that is not in the range [0,2^32-1], but it will generate
  // `OUT_OF_RANGE` if asked to read from an offset past the current
  // file size.

  // There is a fair bit of overlap between `FAILED_PRECONDITION` and
  // `OUT_OF_RANGE`.  We recommend using `OUT_OF_RANGE` (the more specific
  // error) when it applies so that callers who are iterating through
  // a space can easily look for an `OUT_OF_RANGE` error to detect when
  // they are done.
  case object OUT_OF_RANGE_11 extends GrpcStatus(code = 11, HttpStatus.BadRequest_400)

  // The operation is not implemented or is not supported/enabled in this
  // service. Intentionally mapping this to MethodNotAllowed_405 (non-retryable), instead of using a similar NotImplemented_501
  // so as not to make it a retryable error.
  case object UNIMPLEMENTED_12 extends GrpcStatus(code = 12, HttpStatus.MethodNotAllowed_405)

  // Internal errors.  This means that some invariants expected by the
  // underlying system have been broken.  This error code is reserved
  // for serious errors.
  case object INTERNAL_13 extends GrpcStatus(code = 13, HttpStatus.InternalServerError_500)

  // The service is currently unavailable.  This is most likely a
  // transient condition, which can be corrected by retrying with
  // a backoff. Note that it is not always safe to retry
  case object UNAVAILABLE_14 extends GrpcStatus(code = 14, HttpStatus.ServiceUnavailable_503)

  // Unrecoverable data loss or corruption.
  case object DATA_LOSS_15 extends GrpcStatus(code = 15, HttpStatus.InternalServerError_500)

  /**
    * Mapping table from HTTP status to gRPC status code. This table is used for inferring grpc status when a
    * HTTPServerException is thrown within gRPC backend servers.
    */
  private val httpStatusCodeMapping = Map[HttpStatus, GrpcStatus](
    HttpStatus.Unknown_000                       -> UNKNOWN_2,
    HttpStatus.Continue_100                      -> UNKNOWN_2,
    HttpStatus.SwitchingProtocols_101            -> UNKNOWN_2,
    HttpStatus.Processing_102                    -> UNKNOWN_2,
    HttpStatus.Ok_200                            -> OK_0,
    HttpStatus.Created_201                       -> OK_0,
    HttpStatus.Accepted_202                      -> OK_0,
    HttpStatus.NonAuthoritativeInformation_203   -> OK_0,
    HttpStatus.NoContent_204                     -> OK_0,
    HttpStatus.ResetContent_205                  -> OK_0,
    HttpStatus.PartialContent_206                -> OK_0,
    HttpStatus.MultiStatus_207                   -> OK_0,
    HttpStatus.MultipleChoices_300               -> NOT_FOUND_5,
    HttpStatus.MovedPermanently_301              -> NOT_FOUND_5,
    HttpStatus.Found_302                         -> NOT_FOUND_5,
    HttpStatus.SeeOther_303                      -> NOT_FOUND_5,
    HttpStatus.NotModified_304                   -> NOT_FOUND_5,
    HttpStatus.UseProxy_305                      -> NOT_FOUND_5,
    HttpStatus.TemporaryRedirect_307             -> NOT_FOUND_5,
    HttpStatus.PermanentRedirect_308             -> NOT_FOUND_5,
    HttpStatus.BadRequest_400                    -> INVALID_ARGUMENT_3,
    HttpStatus.Unauthorized_401                  -> UNAUTHENTICATED_16,
    HttpStatus.PaymentRequired_402               -> RESOURCE_EXHAUSTED_8,
    HttpStatus.Forbidden_403                     -> PERMISSION_DENIED_7,
    HttpStatus.NotFound_404                      -> NOT_FOUND_5,
    HttpStatus.MethodNotAllowed_405              -> UNIMPLEMENTED_12,
    HttpStatus.NotAcceptable_406                 -> PERMISSION_DENIED_7,
    HttpStatus.ProxyAuthenticationRequired_407   -> UNAUTHENTICATED_16,
    HttpStatus.RequestTimeout_408                -> DEADLINE_EXCEEDED_4,
    HttpStatus.Conflict_409                      -> ABORTED_10,
    HttpStatus.Gone_410                          -> NOT_FOUND_5,
    HttpStatus.LengthRequired_411                -> INVALID_ARGUMENT_3,
    HttpStatus.PreconditionFailed_412            -> FAILED_PRECONDITION_9,
    HttpStatus.RequestEntityTooLarge_413         -> INVALID_ARGUMENT_3,
    HttpStatus.RequestURITooLong_414             -> INVALID_ARGUMENT_3,
    HttpStatus.UnsupportedMediaType_415          -> UNIMPLEMENTED_12,
    HttpStatus.RequestedRangeNotSatisfiable_416  -> OUT_OF_RANGE_11,
    HttpStatus.ExpectationFailed_417             -> FAILED_PRECONDITION_9,
    HttpStatus.EnhanceYourCalm_420               -> RESOURCE_EXHAUSTED_8,
    HttpStatus.UnprocessableEntity_422           -> INVALID_ARGUMENT_3,
    HttpStatus.Locked_423                        -> FAILED_PRECONDITION_9,
    HttpStatus.FailedDependency_424              -> FAILED_PRECONDITION_9,
    HttpStatus.UnorderedCollection_425           -> INVALID_ARGUMENT_3,
    HttpStatus.UpgradeRequired_426               -> FAILED_PRECONDITION_9,
    HttpStatus.PreconditionRequired_428          -> FAILED_PRECONDITION_9,
    HttpStatus.TooManyRequests_429               -> RESOURCE_EXHAUSTED_8,
    HttpStatus.RequestHeaderFieldsTooLarge_431   -> INVALID_ARGUMENT_3,
    HttpStatus.UnavailableForLegalReasons_451    -> PERMISSION_DENIED_7,
    HttpStatus.ClientClosedRequest_499           -> CANCELLED_1,
    HttpStatus.InternalServerError_500           -> INTERNAL_13,
    HttpStatus.NotImplemented_501                -> UNIMPLEMENTED_12,
    HttpStatus.BadGateway_502                    -> UNAVAILABLE_14,
    HttpStatus.ServiceUnavailable_503            -> UNAVAILABLE_14,
    HttpStatus.GatewayTimeout_504                -> DEADLINE_EXCEEDED_4,
    HttpStatus.HttpVersionNotSupported_505       -> UNIMPLEMENTED_12,
    HttpStatus.VariantAlsoNegotiates_506         -> INTERNAL_13,
    HttpStatus.InsufficientStorage_507           -> RESOURCE_EXHAUSTED_8,
    HttpStatus.NotExtended_510                   -> INTERNAL_13,
    HttpStatus.NetworkAuthenticationRequired_511 -> UNAUTHENTICATED_16
  )

  // Mapping HTTP status code to gRPC status
  def ofHttpStatus(status: HttpStatus): GrpcStatus = {
    httpStatusCodeMapping.get(status) match {
      case Some(grpcStatus) => grpcStatus
      case None =>
        status match {
          case s if s.isSuccessful =>
            OK_0
          case s if s.isClientError =>
            INVALID_ARGUMENT_3
          case s if s.isServerError =>
            INTERNAL_13
          case _ =>
            UNKNOWN_2
        }
    }
  }

}
