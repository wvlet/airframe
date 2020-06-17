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

package wvlet.airframe.launcher

import java.lang.reflect.InvocationTargetException

import wvlet.airframe.codec.{MessageCodecFactory, MessageContext, ParamListCodec}
import wvlet.airframe.control.CommandLineTokenizer
import wvlet.airframe.launcher.OptionParser.CLOption
import wvlet.airframe.msgpack.spi.MessagePack
import wvlet.airframe.surface.reflect.ReflectSurfaceFactory
import wvlet.airframe.surface.{CName, MethodSurface, Surface}
import wvlet.log.LogSupport

import scala.reflect.runtime.{universe => ru}

/**
  * Command launcher
  */
object Launcher extends LogSupport {

  /**
    * Create a new Launcher of the given type
    *
    * @tparam A
    * @return
    */
  def of[A: ru.WeakTypeTag]: Launcher = {
    val cl = newCommandLauncher(ReflectSurfaceFactory.of[A], name = "", description = "")
    Launcher(LauncherConfig(), cl)
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l      = Launcher.of[A]
    val result = l.execute(args)
    result.getRootInstance.asInstanceOf[A]
  }

  /**
    * Create a launcher for a class
    *
    * @return
    */
  private[launcher] def newCommandLauncher(surface: Surface, name: String, description: String): CommandLauncher = {
    val parser = OptionParser(surface)

    // Generate a command-line usage message
    val defaultUsage =
      parser.schema.args.map(x => s"[${x}]").mkString(" ")

    import wvlet.airframe.surface.reflect._
    val command = surface.findAnnotationOf[command]
    // If the user specified usage and description via @command annotation, use them.
    val commandUsage       = command.map(_.usage()).find(_.nonEmpty).getOrElse(defaultUsage)
    val commandDescription = command.map(_.description()).find(_.nonEmpty).getOrElse(description.trim)
    val commandName        = if (name.nonEmpty) name else CName.toNaturalName(surface.name).replaceAll("\\s+", "_")

    // Find sub commands marked with [[wvlet.airframe.opts.command]] annotation
    import wvlet.airframe.surface.reflect._
    val methods = ReflectSurfaceFactory.methodsOf(surface)
    val subCommands = for (m <- methods; c <- m.findAnnotationOf[command]) yield {
      newMethodLauncher(m, c)
    }

    // Find the default command
    val defaultCommand = ReflectSurfaceFactory
      .methodsOf(surface)
      .find { m =>
        import wvlet.airframe.surface.reflect._
        m.findAnnotationOf[command] match {
          case Some(cmd) => cmd.isDefault
          case None      => false
        }
      }
      .map { m => { li: LauncherInstance => m.call(li.instance) } }

    new CommandLauncher(
      LauncherInfo(commandName, commandDescription, commandUsage),
      parser,
      subCommands,
      defaultCommand
    )
  }

  /**
    * Create a launcher from a method in a class
    */
  private def newMethodLauncher(m: MethodSurface, command: command): CommandLauncher = {
    val parser       = new OptionParser(m)
    val defaultUsage = parser.schema.args.map(x => s"[${x.name}]").mkString(" ")

    val description =
      Some(command.description())
        .map(x => x)
        .find(_.nonEmpty)
        .getOrElse("")

    val usage = {
      val argLine = Some(command.usage())
        .map(x => x)
        .find(_.nonEmpty)
        .getOrElse(defaultUsage)
      s"${m.name} ${argLine}"
    }

    val li = LauncherInfo(m.name, description, usage, command.isDefault)
    new CommandLauncher(li, parser, Seq.empty, None)
  }
}

private[launcher] case class LauncherConfig(
    helpMessagePrinter: HelpMessagePrinter = HelpMessagePrinter.default,
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactory,
    // command name -> default action
    defaultCommand: LauncherInstance => Any = { li: LauncherInstance => println("Type --help to see the usage") }
)

case class Launcher private[launcher] (config: LauncherConfig, private[launcher] val mainLauncher: CommandLauncher) {
  def printHelp: Unit = {
    mainLauncher.printHelpInternal(config, List(mainLauncher))
  }

  /**
    * Set a function to be used when there is no command is specified
    */
  def withDefaultCommand(newDefaultCommand: LauncherInstance => Any): Launcher = {
    this.copy(config = config.copy(defaultCommand = newDefaultCommand))
  }

  def withHelpMessagePrinter(newHelpMessagePrinter: HelpMessagePrinter): Launcher = {
    this.copy(config = config.copy(helpMessagePrinter = newHelpMessagePrinter))
  }

  def withCodecFactory(newCodecFactory: MessageCodecFactory): Launcher = {
    this.copy(config = config.copy(codecFactory = newCodecFactory))
  }

  def execute(argLine: String): LauncherResult = execute(CommandLineTokenizer.tokenize(argLine))
  def execute(args: Array[String]): LauncherResult = {
    mainLauncher.execute(config, List.empty, args.toSeq, showHelp = false)
  }

  /**
    * Add a sub command module to the launcher
    *
    * @param name sub command name
    * @param description
    * @tparam M
    * @return
    */
  def addModule[M: ru.TypeTag](name: String, description: String): Launcher = {
    Launcher(config, mainLauncher.addCommandModule[M](name, description))
  }

  def add(l: Launcher, name: String, description: String): Launcher = {
    Launcher(config, mainLauncher.add(name, description, l.mainLauncher))
  }
}

/**
  * Command execution results
  *
  * @param result
  */
