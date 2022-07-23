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

import io.grpc.{Attributes, Metadata, MethodDescriptor}
import wvlet.airframe.http.BuildInfo
import wvlet.airframe.http.HttpAccessLogWriter
import wvlet.airframe.http.grpc.GrpcContext
import wvlet.airframe.http.internal.{HttpLogs, RPCCallContext}
import wvlet.log.LogSupport

import scala.collection.immutable.ListMap

trait GrpcRequestLogger extends AutoCloseable {
  def logRPC(grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Unit
  def logError(e: Throwable, grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Unit
}

/**
  */
class DefaultGrpcRequestLogger(serverName: String, logWriter: HttpAccessLogWriter)
    extends GrpcRequestLogger
    with LogSupport {
  def logRPC(grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Unit = {
    val m = logDefault(grpcContext, rpcCallContext)
    logWriter.write(m)

  }
  def logError(e: Throwable, grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Unit = {
    val m = logDefault(grpcContext, rpcCallContext) ++ HttpLogs.errorLogs(e)
    logWriter.write(m)
  }

  private def logDefault(grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Map[String, Any] = {
    val m = {
      HttpLogs.unixTimeLogs() ++
        ListMap("server_name" -> serverName, "x_airframe_server_version" -> BuildInfo.version) ++
        GrpcRequestLogger.logGrpcContext(grpcContext) ++
        HttpLogs.rpcLogs(rpcCallContext)
    }
    m
  }

  override def close(): Unit = {
    logWriter.close()
  }
}

object GrpcRequestLogger extends LogSupport {

  def newLogger(serverName: String) = new DefaultGrpcRequestLogger(serverName, HttpAccessLogWriter.default)
  def newLogger(serverName: String, writer: HttpAccessLogWriter) = new DefaultGrpcRequestLogger(serverName, writer)

  // Logger for discarding all logs
  def nullLogger: GrpcRequestLogger = EmptyGrpcRequestLogger

  private[grpc] object EmptyGrpcRequestLogger extends GrpcRequestLogger {
    override def logRPC(grpcContext: Option[GrpcContext], rpcCallContext: RPCCallContext): Unit = {
      // no-op
    }
    override def logError(e: Throwable, grpcContext: Option[GrpcContext], RPCCallContext: RPCCallContext): Unit = {
      // no-op
    }

    override def close(): Unit = {
      // no-op
    }
  }

  private[grpc] def logGrpcContext(context: Option[GrpcContext]): Map[String, Any] = {
    context
      .map { ctx =>
        logMethodDescriptor(ctx.descriptor) ++
          logMetadata(ctx.metadata) ++
          logAttributes(ctx.attributes)
      }.getOrElse(Map.empty)
  }

  import scala.jdk.CollectionConverters._

  private def logAttributes(a: Attributes): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    // Transport parameter keys become available since grpc-java 1.48.0
    Option(a.get(io.grpc.Grpc.TRANSPORT_ATTR_LOCAL_ADDR)).foreach { addr =>
      m += "local_addr" -> addr.toString.stripPrefix("/")
    }
    Option(a.get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR)).foreach { addr =>
      m += "remote_addr" -> addr.toString.stripPrefix("/")
    }
    m.result()
  }

  private def logMethodDescriptor(d: MethodDescriptor[_, _]): Map[String, Any] = {
    val m = ListMap.newBuilder[String, Any]
    m += "path"             -> s"/${d.getFullMethodName}"
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
