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
import java.util.concurrent.Executors

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.{Channel, ManagedChannel, ManagedChannelBuilder, Server, ServerBuilder, ServerInterceptor}
import wvlet.airframe.http.Router
import wvlet.airframe.http.grpc.GrpcServiceBuilder.GrpcServiceThreadExecutor
import wvlet.airframe.{Design, Session}
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

import scala.language.existentials

/**
  */
case class GrpcServerConfig(
    name: String = "default",
    private val serverPort: Option[Int] = None,
    router: Router = Router.empty,
    interceptors: Seq[ServerInterceptor] = Seq.empty,
    serverInitializer: ServerBuilder[_] => ServerBuilder[_] = identity
) extends LogSupport {
  lazy val port = serverPort.getOrElse(IOUtil.unusedPort)

  def withName(name: String): GrpcServerConfig     = this.copy(name = name)
  def withPort(port: Int): GrpcServerConfig        = this.copy(serverPort = Some(port))
  def withRouter(router: Router): GrpcServerConfig = this.copy(router = router)
  def withServerInitializer(serverInitializer: ServerBuilder[_] => ServerBuilder[_]) =
    this.copy(serverInitializer = serverInitializer)
  def withInterceptor(interceptor: ServerInterceptor): GrpcServerConfig =
    this.copy(interceptors = interceptors :+ interceptor)
  def noInterceptor: GrpcServerConfig = this.copy(interceptors = Seq.empty)

  def newServer(session: Session): GrpcServer = {
    val services = GrpcServiceBuilder.buildService(router, session)
    trace(s"service:\n${services.map(_.getServiceDescriptor).mkString("\n")}")
    // We need to use NettyServerBuilder explicitly when NettyServerBuilder cannot be found from the classpath (e.g., onejar)
    val serverBuilder = NettyServerBuilder.forPort(port)
    for (service <- services) {
      serverBuilder.addService(service)
    }
    for (interceptor <- interceptors) {
      serverBuilder.intercept(interceptor)
    }
    val customServerBuilder = serverInitializer(serverBuilder)
    new GrpcServer(this, customServerBuilder.build())
  }

  /**
    * Start a standalone gRPC server and execute the given code block.
    * After exiting the code block, it will stop the gRPC server.
    *
    * If you want to keep running the server inside the code block, call server.awaitTermination.
    */
  def start[U](body: GrpcServer => U): U = {
    design.run[GrpcServer, U] { server =>
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
      .onStart { _.start }
      .bind[GrpcServiceThreadExecutor].toInstance(Executors.newCachedThreadPool())
      .onShutdown(_.shutdownNow())
  }

  /**
    * Create a design for GrpcServer and ManagedChannel. Useful for testing purpsoe
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

class GrpcServer(grpcServerConfig: GrpcServerConfig, server: Server) extends AutoCloseable with LogSupport {
  def port: Int            = grpcServerConfig.port
  def localAddress: String = s"localhost:${grpcServerConfig.port}"

  def start: Unit = {
    info(s"Starting gRPC server ${grpcServerConfig.name} at ${localAddress}")
    server.start()
  }

  def awaitTermination: Unit = {
    server.awaitTermination()
  }

  override def close(): Unit = {
    info(s"Closing gRPC server ${grpcServerConfig.name} at ${localAddress}")
    server.shutdownNow()
  }
}
