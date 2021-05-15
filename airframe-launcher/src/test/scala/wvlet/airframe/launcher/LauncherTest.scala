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

//--------------------------------------
//
// LauncherTest.scala
// Since: 2012/10/25 4:38 PM
//
//--------------------------------------

package wvlet.airframe.launcher

import java.io.ByteArrayOutputStream

import wvlet.airspec.AirSpec
import wvlet.log.{LogLevel, LogSupport, Logger}

class LauncherTest extends AirSpec {
  import LauncherTest._
  test("populate arguments in constructor") {
    capture {
      val l = Launcher.execute[GlobalOption]("-h -l debug")
      l.help shouldBe true
      l.loglevel shouldBe Some(LogLevel.DEBUG)
      l.started shouldBe true
    }
  }

  test("populate arguments in constructor even when no parameter is given") {
    val l = Launcher.execute[GlobalOption]("")
    l.help shouldBe false
    l.loglevel shouldBe None
    l.started shouldBe true
  }

  test("display help message") {
    val help = capture {
      val l = Launcher.execute[GlobalOption]("-h -l debug")
      l.started shouldBe true
    }
    trace(s"help message:\n$help")
    help.contains("-h") shouldBe true
    help.contains("--help") shouldBe true
    help.contains("-l") shouldBe true
    help.contains("--loglevel") shouldBe true
  }

  test("display full options in help") {
    capture {
      Launcher.execute[MyCommand]("--help")
    }.contains("--help") shouldBe true
    capture {
      Launcher.execute[MyCommand]("hello --help")
    }.contains("--help") shouldBe true
  }

  test("parse double hyphen options") {
    capture {
      val l = Launcher.execute[GlobalOption]("--help --loglevel debug")
      l.help shouldBe true
      l.loglevel shouldBe Some(LogLevel.DEBUG)
    }
  }

  test("populate nested options") {
    capture {
      val l = Launcher.execute[NestedOption]("-h -l debug")
      l.g.help shouldBe true
      l.g.loglevel shouldBe Some(LogLevel.DEBUG)
      l.g.started shouldBe true
    }
  }

  test("display help message of nested option") {
    val help = capture {
      Launcher.execute[NestedOption]("-h -l debug")
    }
    help.contains("-h") shouldBe true
    help.contains("--help") shouldBe true
    help.contains("-l") shouldBe true
    help.contains("--loglevel") shouldBe true
  }

  test("populate nested options even when no parameter is given") {
    val l = Launcher.execute[NestedOption]("")
    l.g != null shouldBe true
    l.g.help shouldBe false
    l.g.loglevel shouldBe None
    l.g.started shouldBe true
  }

  test("find commands") {
    val c = Launcher.execute[SimpleCommandSet]("hello")
    c.helloIsExecuted shouldBe true
  }

  test("display command list") {
    val help = capture {
      Launcher.of[SimpleCommandSet].printHelp
    }
    trace(s"command list help:\n$help")
    help.contains("hello") shouldBe true
    help.contains("say hello") shouldBe true
    help.contains("world") shouldBe true
    help.contains("say world") shouldBe true
    help.contains("default") shouldNotBe true
  }

  test("run default command") {
    val help = capture {
      Launcher.execute[SimpleCommandSet]("")
    }
    debug(s"default command message:\n$help")
    help.contains(DEFAULT_MESSAGE) shouldBe true
  }

  test("create command modules") {
    val c = myCommandModule

    capture {
      val r = c.execute("box hello")
      val m = r.executedInstance
      m.getClass shouldBe classOf[SimpleCommandSet]
      m.asInstanceOf[SimpleCommandSet].helloIsExecuted shouldBe true
    }
  }

  test("display command module help") {
    val help = capture {
      myCommandModule.execute("-h")
    }
    trace(help)
    help.contains("-h") shouldBe true
    help.contains("-l") shouldBe true
    help.contains("box") shouldBe true
    help.contains("command set") shouldBe true
  }

  test("display individual command help") {
    val help = capture {
      val result = myCommandModule.execute("box --help")
      val m      = result.getRootInstance.asInstanceOf[MyCommandModule]
      m.g.help shouldBe true
    }
    trace(help)
    help.contains("hello") shouldBe true
    help.contains("world") shouldBe true
  }

  test("display sub-command help") {
    val help = capture {
      val result = myCommandModule.execute("box world --help")
      val m      = result.getRootInstance.asInstanceOf[MyCommandModule]
      m.g.help shouldBe true
    }
    trace(s"box world --help:\n$help")
    help.contains("argMessage") shouldBe true
    help.contains("--color") shouldBe true
    help.contains("use color") shouldBe true
    help.contains("say world") shouldBe true
  }

