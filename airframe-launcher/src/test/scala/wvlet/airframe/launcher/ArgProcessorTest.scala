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
package wvlet.airframe.launcher
import wvlet.airframe.AirframeSpec
import wvlet.airframe.launcher.LauncherTest.capture
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

object ArgProcessorTest {

  @command(description = "My command")
  case class Cmd(
      @option(prefix = "-h,--help", description = "show help messages", isHelp = true) help: Boolean = false) {

    @defaultCommand
    def usage: Unit = {
      println("Type --help to show the list of sub commands")
    }
  }

  case class SubCmd(@option(prefix = "-p", description = "port number") port: Int) extends LogSupport {
    @command(description = "say hello")
    def hello(@option(prefix = "-t", description = "timeout sec") timeoutSec: Int = 10): Unit = {
      info(s"hello: timeout=${timeoutSec}")
    }
  }

  val nestedLauncher =
    Launcher
      .of[Cmd]
      .addModule[SubCmd]("sub", description = "sub command")

  class NestedCmd {
    @command(description = "hello a")
    def a: String = "hello"
    @command(description = "hello b")
    def b: String = "launcher"
  }

  val subCommandModule = Launcher
    .of[SubCmd]
    .addModule[NestedCmd](name = "nested1", description = "further nested command set 1")
    .addModule[NestedCmd](name = "nested2", description = "further nested command set 2")

  val moreNestedLauncher =
    Launcher
      .of[Cmd]
      .add(subCommandModule, name = "sub", description = "sub command")

  class FunctionArg(
      @option(prefix = "-e", description = "Environment")
      env: String = "default-env")
      extends LogSupport {

    @command(description = "Start a proxy server")
    def proxy(
        @option(prefix = "-p,--port", description = "port number")
        port: Int = IOUtil.randomPort) = {}
  }

  case class SeqArg(@argument args: Seq[String])
  case class SeqOption(@option(prefix = "-p") ports: Seq[Int])

  case class BooleanOption(@option(prefix = "--flag") flag: Option[Boolean] = None)
}

class ArgProcessorTest extends AirframeSpec {
  import ArgProcessorTest._

  "should run the default command" in {

    val c = capture {
      Launcher.of[Cmd].execute("")
    }
    c should include("Type --help to show the list of sub commands")
  }

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

  "should support more nested commands" in {
    val c = capture {
      moreNestedLauncher.execute("sub nested1 --help")
    }
    c should include("hello a")
    c should include("hello b")

    val c2 = capture {
      moreNestedLauncher.execute("--help")
    }
    c2 should include("sub")
    c2 should not include ("nested1")

    val c3 = capture {
      moreNestedLauncher.execute("sub --help")
    }
    c3 should include("hello")
    c3 should include("nested1")
    c3 should include("nested2")
  }

  "should support function arg" taggedAs ("farg") in {
    Launcher.of[FunctionArg].execute("proxy")
  }

  "should support argument list" in {
    // Single element => Seq("apple")
    Launcher.of[SeqArg].execute("apple").getRootInstance should be(SeqArg(Seq("apple")))
    // Multiple elements => Seq("apple", "banana")
    Launcher.of[SeqArg].execute("apple banana").getRootInstance should be(SeqArg(Seq("apple", "banana")))
  }

  "should support multiple same-name options" in {
    Launcher.of[SeqOption].execute("-p 10").getRootInstance should be(SeqOption(Seq(10)))
    Launcher.of[SeqOption].execute("-p 10 -p 20 -p 30").getRootInstance should be(SeqOption(Seq(10, 20, 30)))
  }

  "should set Option[Boolean] option" taggedAs working in {
    Launcher.of[BooleanOption].execute("--flag").getRootInstance shouldBe BooleanOption(Some(true))
    Launcher.of[BooleanOption].execute("").getRootInstance shouldBe BooleanOption(None)
  }
}
