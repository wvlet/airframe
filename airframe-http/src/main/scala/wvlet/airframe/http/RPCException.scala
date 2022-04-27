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

import wvlet.airframe.codec.{GenericException, GenericStackTraceElement, MessageCodec}

/**
  * RPCException provides a backend-independent (e.g., Finagle or gRPC) RPC error reporting mechanism. Create this
  * exception with (RPCStatus code).toException(...) method.
  *
  * If necessary, we can add more standard error_details parameter like
  * https://github.com/googleapis/googleapis/blob/master/google/rpc/error_details.proto
  */
case class RPCException(
    // RPC status
    status: RPCStatus = RPCStatus.INTERNAL_ERROR_I0,
    // Error message
    message: String = "",
    // Cause of the exception
    cause: Option[Throwable] = None,
    // [optional] Application-specific status code
    appErrorCode: Option[Int] = None,
    // [optional] Application-specific metadata
    metadata: Map[String, Any] = Map.empty
) extends Exception(s"[${status}] ${message}", cause.getOrElse(null)) {

  private var _includeStackTrace: Boolean = true

  /**
    * Do not embed stacktrace and the cause objects in the RPC exception error response
    */
  def noStackTrace: RPCException = {
    _includeStackTrace = false
    this
  }

  def toMessage: RPCErrorMessage = {
    RPCErrorMessage(
      code = status.code,
      codeName = status.name,
      message = message,
      stackTrace = if (_includeStackTrace) Some(GenericException.extractStackTrace(this)) else None,
      cause = if (_includeStackTrace) cause else None,
      appErrorCode = appErrorCode,
      metadata = metadata
    )
  }

  def toJson: String = {
    MessageCodec.of[RPCErrorMessage].toJson(toMessage)
  }
}

/**
  * A model class for RPC error message body. This message will be embedded to HTTP response body or gRPC trailer.
  *
  * We need this class to avoid directly serde RPCException class in airframe-codec, so that we can properly propagate
  * the exact stack trace to the client.
  */
case class RPCErrorMessage(
    code: Int = RPCStatus.UNKNOWN_I1.code,
    codeName: String = RPCStatus.UNKNOWN_I1.name,
    message: String = "",
    stackTrace: Option[Seq[GenericStackTraceElement]] = None,
    cause: Option[Throwable] = None,
    appErrorCode: Option[Int] = None,
    metadata: Map[String, Any] = Map.empty
)

object RPCException {
  def fromJson(json: String): RPCException = {
    val codec = MessageCodec.of[RPCErrorMessage]
    val m     = codec.fromJson(json)
    val ex = new RPCException(
      status = RPCStatus.ofCode(m.code),
      message = m.message,
      cause = m.cause,
      appErrorCode = m.appErrorCode,
      metadata = m.metadata
    )
    // Recover the original stack trace
    m.stackTrace.foreach { x =>
      ex.setStackTrace(x.map(_.toJavaStackTraceElement).toArray)
    }
    ex
  }
}
