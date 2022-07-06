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
import wvlet.airframe.launcher.LauncherTest.capture
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

object ArgProcessorTest {
  @command(description = "My command")
  case class Cmd(
      @option(prefix = "-h,--help", description = "show help messages", isHelp = true) help: Boolean = false
  ) {
    @command(isDefault = true)
    def usage: Unit = {
      println("Type --help to show the list of sub commands")
    }
  }

  case class SubCmd(@option(prefix = "-p", description = "port number") port: Int) extends LogSupport {
    @command(description = "say hello")
    def hello(@option(prefix = "-t", description = "timeout sec") timeoutSec: Int = 10): Unit = {
      debug(s"hello: timeout=${timeoutSec}")
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
      env: String = "default-env"
  ) extends LogSupport {
    @command(description = "Start a proxy server")
    def proxy(
        @option(prefix = "-p,--port", description = "port number")
        port: Int = IOUtil.randomPort
    ) = {}
  }

  case class SeqArg(@argument args: Seq[String])
  case class SeqOption(@option(prefix = "-p") ports: Seq[Int])

  case class BooleanOption(@option(prefix = "--flag") flag: Option[Boolean] = None)
}

class ArgProcessorTest extends AirSpec {
  import ArgProcessorTest._

  test("should run the default command") {
    val c = capture {
      Launcher.of[Cmd].execute("")
    }
    c.contains("Type --help to show the list of sub commands") shouldBe true
  }

  test("should parse top-level arguments") {
    val c = capture {
      val l = Launcher.of[Cmd]
      l.execute("")
      l.execute("-h")
    }
    c.contains("My command")
  }

  test("should show global options") {
    val c = capture {
      nestedLauncher.execute("sub -h")
    }
    c.contains("global options") shouldBe true
    c.contains("port number") shouldBe true
  }

  test("should show sub command options") {
    val c = capture {
      nestedLauncher.execute("sub hello -h")
    }
    c.contains("global options") shouldBe true
    c.contains("port number") shouldBe true
  }

  test("should execute sub commands") {
    capture {
      nestedLauncher.execute("sub hello")
      nestedLauncher.execute("sub hello -t 100")
    }
  }

  test("should support more nested commands") {
    val c = capture {
      moreNestedLauncher.execute("sub nested1 --help")
    }
    c.contains("hello a") shouldBe true
    c.contains("hello b") shouldBe true

    val c2 = capture {
      moreNestedLauncher.execute("--help")
    }
    c2.contains("sub") shouldBe true
    c2.contains("nested1") shouldNotBe true

    val c3 = capture {
      moreNestedLauncher.execute("sub --help")
    }
    c3.contains("hello") shouldBe true
    c3.contains("nested1") shouldBe true
    c3.contains("nested2") shouldBe true
  }

  test("should support function arg") {
    Launcher.of[FunctionArg].execute("proxy")
  }

  test("should support argument list") {
    // Single element => Seq("apple")
    Launcher.of[SeqArg].execute("apple").getRootInstance shouldBe SeqArg(Seq("apple"))
    // Multiple elements => Seq("apple", "banana")
    Launcher.of[SeqArg].execute("apple banana").getRootInstance shouldBe SeqArg(Seq("apple", "banana"))
  }

  test("should support multiple same-name options") {
    Launcher.of[SeqOption].execute("-p 10").getRootInstance shouldBe SeqOption(Seq(10))
    Launcher.of[SeqOption].execute("-p 10 -p 20 -p 30").getRootInstance shouldBe SeqOption(Seq(10, 20, 30))
  }

  test("should set Option[Boolean] option") {
    Launcher.of[BooleanOption].execute("--flag").getRootInstance shouldBe BooleanOption(Some(true))
    Launcher.of[BooleanOption].execute("").getRootInstance shouldBe BooleanOption(None)
  }
}
