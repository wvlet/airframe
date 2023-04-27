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

import greeter.api.{GreeterApi, GreeterRPC}
import wvlet.airframe.http.Http
import wvlet.airframe.http.router.RxRouter
import wvlet.airframe.http.netty.Netty
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.log.{LogSupport, Logger}
import scala.util.Using

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

  private def router = RxRouter.of[GreeterApi]

  @command(isDefault = true)
  def default: Unit = {
    info(s"Type --help to see the list of commands")
  }

  @command(description = "Start an RPC server")
  def server: Unit = {
    Netty.server
      .withRouter(router)
      .withPort(port)
      .start { server =>
        server.awaitTermination()
      }
  }

  @command(description = "Make RPC requests")
  def client(@option(prefix = "-n", description = "request count") n: Int = 3): Unit = {
    Using.resource(GreeterRPC.newRPCSyncClient(Http.client.newSyncClient(s"localhost:${port}"))) { client =>
      for (i <- 0 until n) {
        val response = client.GreeterApi.hello(s"RPC${i}")
        info(s"Received: ${response}")
      }
    }
  }
}
