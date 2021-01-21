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
package wvlet.airframe.http.grpc

import io.grpc.{Attributes, Metadata, MethodDescriptor}
import wvlet.airframe.http.HttpAccessLogWriter
import wvlet.airframe.http.router.RPCCallContext
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap

trait GrpcRequestLogger {
  def logRPC(rpcCallContext: RPCCallContext): Unit
  def logError(e: Throwable, rpcCallContext: RPCCallContext): Unit
}

/**
  */
class DefaultGrpcRequestLogger(logWriter: HttpAccessLogWriter) extends GrpcRequestLogger with LogSupport {
  def logRPC(rpcCallContext: RPCCallContext): Unit = {
    val m = logDefault(rpcCallContext)
    logWriter.write(m)

  }
  def logError(e: Throwable, rpcCallContext: RPCCallContext): Unit = {
    val m = logDefault(rpcCallContext) ++
      HttpAccessLogWriter.errorLog(e)
    logWriter.write(m)
  }

  private def logDefault(rpcCallContext: RPCCallContext): Map[String, Any] = {
    val m = HttpAccessLogWriter.logUnixTime ++
      GrpcRequestLogger.logGrpcContext(GrpcContext.current) ++
      HttpAccessLogWriter.rpcLog(rpcCallContext)
    m
  }
}

object GrpcRequestLogger {

  def apply(writer:HttpAccessLogWriter) = new DefaultGrpcRequestLogger(writer)
  def default: GrpcRequestLogger    = apply(HttpAccessLogWriter.default)
  // Logger for discarding all logs
  def nullLogger: GrpcRequestLogger = EmptyGrpcRequestLogger

  private[grpc] object EmptyGrpcRequestLogger extends GrpcRequestLogger {
    override def logRPC(rpcCallContext: RPCCallContext): Unit = {
      // no-op
    }
    override def logError(e: Throwable, RPCCallContext: RPCCallContext): Unit = {
      // no-op
    }
  }

  private[grpc] def logGrpcContext(context: GrpcContext): Map[String, Any] = {
    logMethodDescriptor(context.descriptor) ++ logMetadata(context.metadata) ++ logAttributes(context.attributes)
  }

  import scala.jdk.CollectionConverters._

  private def logAttributes(a: Attributes): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    // A hack to extract client local/remote addresses from the string representation of an gRPC Attribute:
    for (elems <- a.toString.stripPrefix("{").stripSuffix("}").split(",\\s+")) {
      elems.split("=") match {
        case Array(k, v) if k.endsWith("-addr") =>
          m += HttpAccessLogWriter.sanitizeHeader(k) -> v.stripPrefix("/")
        case _ =>
      }
    }
    m.result()
  }

  private def logMethodDescriptor(d: MethodDescriptor[_, _]): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "path"        -> s"/${d.getFullMethodName}"
    m += "grpc_method_type" -> d.getType.toString
    m.result()
  }

  private def logMetadata(metadata: Metadata): Map[String, Any] = {
    val m = for (k <- metadata.keys().asScala) yield {
      HttpAccessLogWriter.sanitizeHeader(k) -> metadata.get(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER))
    }
    m.toMap[String, Any]
  }
}
