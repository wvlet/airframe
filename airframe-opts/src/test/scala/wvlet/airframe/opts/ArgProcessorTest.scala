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
package wvlet.airframe.opts
import wvlet.airframe.AirframeSpec
import wvlet.airframe.opts.LauncherTest.capture
import wvlet.log.LogSupport

object ArgProcessorTest {

  case class Cmd(
      @option(prefix = "-h,--help", description = "show help messages", isHelp = true) help: Boolean = false) {

    @defaultCommand
    def usage: Unit = {
      println("Type --help to show the list of sub commands")
    }
  }

  case class SubCmd(@option(prefix = "-p", description = "port number") port: Int) extends LogSupport {

    @command
    def hello(@option(prefix = "-t", description = "timeout sec") timeoutSec: Int = 10): Unit = {
      info(s"hello: timeout=${timeoutSec}")
    }
  }

  val nestedLauncher =
    Launcher
      .of[Cmd]
      .addCommandModule[SubCmd]("sub", description = "sub command")

}

class ArgProcessorTest extends AirframeSpec {
  import ArgProcessorTest._

  "should parse top-level arguments" in {
    val l = Launcher.of[Cmd]
    l.execute("")
    l.execute("-h")
  }

  "should show global options" in {
    val c = capture {
      nestedLauncher.execute("sub -h")
    }
    c should include("global options")
    c should include("port number")
  }

  "should show sub command options" in {
    val c = capture {
      nestedLauncher.execute("sub hello -h")
    }
    c should include("global options")
    c should include("port number")
  }

  "should execute sub commands" in {
    capture {
      nestedLauncher.execute("sub hello")
      nestedLauncher.execute("sub hello -t 100")
    }
  }

}
