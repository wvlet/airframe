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
package wvlet.airframe.http.grpc.internal

import io.grpc.{StatusException, StatusRuntimeException}
import wvlet.airframe.codec.MessageCodecException
import wvlet.airframe.http.{HttpServerException, HttpStatus}

import java.lang.reflect.InvocationTargetException
import scala.annotation.tailrec
import scala.concurrent.ExecutionException

/**
  */
object GrpcException {

  /**
    * Convert an exception to gRPC-specific exception types
    *
    * @param e
    * @return
    */
  def wrap(e: Throwable): Throwable = {

    /**
      * Find the root cause of the exception from wrapped exception classes
      */
    @tailrec
    def findCause(e: Throwable): Throwable = {
      e match {
        case i: InvocationTargetException if i.getTargetException != null =>
          findCause(i.getTargetException)
        case ee: ExecutionException if ee.getCause != null =>
          findCause(ee.getCause)
        case _ =>
          e
      }
    }

    findCause(e) match {
      case s: StatusException =>
        s
      case s: StatusRuntimeException =>
        s
      case e: MessageCodecException =>
        io.grpc.Status.INTERNAL
          .withDescription(s"Failed to encode/decode data")
          .withCause(e)
          .asRuntimeException()
      case e: IllegalArgumentException =>
        io.grpc.Status.INVALID_ARGUMENT
          .withCause(e)
          .asRuntimeException()
      case e: HttpServerException =>
        val s = e.status match {
          case HttpStatus.BadRequest_400 =>
            io.grpc.Status.INVALID_ARGUMENT
          case HttpStatus.Unauthorized_401 =>
            io.grpc.Status.UNAUTHENTICATED
          case HttpStatus.Forbidden_403 =>
            io.grpc.Status.PERMISSION_DENIED
          case HttpStatus.NotFound_404 =>
            io.grpc.Status.UNIMPLEMENTED
          case HttpStatus.Conflict_409 =>
            io.grpc.Status.ALREADY_EXISTS
          case HttpStatus.TooManyRequests_429 | HttpStatus.BadGateway_502 | HttpStatus.ServiceUnavailable_503 |
              HttpStatus.GatewayTimeout_504 =>
            io.grpc.Status.UNAVAILABLE
          case s if s.isServerError =>
            io.grpc.Status.INTERNAL
          case s if s.isClientError =>
            io.grpc.Status.INVALID_ARGUMENT
          case other =>
            io.grpc.Status.UNKNOWN
        }
        s.withCause(e).asRuntimeException()
      case other =>
        io.grpc.Status.INTERNAL
          .withCause(other)
          .asRuntimeException()
    }
  }
}
