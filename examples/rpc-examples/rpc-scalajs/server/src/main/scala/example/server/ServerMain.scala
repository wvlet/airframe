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
package example.server

import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.Finagle
import wvlet.airframe.launcher.{Launcher, command, option}
import wvlet.log.{LogSupport, Logger}

/**
  */
object ServerMain {

  def main(args: Array[String]): Unit = {
    Logger.init
    Launcher.of[ServerMain].execute(args)
  }
}

class ServerMain(
    @option(prefix = "-h,--help", description = "Display help messages", isHelp = true)
    help: Boolean)
    extends LogSupport {

  @command(isDefault = true)
  def default: Unit = {
    info(s"Type --help for the list of sub commands")
  }

  @command(description = "Launch an RPC server")
  def server(
      @option(prefix = "-p,--port", description = "port number")
      port: Int = 8080): Unit = {

    val router = Router
      .add[HelloApiImpl]
      .add[ServerApi]
    info(router)

    Finagle.server
      .withRouter(router)
      .withPort(port)
      .withName("example-server")
      .start { server =>
        server.waitServerTermination
      }
  }
}
