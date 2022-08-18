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

import io.grpc.ServerServiceDefinition
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import wvlet.airframe.http.grpc.internal.{GrpcContextTrackInterceptor, GrpcRequestLogger, GrpcResponseHeaderInterceptor}
import wvlet.log.LogSupport

import java.util.concurrent.ExecutorService
import scala.language.existentials

/**
  * GrpcService is a holder of the thread executor and service definitions for running gRPC servers
  */
case class GrpcService(
    config: GrpcServerConfig,
    executorService: ExecutorService,
    requestLogger: GrpcRequestLogger,
    serviceDefinitions: Seq[ServerServiceDefinition]
) extends AutoCloseable
    with LogSupport {
  def newServer: GrpcServer = {
    trace(s"service:\n${serviceDefinitions.map(_.getServiceDescriptor).mkString("\n")}")
    // We need to use NettyServerBuilder explicitly when NettyServerBuilder cannot be found from the classpath (e.g., onejar)
    val serverBuilder = NettyServerBuilder.forPort(config.port)
    for (service <- serviceDefinitions) {
      serverBuilder.addService(service)
    }

    // Add user-provided interceptors
    for (interceptor <- config.interceptors) {
      serverBuilder.intercept(interceptor)
    }
    // Add an interceptor for setting content-type response header
    serverBuilder.intercept(GrpcResponseHeaderInterceptor)
    // Add an interceptor for remembering GrpcContext. This must happen at the root level
    serverBuilder.intercept(GrpcContextTrackInterceptor)

    val customServerBuilder = config.serverInitializer(serverBuilder)
    val server              = new GrpcServer(this, customServerBuilder.build())
    server.start
    server
  }

  override def close(): Unit = {
    requestLogger.close()
    executorService.shutdownNow()
  }
}
