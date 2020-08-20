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
package greeter

import greeter.api.{GreeterApi, ServiceGrpc, ServiceSyncClient}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.Finagle
import wvlet.airframe.http.grpc.gRPC
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.log.{LogSupport, Logger}

/**
  */
object GreeterMain {
  def main(args: Array[String]): Unit = {
    Launcher.of[GreeterMain].execute(args)
  }
}

class GreeterMain(
    @option(prefix = "-h,--help", description = "show help messages", isHelp = true)
    help: Boolean,
    @option(prefix = "-p,--port", description = "server/client port (default:8080)")
    port: Int = 8080
) extends LogSupport {
  Logger.init

  private def router = Router.of[GreeterApi]

  @command(isDefault = true)
  def default: Unit = {
    info(s"Type --help to see the list of commands")
  }

  @command(description = "Start a Finagle server")
  def finagleServer: Unit = {
    Finagle.server
      .withRouter(router)
      .withPort(port)
      .start { server =>
        server.waitServerTermination
      }
  }

  @command(description = "Make Finagle RPC requests")
  def finagleClient(@option(prefix = "-n", description = "request count") n: Int = 3): Unit = {
    withResource(
      new ServiceSyncClient(
        Finagle.client
          .newSyncClient(s"localhost:${port}")
      )
    ) { client =>
      for (i <- 0 until n) {
        val response = client.GreeterApi.hello(s"RPC${i}")
        info(s"Received: ${response}")
      }
    }
  }

  @command(description = "Start a gRPC server")
  def grpcServer: Unit = {
    gRPC.server
      .withRouter(router)
      .withPort(port)
      .start { server =>
        server.awaitTermination
      }
  }

  @command(description = "Make gRPC requests")
  def grpcClient(@option(prefix = "-n", description = "request count") n: Int = 3): Unit = {
    val channel: ManagedChannel =
      ManagedChannelBuilder
        .forTarget(s"localhost:${port}")
        .usePlaintext()
        .build()

    val client = ServiceGrpc.newSyncClient(channel)
    for (i <- 0 until n) {
      val response = client.GreeterApi.hello(s"RPC${i}")
      info(s"Received: ${response}")
    }
    channel.shutdownNow()
  }

}
