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
// Launcher.scala
// Since: 2012/10/25 4:37 PM
//
//--------------------------------------

package wvlet.airframe.opts

import java.lang.reflect.InvocationTargetException

import org.msgpack.core.MessagePack
import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory, MessageHolder, ParamListCodec}
import wvlet.airframe.control.CommandLineTokenizer
import wvlet.airframe.opts.OptionParser.CLOption
import wvlet.airframe.surface.reflect.{CName, SurfaceFactory}
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

/**
  * Command launcher
  */
object Launcher extends LogSupport {

  /**
    * Create a new Launcher of the given type
    * @tparam A
    * @return
    */
  def of[A: ru.WeakTypeTag]: Launcher = {
    val cl = newCommandLauncher(SurfaceFactory.of[A], name = "", description = "")
    new Launcher(LauncherConfig(), cl)
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l      = Launcher.of[A]
    val result = l.execute(args)
    result.getRootInstance.asInstanceOf[A]
  }

  /**
    * Create a launcher for a class
    * @return
    */
  private[opts] def newCommandLauncher(surface: Surface, name: String, description: String): CommandLauncher = {
    val parser = OptionParser(surface)

    // Generate a command-line usage message
    val defaultUsage =
      parser.schema.args.map(x => s"[${x}]").mkString(" ")

    import wvlet.airframe.surface.reflect._

    // If the user specified a usage description via @command annotation, use this.
    val usage =
      surface
        .findAnnotationOf[command]
        .map(_.usage)
        .find(_.nonEmpty)
        .getOrElse(defaultUsage)

    // Find sub commands marked with [[wvlet.airframe.opts.command]] annotation
    import wvlet.airframe.surface.reflect._
    val methods = SurfaceFactory.methodsOf(surface)
    val subCommands = for (m <- methods; c <- m.findAnnotationOf[command]) yield {
      newMethodLauncher(m, c)
    }

    // Find the default command
    val defaultCommand = SurfaceFactory
      .methodsOf(surface)
      .find { m =>
        import wvlet.airframe.surface.reflect._
        m.findAnnotationOf[defaultCommand].isDefined
      }
      .map { m =>
        { li: LauncherInstance =>
          m.call(li.instance)
        }
      }

    new CommandLauncher(LauncherInfo(name, description, usage), parser, subCommands, defaultCommand)
  }

  /**
    * Create a launcher from a method in a class
    */
  private def newMethodLauncher(m: MethodSurface, command: command): CommandLauncher = {

    val parser       = new OptionParser(m)
    val defaultUsage = parser.schema.args.map(x => s"[${x}]").mkString(" ")

    val description =
      Some(command.description())
        .map(x => x)
        .find(_.nonEmpty)
        .getOrElse("")

    val usage = {
      val argLine = Some(command.description())
        .map(x => x)
        .find(_.nonEmpty)
        .getOrElse(defaultUsage)
      s"${m.name} ${argLine}"
    }

    val li = LauncherInfo(m.name, description, usage)
    new CommandLauncher(li, parser, Seq.empty, None)
  }
}

/**
  * Using a mutable data structure for simplicity.
  * This should be safe since this config is internal-only config holder
  */
private[opts] case class LauncherConfig(
    var withHelpOption: Boolean = true,
    var helpMessagePrinter: HelpMessagePrinter = new HelpMessagePrinter,
    var codecFactory: MessageCodecFactory = MessageCodec.defaultFactory,
    // command name -> default action
    var defaultCommand: LauncherInstance => Any = { li: LauncherInstance =>
      println("Type --help to see the usage")
    }
)

class Launcher(config: LauncherConfig, mainLauncher: CommandLauncher) {

  def printHelp: Unit = {
    config.helpMessagePrinter.printHelp(List(mainLauncher))
  }

  /**
    * Set a function to be used when there is no command is specified
    * @param command
    * @tparam U
    * @return
    */
  def withDefaultCommand(newDefaultCommand: LauncherInstance => Any): Launcher = {
    config.defaultCommand = newDefaultCommand
    this
  }

  def withHelpMessagePrinter(newHelpMessagePrinter: HelpMessagePrinter): Launcher = {
    config.helpMessagePrinter = newHelpMessagePrinter
    this
  }

  def withCodecFactory(newCodecFactory: MessageCodecFactory): Launcher = {
    config.codecFactory = newCodecFactory
    this
  }

  def execute(argLine: String): LauncherResult = execute(CommandLineTokenizer.tokenize(argLine))
  def execute(args: Array[String]): LauncherResult = {
    mainLauncher.execute(config, List.empty, args.toSeq, showHelp = false)
  }

  /**
    * Add a sub command module to the launcher
    * @param subCommandName
    * @param description
    * @tparam A
    * @return
    */
  def addCommandModule[B: ru.TypeTag](name: String, description: String): Launcher = {
    new Launcher(config, mainLauncher.addCommandModule[B](name, description))
  }

  def addNestedCommandModule[B: ru.TypeTag](name: String, description: String)(
      nested: CommandLauncher => CommandLauncher): Launcher = {
    new Launcher(config, mainLauncher.addNestedCommandModule[B](name, description)(nested))
  }

}

/**
  * Command execution results
  * @param executedModule
  * @param result
  */
case class LauncherResult(launcherStack: List[LauncherInstance], result: Option[Any]) {
  require(launcherStack.nonEmpty, "launcherStack should not be empty")

  def getRootInstance: Any  = launcherStack.reverse.head.instance
  def executedInstance: Any = launcherStack.head.instance
}
case class LauncherInstance(launcher: CommandLauncher, instance: Any)

case class LauncherInfo(name: String, description: String, usage: String)