  test("display invalid command error") {
    val msg = capture {
      intercept[IllegalArgumentException] {
        myCommandModule.execute("unknown-command")
      }
    }
    trace(msg)
  }

  test("unwrap InvocationTargetException") {
    val msg = capture {
      intercept[IllegalArgumentException] {
        myCommandModule.execute("errorTest")
      }
    }
    trace(msg)
  }

  test("handle private parameters in constructors") {
    capture {
      val l = Launcher.execute[CommandWithPrivateField]("-h")
      l.started shouldBe true
    }
  }

  test("run test command") {
    val message = capture {
      Launcher.execute[MyCommand]("hello -r 3") // hello x 3
    }
    debug(message)
    message.contains("hello!hello!hello!") shouldBe true
  }

  test("accept array type arguments") {
    val f = Launcher.execute[ArrayOpt]("file1 file2 file3")
    f.files shouldBe Array("file1", "file2", "file3")
  }

  test("accept array type arguments with default values") {
    val f = Launcher.execute[ArrayOptWithDefault]("")
    f.files shouldBe Array("sample")

    val f2 = Launcher.execute[ArrayOptWithDefault]("sampleA sampleB")
    f2.files shouldBe Array("sampleA", "sampleB")
  }

  test("accept list type arguments") {
    val f = Launcher.execute[ListOpt]("-f file1 -f file2 -f file3")
    f.files shouldBe List("file1", "file2", "file3")
  }

  test("accept Option arguments") {
    val f = Launcher.execute[OptArg]("")
    f.arg shouldBe None

    val f2 = Launcher.execute[OptArg]("hello")
    f2.arg shouldBe defined
    f2.arg.get shouldBe "hello"
  }
}

object LauncherTest {
  private val logger = Logger.of[LauncherTest]

  /**
    * Captures the output stream and returns the printed messages as a String
    *
    * @param body
    * @tparam U
    * @return
    */
  def capture[U](body: => U): String = {
    val out = new ByteArrayOutputStream
    Console.withOut(out) {
      body
    }
    val s = new String(out.toByteArray)
    logger.debug(s)
    s
  }

  def captureErr[U](body: => U): String = {
    val out = new ByteArrayOutputStream
    Console.withErr(out) {
      body
    }
    val s = new String(out.toByteArray)
    logger.debug(s)
    s
  }

  case class GlobalOption(
      @option(prefix = "-h,--help", description = "display help messages", isHelp = true) help: Boolean = false,
      @option(prefix = "-l,--loglevel", description = "log level") loglevel: Option[LogLevel] = None,
      var started: Boolean = false
  ) extends LogSupport {
    trace("started GlobalOption command")
    started = true
  }

  class NestedOption(val g: GlobalOption) extends LogSupport {
    trace("started NestedOption command")
  }

  val DEFAULT_MESSAGE = "Type --help to display the list of commands"

  @command(usage = "(sub command) [opts]", description = "simple command set")
  class SimpleCommandSet extends LogSupport {
    @command(isDefault = true)
    def default: Unit = {
      println(DEFAULT_MESSAGE)
    }

    var helloIsExecuted = false
    @command(description = "say hello")
    def hello: Unit = {
      trace("hello")
      helloIsExecuted = true
    }
    @command(description = "say world")
    def world(
        @argument argMessage: String,
        @option(prefix = "--color", description = "use color") color: Boolean
    ): Unit = debug("world world")
  }

  def myCommandModule =
    Launcher
      .of[MyCommandModule]
      .addModule[SimpleCommandSet]("box", description = "sub command set")

  class MyCommandModule(val g: GlobalOption) extends LogSupport {
    trace(s"global option: $g")

    @command(description = "exception test")
    def errorTest: Unit = {
      throw new IllegalArgumentException("error test")
    }
  }

  class CommandWithPrivateField(
      @option(prefix = "-h,--help", description = "display help", isHelp = true) help: Boolean,
      var started: Boolean = false
  ) {
    started = true
  }

  class MyCommand(@option(prefix = "-h,--help", description = "display help", isHelp = true) help: Boolean) {
    @command(description = "say hello")
    def hello(
        @option(prefix = "-r", description = "repeat times") repeat: Int = 1,
        @argument message: String = "hello!"
    ): Unit = {
      for (i <- 0 until repeat) print(message)
    }
  }

  class ArrayOpt(@argument val files: Array[String])

  class ListOpt(@option(prefix = "-f") val files: List[String])

  class OptArg(@argument val arg: Option[String] = None)

  class ArrayOptWithDefault(@argument val files: Array[String] = Array("sample"))
}
