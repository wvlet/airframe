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

import wvlet.airframe.opts.OptionParser.CLOption
import wvlet.airframe.surface.reflect.{CName, MethodCallBuilder, SurfaceFactory}
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
  def of[A: ru.WeakTypeTag]: Launcher[A] = {
    newLauncher[A](SurfaceFactory.of[A], name = "", description = "", helpMessagePrinter = new HelpMessagePrinter)
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l      = Launcher.of[A]
    val result = l.execute(args)
    result.getRootInstance.asInstanceOf[A]
  }

  private def newLauncher[A](surface: Surface,
                             name: String,
                             description: String,
                             helpMessagePrinter: HelpMessagePrinter): Launcher[A] = {
    val parser = OptionParser(surface)

    val defaultUsage =
      parser.schema.args.map(x => s"[${x}]").mkString(" ")

    import wvlet.airframe.surface.reflect._
    val usage =
      surface
        .findAnnotationOf[command]
        .map(_.usage)
        .find(_.nonEmpty)
        .getOrElse(defaultUsage)

    // Find sub commands
    val methods = SurfaceFactory.methodsOf(surface)
    import wvlet.airframe.surface.reflect._
    // Register sub command functions marked with [[wvlet.airframe.opts.command]] annotation
    val subCommands = for (m <- methods; c <- m.findAnnotationOf[command]) yield {
      newMethodLauncher(m, c, helpMessagePrinter)
    }

    // Find the default command
    val defaultCommand = SurfaceFactory
      .methodsOf(surface)
      .find { m =>
        import wvlet.airframe.surface.reflect._
        m.findAnnotationOf[defaultCommand].isDefined
      }
      .map { m => x: A =>
        m.call(x)
      }

    new Launcher[A](LauncherInfo(name, description, usage), parser, subCommands, defaultCommand, helpMessagePrinter)
  }

  private def newMethodLauncher(m: MethodSurface,
                                command: command,
                                helpMessagePrinter: HelpMessagePrinter): Launcher[_] = {

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
    new Launcher(li, parser, Seq.empty, None, helpMessagePrinter)
  }

}

/**
  * Command execution results
  * @param executedModule
  * @param result
  */
case class LauncherResult(launcherStack: List[LauncherInstance], result: Option[Any]) {
  require(launcherStack.nonEmpty, "launcherStack should not be empty")

  def getRootInstance: Any = launcherStack.reverse.head.instance
}
case class LauncherInstance(launcher: Launcher[_], instance: Any)

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
class Launcher[A](launcherInfo: LauncherInfo,
                  optionParser: OptionParser,
                  subCommands: Seq[Launcher[_]],
                  defaultCommand: Option[A => Any],
                  helpMessagePrinter: HelpMessagePrinter)
    extends LogSupport {
  import Launcher._

  def name: String = launcherInfo.name
  def optionList: Seq[CLOption] = {
    optionParser.optionList
  }

  /**
    * Set a function to be used when there is no command is specified
    * @param command
    * @tparam U
    * @return
    */
  def withDefaultCommand(body: A => Any): Launcher[A] = {
    new Launcher(launcherInfo, optionParser, subCommands, Some(body), helpMessagePrinter)
  }

  def withHelpMessagePrinter(newHelpMessagePrinter: HelpMessagePrinter): Launcher[A] = {
    new Launcher(launcherInfo, optionParser, subCommands, defaultCommand, newHelpMessagePrinter)
  }

  /**
    * Add a sub command module to the launcher
    * @param subCommandName
    * @param description
    * @tparam A
    * @return
    */
  def addModule[A: ru.TypeTag](name: String, description: String = ""): Launcher[A] = {
    val moduleSurface = SurfaceFactory.ofType(implicitly[ru.TypeTag[A]].tpe)
    add(name, Launcher.newLauncher(moduleSurface, name, description, helpMessagePrinter))
  }

  def add(subCommandName: String, launcher: Launcher[_]): Launcher[A] = {
    new Launcher(launcherInfo, optionParser, subCommands :+ launcher, defaultCommand, helpMessagePrinter)
  }

  def execute(argLine: String): LauncherResult = execute(CommandLineTokenizer.tokenize(argLine))
  def execute(args: Array[String], showHelp: Boolean = false): LauncherResult =
    execute(List.empty, args.toSeq, showHelp)

  private[opts] def execute(stack: List[LauncherInstance], args: Seq[String], showHelp: Boolean): LauncherResult = {
    val result = optionParser.parse(args.toArray)
    debug(result)

    // For class call
    val obj       = result.buildObject(surface)
    val nextStack = LauncherInstance(this, obj) :: stack
    // For method call
    //    val parentObj = stack.headOption.map(_.instance).getOrElse {
    //      throw new IllegalStateException("parent should not be empty")
    //    }

    val showHelpMessage = result.showHelp | showHelp

    if (result.unusedArgument.isEmpty) {
      // This Launcher is a leaf (= no more sub commands)
      if (showHelpMessage) {
        // Show the help message
        helpMessagePrinter.printHelp(stack)
        LauncherResult(nextStack, None)
      } else {
        // Run the default command
        defaultCommand
          .map { defaultCommand =>
            defaultCommand(obj)
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
          subCommand.execute(nextStack, result.unusedArgument.tail, showHelpMessage)
        case None =>
          throw new IllegalArgumentException(s"Unknown sub command: ${subCommandName}")
      }

      // For Method call
      //      try {
      //        val m            = new MethodCallBuilder(methodSurface, parentObj.asInstanceOf[AnyRef])
      //        val methodResult = result.build(m).execute
      //        LauncherResult(stack, Some(methodResult))
      //      } catch {
      //        case e: InvocationTargetException => throw e.getTargetException
      //        case other: Throwable             => throw other
      //      }
    }
  }

  private[opts] def findSubCommand(name: String): Option[Launcher[_]] = {
    val cname = CName(name)
    subCommands.find(x => CName(x.name) == cname)
  }
}
