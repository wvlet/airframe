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
  * gRPC error code definitions in https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto
  */
object GrpcStatus {
  // Not an error; returned on success
  //
  // HTTP Mapping: 200 OK
  case object OK_0 extends GrpcStatus(code = 0, HttpStatus.Ok_200)

  // The operation was cancelled, typically by the caller.

  // HTTP Mapping: 499 Client Closed Request
  case object CANCELLED_1 extends GrpcStatus(code = 1, HttpStatus.ClientClosedRequest_499)

  // Unknown error.  For example, this error may be returned when
  // a `Status` value received from another address space belongs to
  // an error space that is not known in this address space.  Also
  // errors raised by APIs that do not return enough error information
  // may be converted to this error.

  // HTTP Mapping: 500 Internal Server Error
  case object UNKNOWN_2 extends GrpcStatus(code = 2, HttpStatus.InternalServerError_500)

  // The client specified an invalid argument.  Note that this differs
  // from `FAILED_PRECONDITION`.  `INVALID_ARGUMENT` indicates arguments
  // that are problematic regardless of the state of the system
  // (e.g., a malformed file name).

  // HTTP Mapping: 400 Bad Request
  case object INVALID_ARGUMENT_3 extends GrpcStatus(code = 3, HttpStatus.BadRequest_400)

  // The deadline expired before the operation could complete. For operations
  // that change the state of the system, this error may be returned
  // even if the operation has completed successfully.  For example, a
  // successful response from a server could have been delayed long
  // enough for the deadline to expire.

  // HTTP Mapping: 504 Gateway Timeout
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

  // HTTP Mapping: 409 Conflict
  case object ALREADY_EXISTS_6 extends GrpcStatus(code = 6, HttpStatus.Conflict_409)

  // The caller does not have permission to execute the specified
  // operation. `PERMISSION_DENIED` must not be used for rejections
  // caused by exhausting some resource (use `RESOURCE_EXHAUSTED`
  // instead for those errors). `PERMISSION_DENIED` must not be
  // used if the caller can not be identified (use `UNAUTHENTICATED`
  // instead for those errors). This error code does not imply the
  // request is valid or the requested entity exists or satisfies
  // other pre-conditions.

  // HTTP Mapping: 403 Forbidden
  case object PERMISSION_DENIED_7 extends GrpcStatus(code = 7, HttpStatus.Forbidden_403)

  // The request does not have valid authentication credentials for the
  // operation.

  // HTTP Mapping: 401 Unauthorized
  case object UNAUTHENTICATED_16 extends GrpcStatus(code = 16, HttpStatus.Unauthorized_401)

  // Some resource has been exhausted, perhaps a per-user quota, or
  // perhaps the entire file system is out of space.

  // HTTP Mapping: 429 Too Many Requests
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
  // HTTP Mapping: 400 Bad Request
  case object FAILED_PRECONDITION_9 extends GrpcStatus(code = 9, HttpStatus.BadRequest_400)

  // The operation was aborted, typically due to a concurrency issue such as
  // a sequencer check failure or transaction abort.

  // See the guidelines above for deciding between `FAILED_PRECONDITION`,
  // `ABORTED`, and `UNAVAILABLE`.
  //
  // HTTP Mapping: 409 Conflict
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
  // service.

  // HTTP Mapping: 501 Not Implemented
  case object UNIMPLEMENTED_12 extends GrpcStatus(code = 12, HttpStatus.NotImplemented_501)

  // Internal errors.  This means that some invariants expected by the
  // underlying system have been broken.  This error code is reserved
  // for serious errors.

  case object INTERNAL_13 extends GrpcStatus(code = 13, HttpStatus.InternalServerError_500)

  // The service is currently unavailable.  This is most likely a
  // transient condition, which can be corrected by retrying with
  // a backoff. Note that it is not always safe to retry
  // non-idempotent operations.

  // HTTP Mapping: 503 Service Unavailable
  case object UNAVAILABLE_14 extends GrpcStatus(code = 14, HttpStatus.ServiceUnavailable_503)

  // Unrecoverable data loss or corruption.
  //
  // HTTP Mapping: 500 Internal Server Error
  case object DATA_LOSS_15 extends GrpcStatus(code = 15, HttpStatus.InternalServerError_500)
}

sealed abstract class GrpcStatus(
    // gRPC status code
    val code: Int,
    // Mapping to an HTTP Status code (2xx - 5xx)
    val httpStatus: HttpStatus
) {

  /**
    * The string representation of this status code
    */
  def name: String = {
    // Trim _(status code) suffix
    val s = toString()
    s.indexOf('_') match {
      case -1  => s
      case pos => s.substring(0, pos)
    }
  }
}
