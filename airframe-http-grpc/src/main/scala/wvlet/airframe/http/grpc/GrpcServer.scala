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

import io.grpc._
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.http.Router
import wvlet.airframe.http.grpc.internal.{GrpcRequestLogger, GrpcServiceBuilder}
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, ForkJoinPool, ForkJoinWorkerThread}
import scala.language.existentials

/**
  */
case class GrpcServerConfig(
    // The server name
    name: String = "default",
    private val serverPort: Option[Int] = None,
    router: Router = Router.empty,
    interceptors: Seq[ServerInterceptor] = Seq.empty,
    serverInitializer: ServerBuilder[_] => ServerBuilder[_] = identity,
    executorProvider: GrpcServerConfig => ExecutorService = GrpcServer.newAsyncExecutorFactory,
    maxThreads: Int = (Runtime.getRuntime.availableProcessors() * 2).max(2),
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForMapOutput,
    requestLoggerProvider: GrpcServerConfig => GrpcRequestLogger = { config: GrpcServerConfig =>
      GrpcRequestLogger
        .newLogger(config.name)
    }
) extends LogSupport {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  def withName(name: String): GrpcServerConfig     = this.copy(name = name)
  def withPort(port: Int): GrpcServerConfig        = this.copy(serverPort = Some(port))
  def withRouter(router: Router): GrpcServerConfig = this.copy(router = router)

  /**
    * Use this method to customize gRPC server, e.g., setting tracer, add transport filter, etc.
    *
    * @param serverInitializer
    * @return
    */
  def withServerInitializer(serverInitializer: ServerBuilder[_] => ServerBuilder[_]) =
    this.copy(serverInitializer = serverInitializer)

  /**
    * Add an gRPC interceptor
    *
    * @param interceptor
    * @return
    */
  def withInterceptor(interceptor: ServerInterceptor): GrpcServerConfig =
    this.copy(interceptors = interceptors :+ interceptor)
  def noInterceptor: GrpcServerConfig = this.copy(interceptors = Seq.empty)

  /**
    * Set a custom thread pool. The default is Executors.newCachedThreadPool()
    */
  def withExecutorServiceProvider(provider: GrpcServerConfig => ExecutorService) =
    this.copy(executorProvider = provider)

  /**
    * Set the maximum number of grpc server threads. The default is max(the number of CPU x 2, 2).
    * If you are using a custom ExecutorService, this setting might not be effective.
    */
  def withMaxThreads(numThreads: Int) = this.copy(maxThreads = numThreads)

  def withCodecFactory(newCodecFactory: MessageCodecFactory) = this.copy(codecFactory = newCodecFactory)

  def withRequestLoggerProvider(provider: GrpcServerConfig => GrpcRequestLogger) = this
    .copy(requestLoggerProvider = provider)
  // Disable RPC logging
  def noRequestLogging = this.copy(requestLoggerProvider = { config: GrpcServerConfig => GrpcRequestLogger.nullLogger })

  /**
    * Create and start a new server based on this config.
    */
  def newServer(session: Session): GrpcServer = {
    val grpcService = GrpcServiceBuilder.buildService(this, session)
    grpcService.newServer
  }

  /**
    * Start a standalone gRPC server and execute the given code block.
    * After exiting the code block, it will stop the gRPC server.
    *
    * If you want to keep running the server inside the code block, call server.awaitTermination.
    */
  def start[U](body: GrpcServer => U): U = {
    design.noLifeCycleLogging.run[GrpcServer, U] { server =>
      body(server)
    }
  }

  /**
    * Create a GrpcServer design for Airframe DI
    */
  def design: Design = {
    Design.newDesign
      .bind[GrpcServerConfig].toInstance(this)
      .bind[GrpcServer].toProvider { (config: GrpcServerConfig, session: Session) => config.newServer(session) }
  }

  /**
    * Create a design for GrpcServer and ManagedChannel. Useful for testing purpose
    *
    * @return
    */
  def designWithChannel: Design = {
    design
      .bind[Channel].toProvider { server: GrpcServer =>
        ManagedChannelBuilder.forTarget(server.localAddress).usePlaintext().build()
      }
      .onShutdown {
        case m: ManagedChannel => m.shutdownNow()
        case _                 =>
      }
  }
}

class GrpcServer(grpcService: GrpcService, server: Server) extends AutoCloseable with LogSupport {
  def port: Int            = grpcService.config.port
  def localAddress: String = s"localhost:${port}"

  def start: Unit = {
    info(s"Starting gRPC server [${grpcService.config.name}] at ${localAddress}")
    server.start()
  }

  def awaitTermination: Unit = {
    server.awaitTermination()
  }

  override def close(): Unit = {
    info(s"Closing gRPC server [${grpcService.config.name}] at ${localAddress}")
    server.shutdownNow()
    grpcService.close()
  }
}

object GrpcServer {
  private[grpc] def newAsyncExecutorFactory(config: GrpcServerConfig): ExecutorService = {
    new ForkJoinPool(
      // The number of threads
      config.maxThreads,
      new ForkJoinWorkerThreadFactory() {
        private val threadCount = new AtomicInteger()
        override def newThread(pool: ForkJoinPool): ForkJoinWorkerThread = {
          val thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
          val name   = s"grpc-${config.name}-${threadCount.getAndIncrement()}"
          thread.setDaemon(true)
          thread.setName(name)
          thread
        }
      },
      null, // Use the default behavior for unrecoverable exceptions
      true  // Enable asyncMode as grpc server will never join
    );
  }
}