/**
  * Command launcher.
  *
  * {{{
  * class MyCommand(@option(prefix="-h,--help", description="display help", isHelp=true) help:Boolean) {
  *   @command(description="Say hello")
  *   def hello(@option(prefix="-r", description="repeat times")
  *             repeat:Int=1,
  *             @argument
  *             message:String = "hello") {
  *      for(i <- 0 until repeat) println(message)
  *   }
  * }
  *
  * Launcher.execute[MyCommand]("hello -r 3")  // hello x 3
  * }}}
  *
  */
class CommandLauncher(launcherInfo: LauncherInfo,
                      optionParser: OptionParser,
                      private[opts] val subCommands: Seq[CommandLauncher],
                      defaultCommand: Option[LauncherInstance => Any])
    extends LogSupport {

  def name: String        = launcherInfo.name
  def description: String = launcherInfo.description
  def usage: String       = launcherInfo.usage

  private[opts] def optionList: Seq[CLOption] = {
    optionParser.optionList
  }

  def addCommandModule[B: ru.TypeTag](name: String, description: String): CommandLauncher = {
    val moduleSurface = SurfaceFactory.ofType(implicitly[ru.TypeTag[B]].tpe)
    val subLauncher   = Launcher.newCommandLauncher(moduleSurface, name, description)
    add(name, subLauncher)
  }

  def addNestedCommandModule[B: ru.TypeTag](name: String, description: String)(
      nested: CommandLauncher => CommandLauncher): CommandLauncher = {
    val moduleSurface = SurfaceFactory.ofType(implicitly[ru.TypeTag[B]].tpe)
    val subLauncher   = nested(Launcher.newCommandLauncher(moduleSurface, name, description))
    add(name, subLauncher)
  }

  private[opts] def add(subCommandName: String, commandLauncher: CommandLauncher): CommandLauncher = {
    new CommandLauncher(launcherInfo, optionParser, subCommands :+ commandLauncher, defaultCommand)
  }

  private[opts] def execute(launcherConfig: LauncherConfig,
                            stack: List[LauncherInstance],
                            args: Seq[String],
                            showHelp: Boolean): LauncherResult = {
    val result = optionParser.parse(args.toArray)
    trace(result)

    val showHelpMessage = result.showHelp | showHelp

    optionParser.schema match {
      case c: ClassOptionSchema =>
        val msgpack = result.parseTree.toMsgPack
        val codec   = launcherConfig.codecFactory.withObjectMapCodec.of(c.surface)
        val h       = new MessageHolder
        codec.unpack(MessagePack.newDefaultUnpacker(msgpack), h)
        h.getError.map { e =>
          throw new IllegalArgumentException(s"Error occurered in launching ${c.surface}: ${e.getMessage}")
        }
        val obj = h.getLastValue

        //val obj       = result.buildObject(c.surface)
        val head      = LauncherInstance(this, obj)
        val nextStack = head :: stack

        if (result.unusedArgument.isEmpty) {
          // This Launcher is a leaf (= no more sub commands)
          if (showHelpMessage) {
            // Show the help message
            launcherConfig.helpMessagePrinter.printHelp(nextStack.map(_.launcher))
            LauncherResult(nextStack, None)
          } else {
            // Run the default command
            defaultCommand
              .map { defaultCommand =>
                defaultCommand(head)
              }
              .map { x =>
                LauncherResult(nextStack, Some(x))
              }
              .getOrElse {
                LauncherResult(nextStack, None)
              }
          }
        } else {
          // The first argument should be sub command name
          val subCommandName = result.unusedArgument.head
          findSubCommand(subCommandName) match {
            case Some(subCommand) =>
              subCommand.execute(launcherConfig, nextStack, result.unusedArgument.tail, showHelpMessage)
            case None =>
              throw new IllegalArgumentException(s"Unknown sub command: ${subCommandName}")
          }
        }
      case m: MethodOptionSchema =>
        // A command method inside the class
        if (result.unusedArgument.nonEmpty) {
          throw new IllegalArgumentException(s"Unknown arguments are found: [${result.unusedArgument.mkString(", ")}]")
        }

        val parentObj = stack.headOption.map(_.instance).getOrElse {
          throw new IllegalStateException("parent should not be empty")
        }

        if (showHelpMessage) {
          // Show the help message
          launcherConfig.helpMessagePrinter.printHelp(stack.map(_.launcher))
          LauncherResult(stack, None)
        } else {
          try {
            // parseTree -> msgpack -> method arguments
            val methodSurface = m.method
            val paramCodecs = methodSurface.args.map { x =>
              launcherConfig.codecFactory.of(x.surface)
            }
            val methodArgCodec = new ParamListCodec(
              methodSurface.name,
              methodSurface.args.toIndexedSeq,
              paramCodecs,
              // We need to supply default values of method parameters by using the parent object
              ParamListCodec.resolveMethodArgDefaultFromOwnerObject(parentObj)
            )

            val msgpack = result.parseTree.toMsgPack
            methodArgCodec
              .unpackMsgPack(msgpack).map { args =>
                val methodResult = methodSurface.call(parentObj, args: _*)
                LauncherResult(stack, Some(methodResult))
              }
              .getOrElse {
                throw new IllegalArgumentException(s"Failed to call ${methodSurface}: ${result.parseTree}")
              }
          } catch {
            case e: InvocationTargetException => throw e.getTargetException
            case other: Throwable             => throw other
          }
        }
    }
  }

  private def findSubCommand(name: String): Option[CommandLauncher] = {
    val cname = CName(name)
    subCommands.find(x => CName(x.name) == cname)
  }
}
