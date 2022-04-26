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

import io.grpc.{Metadata, Status, StatusException, StatusRuntimeException}
import wvlet.airframe.codec.MessageCodecException
import wvlet.airframe.http.{GrpcStatus, HttpServerException, HttpStatus}
import wvlet.log.LogSupport

import java.lang.reflect.InvocationTargetException
import scala.annotation.tailrec
import scala.concurrent.ExecutionException

/**
  */
object GrpcException extends LogSupport {

  private[grpc] val rpcErrorKey = Metadata.Key.of[String]("airframe_rpc_error", Metadata.ASCII_STRING_MARSHALLER)

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
          .withDescription(s"Failed to encode/decode data: ${e.getMessage}")
          .withCause(e)
          .asRuntimeException()
      case e: IllegalArgumentException =>
        io.grpc.Status.INVALID_ARGUMENT
          .withCause(e)
          .withDescription(e.getMessage)
          .asRuntimeException()
      case e: UnsupportedOperationException =>
        io.grpc.Status.UNIMPLEMENTED
          .withCause(e)
          .withDescription(e.getMessage)
          .asRuntimeException()
      case e: HttpServerException =>
        val grpcStatus = GrpcStatus.ofHttpStatus(e.status)
        val s = Status
          .fromCodeValue(grpcStatus.code)
          .withCause(e)
          .withDescription(e.getMessage)

        if (e.message.nonEmpty) {
          val m        = e.message
          val metadata = new Metadata()
          metadata.put[String](rpcErrorKey, s"${m.toContentString}")
          s.asRuntimeException(metadata)
        } else {
          s.asRuntimeException()
        }
      case other =>
        io.grpc.Status.INTERNAL
          .withCause(other)
          .asRuntimeException()
    }
  }
}
