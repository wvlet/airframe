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

package wvlet.airframe.opts

import java.io.ByteArrayOutputStream

import wvlet.log.{LogLevel, LogSupport}
import wvlet.airframe.AirframeSpec

/**
  * @author leo
  */
class LauncherTest extends AirframeSpec {

  import Launcher._

  "Launcher" should {

    import LauncherTest._

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
      new String(out.toByteArray)
    }

    def captureErr[U](body: => U): String = {
      val out = new ByteArrayOutputStream
      Console.withErr(out) {
        body
      }
      new String(out.toByteArray)
    }

    "populate arguments in constructor" taggedAs ("test1") in {
      capture {
        val l = Launcher.execute[GlobalOption]("-h -l debug")
        l.help should be(true)
        l.loglevel should be(Some(LogLevel.DEBUG))
        l.started should be(true)
      }
    }

    "populate arguments in constructor even when no parameter is given" taggedAs ("popl") in {
      val l = Launcher.execute[GlobalOption]("")
      l.help should be(false)
      l.loglevel should be(None)
      l.started should be(true)
    }

    "display help message" in {
      val help = capture {
        val l = Launcher.execute[GlobalOption]("-h -l debug")
        l.started should be(true)
      }
      trace(s"help message:\n$help")
      help should (include("-h"))
      help should (include("--help"))
      help should (include("-l"))
      help should (include("--loglevel"))
    }

    "parse double hyphen options" in {
      capture {
        val l = Launcher.execute[GlobalOption]("--help --loglevel debug")
        l.help should be(true)
        l.loglevel should be(Some(LogLevel.DEBUG))
      }
    }

    "populate nested options" taggedAs ("nested") in {
      capture {
        val l = Launcher.execute[NestedOption]("-h -l debug")
        l.g.help should be(true)
        l.g.loglevel should be(Some(LogLevel.DEBUG))
        l.g.started should be(true)
      }
    }

    "display help message of nested option" in {
      val help = capture {
        Launcher.execute[NestedOption]("-h -l debug")
      }
      help should (include("-h"))
      help should (include("--help"))
      help should (include("-l"))
      help should (include("--loglevel"))
    }

    "populate nested options even when no paramter is given" taggedAs ("nested2") in {
      val l = Launcher.execute[NestedOption]("")
      l.g should not be (null)
      l.g.help should be(false)
      l.g.loglevel should be(None)
      l.g.started should be(true)
    }

    "find commands" in {
      val c = Launcher.execute[SimpleCommandSet]("hello")
      c.helloIsExecuted should be(true)
    }

    "display command list" in {
      val help = capture {
        Launcher.of[SimpleCommandSet].printHelp
      }
      trace(s"command list help:\n$help")
      help should (include("hello"))
      help should (include("say hello"))
      help should (include("world"))
      help should (include("say world"))
    }

    "run default command" in {
      val help = capture {
        Launcher.execute[SimpleCommandSet]("")
      }
      debug(s"default command message:\n$help")
      help should (include(DEFAULT_MESSAGE))
    }

    "create command modules" in {
      val c = Launcher.execute[MyCommandModule]("box hello")
      c.executedModule should be('defined)
      c.executedModule map { m =>
        m._1 should be("box")
        m._2.getClass should be(classOf[SimpleCommandSet])
        m._2.asInstanceOf[SimpleCommandSet].helloIsExecuted should be(true)
      }
      c.g should not be (null)
    }

    "display comand module help" in {
      val help = capture {
        Launcher.execute[MyCommandModule]("-h")
      }
      trace(help)
      help should (include("-h"))
      help should (include("-l"))
      help should (include("box"))
      help should (include("command set"))
    }

    "display individual command help" in {
      val help = capture {
        val l = Launcher.execute[MyCommandModule]("box --help")
        l.g.help should be(true)
      }
      trace(help)
      help should (include("hello"))
      help should (include("world"))
    }

    "display subcommand help" in {
      val help = capture {
        val l = Launcher.execute[MyCommandModule]("box world --help")
        l.g.help should be(true)
      }
      trace(s"box world --help:\n$help")
      help should (include("message"))
    }

    "display invalid command error" in {
      val msg = capture {
        Launcher.execute[MyCommandModule]("unknown-command")
      }
      trace(msg)
    }

    "unwrap InvocationTargetException" in {
      val msg = capture {
        intercept[IllegalArgumentException] {
          Launcher.execute[MyCommandModule]("errorTest")
        }
      }
      trace(msg)
    }

    "handle private parameters in constructors" in {
      capture {
        val l = Launcher.execute[CommandWithPrivateField]("-h")
        l.started should be(true)
      }
    }

    "run test command" in {
      val message = capture {
        Launcher.execute[MyCommand]("hello -r 3") // hello x 3
      }
      debug(message)
      message should (include("hello!hello!hello!"))
    }

    "accept array type arguments" taggedAs ("array") in {
      val f = Launcher.execute[ArrayOpt]("file1 file2 file3")
      f.files should be(Array("file1", "file2", "file3"))
    }

    "accept array type arguments with default values" taggedAs ("array-default") in {
      val f = Launcher.execute[ArrayOptWithDefault]("")
      f.files should be(Array("sample"))

      val f2 = Launcher.execute[ArrayOptWithDefault]("sampleA sampleB")
      f2.files should be(Array("sampleA", "sampleB"))
    }

    "accept list type arguments" taggedAs ("list") in {
      val f = Launcher.execute[ListOpt]("-f file1 -f file2 -f file3")
      f.files should be(List("file1", "file2", "file3"))
    }

    "accept Option arguments" taggedAs ("optarg") in {
      val f = Launcher.execute[OptArg]("")
      f.arg should be(None)

      val f2 = Launcher.execute[OptArg]("hello")
      f2.arg should be('defined)
      f2.arg.get should be("hello")
    }

  }
}

object LauncherTest {

  case class GlobalOption(
      @option(prefix = "-h,--help", description = "display help messages", isHelp = true) help: Boolean = false,
      @option(prefix = "-l,--loglevel", description = "log level") loglevel: Option[LogLevel] = None,
      var started: Boolean = false)
      extends LogSupport {

    trace("started GlobalOption command")
    started = true
  }

  class NestedOption(val g: GlobalOption) extends LogSupport {
    trace("started NestedOption command")

  }

  val DEFAULT_MESSAGE = "Type --help to display the list of commands"

  class SimpleCommandSet extends DefaultCommand with LogSupport {

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
    def world(@argument message: String): Unit = debug("world world")
  }

  class MyCommandModule(val g: GlobalOption) extends CommandModule with LogSupport {
    def modules = Seq(ModuleDef[SimpleCommandSet]("box", description = "command set"))

    trace(s"global option: $g")

    @command(description = "exception test")
    def errorTest: Unit = {
      throw new IllegalArgumentException(s"error test")
    }
  }

  class CommandWithPrivateField(
      @option(prefix = "-h,--help", description = "display help", isHelp = true) help: Boolean,
      var started: Boolean = false) {
    started = true
  }

  class MyCommand(@option(prefix = "-h,--help", description = "display help", isHelp = true) help: Boolean) {
    @command(description = "say hello")
    def hello(@option(prefix = "-r", description = "repeat times") repeat: Int = 1,
              @argument message: String = "hello!"): Unit = {
      for (i <- 0 until repeat) print(message)
    }
  }

  class ArrayOpt(@argument val files: Array[String])

  class ListOpt(@option(prefix = "-f") val files: List[String])

  class OptArg(@argument val arg: Option[String] = None)

  class ArrayOptWithDefault(@argument val files: Array[String] = Array("sample"))

}