case class LauncherResult(launcherStack: List[LauncherInstance], result: Option[Any]) {
  require(launcherStack.nonEmpty, "launcherStack should not be empty")

  def getRootInstance: Any  = launcherStack.reverse.head.instance
  def executedInstance: Any = launcherStack.head.instance
}
case class LauncherInstance(launcher: CommandLauncher, instance: Any)

case class LauncherInfo(name: String, description: String, usage: String, isDefault: Boolean = false)

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
  */
class CommandLauncher(
    private[launcher] val launcherInfo: LauncherInfo,
    private[launcher] val optionParser: OptionParser,
    private[launcher] val subCommands: Seq[CommandLauncher],
    defaultCommand: Option[LauncherInstance => Any]
) extends LogSupport {
  def name: String        = launcherInfo.name
  def description: String = launcherInfo.description
  def usage: String       = launcherInfo.usage

  private[launcher] def withLauncherInfo(name: String, description: String): CommandLauncher = {
    new CommandLauncher(LauncherInfo(name, description, launcherInfo.usage), optionParser, subCommands, defaultCommand)
  }

  private[launcher] def optionList: Seq[CLOption] = {
    optionParser.optionList
  }

  private[launcher] def addCommandModule[B: ru.TypeTag](name: String, description: String): CommandLauncher = {
    val moduleSurface = ReflectSurfaceFactory.ofType(implicitly[ru.TypeTag[B]].tpe)
    val subLauncher   = Launcher.newCommandLauncher(moduleSurface, name, description)
    add(name, description, subLauncher)
  }

  private[launcher] def add(name: String, description: String, commandLauncher: CommandLauncher): CommandLauncher = {
    new CommandLauncher(
      launcherInfo,
      optionParser,
      subCommands :+ commandLauncher.withLauncherInfo(name, description),
      defaultCommand
    )
  }

  private[launcher] def printHelp(launcherConfig: LauncherConfig, stack: List[LauncherInstance]): Unit = {
    printHelpInternal(launcherConfig, stack.map(_.launcher))
  }

  private[launcher] def printMethodHelp(
      launcherConfig: LauncherConfig,
      m: MethodOptionSchema,
      stack: List[LauncherInstance]
  ): Unit = {
    val h             = stack.head
    val globalOptions = stack.tail.flatMap(_.launcher.optionParser.optionList)

    val li = h.launcher.launcherInfo

    val help = launcherConfig.helpMessagePrinter.render(
      commandName = li.name,
      arguments = m.args,
      oneLineUsage = if (li.usage.isEmpty) None else Some(li.usage),
      description = li.description,
      options = m.options,
      globalOptions = globalOptions,
      subCommands = Seq.empty
    )

    print(help)
  }

  private[launcher] def printHelpInternal(launcherConfig: LauncherConfig, stack: List[CommandLauncher]): Unit = {
    val l             = stack.head
    val schema        = l.optionParser.schema
    val globalOptions = stack.tail.flatMap(_.optionParser.optionList)

    val help = launcherConfig.helpMessagePrinter.render(
      commandName = l.name,
      arguments = schema.args,
      oneLineUsage = if (l.usage.isEmpty) None else Some(l.usage),
      description = l.description,
      options = schema.options,
      globalOptions = globalOptions,
      subCommands = l.subCommands.filterNot(x => x.launcherInfo.isDefault)
    )

    print(help)
  }

  private[launcher] def execute(
      launcherConfig: LauncherConfig,
      stack: List[LauncherInstance],
      args: Seq[String],
      showHelp: Boolean
  ): LauncherResult = {
    val result = optionParser.parse(args.toArray)
    trace(result)

    val showHelpMessage = result.showHelp | showHelp

    optionParser.schema match {
      case c: ClassOptionSchema =>
        val parseTree_mp = result.parseTree.toMsgPack
        val codec        = launcherConfig.codecFactory.withMapOutput.of(c.surface)
        val h            = new MessageContext
        codec.unpack(MessagePack.newUnpacker(parseTree_mp), h)
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
            printHelp(launcherConfig, nextStack)
            LauncherResult(nextStack, None)
          } else {
            // Run the default command
            defaultCommand
              .map { defaultCommand => defaultCommand(head) }
              .map { x => LauncherResult(nextStack, Some(x)) }
              .getOrElse {
                LauncherResult(nextStack, None)
              }
          }
        } else {
          // The first argument should be sub command name
          val subCommandName = result.unusedArgument.head
          findSubCommand(subCommandName) match {
            case Some(subCommand) =>
              subCommand.execute(launcherConfig, nextStack, result.unusedArgument.tail.toIndexedSeq, showHelpMessage)
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
          printMethodHelp(launcherConfig, m, LauncherInstance(this, parentObj) :: stack)
          LauncherResult(stack, None)
        } else {
          try {
            // parseTree -> msgpack -> method arguments
            val methodSurface = m.method
            val paramCodecs   = methodSurface.args.map { x => launcherConfig.codecFactory.of(x.surface) }
            val methodArgCodec = new ParamListCodec(
              methodSurface.name,
              methodSurface.args.toIndexedSeq,
              paramCodecs,
              // We need to supply method owner object to resolve function arg values
              methodOwner = Some(parentObj)
            )

            val msgpack = result.parseTree.toMsgPack
            methodArgCodec
              .unpackMsgPack(msgpack).map { args =>
                trace(s"calling method ${methodSurface} with args: ${args.mkString(", ")}")
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
