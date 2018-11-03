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
  def of[A: ru.WeakTypeTag]: Launcher = {
    ClassLauncher(SurfaceFactory.of[A], name = "", description = "")
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l      = Launcher.of[A]
    val result = l.execute(args)
    result.getRootInstance.asInstanceOf[A]
  }

  val defaultUsageTemplate =
    """|usage:$COMMAND$ $ARGUMENT_LIST$
       |  $DESCRIPTION$
       |$OPTION_LIST$""".stripMargin
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
case class LauncherInstance(launcher: Launcher, instance: Any)

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
abstract class Launcher extends LogSupport {
  import Launcher._

  def name: String
  def subCommands: Seq[Launcher]

  def optionList: Seq[CLOption] = {
    optionParser.optionList
  }

  def optionParser: OptionParser

  protected def helpMessageTemplate = defaultUsageTemplate
  //lazy private[opts] val schema = ClassOptionSchema(surface)

  /**
    * Add a sub command to the launcher
    * @param subCommandName
    * @param description
    * @tparam A
    * @return
    */
  def addSubCommand[A: ru.TypeTag](name: String, description: String = ""): Launcher = {
    val moduleSurface = SurfaceFactory.ofType(implicitly[ru.TypeTag[A]].tpe)
    add(name, ClassLauncher(moduleSurface, name, description))
  }

  def add(subCommandName: String, launcher: Launcher): Launcher

  def execute(argLine: String): LauncherResult = execute(CommandLineTokenizer.tokenize(argLine))
  def execute(args: Array[String], showHelp: Boolean = false): LauncherResult =
    execute(List.empty, args.toSeq, showHelp)
  private[opts] def execute(stack: List[LauncherInstance], args: Seq[String], showHelp: Boolean): LauncherResult

  private[opts] def findDefaultCommand: Option[MethodSurface]
  private[opts] def findSubCommand(name: String): Option[Launcher] = {
    val cname = CName(name)
    subCommands.find(x => CName(x.name) == cname)
  }

  def description: String
  def usage: String

  protected def defaultUsage: String = {
    val l = for (a <- optionParser.schema.args) yield {
      a.name
    }
    l.map("[%s]".format(_)).mkString(" ")
  }

  def printUsage = {
    val m = StringTemplate.eval(helpMessageTemplate) {
      Map('ARGUMENT_LIST -> usage, 'OPTION_LIST -> createOptionHelpMessage, 'DESCRIPTION -> description)
    }
    print(m)
  }

  def printHelp(stack: List[LauncherInstance] = Nil): Unit = {
    trace("print usage")
    val p = optionParser
    printUsage

    // Show parent options
    val parentOptions = stack.flatMap { x =>
      x.launcher.optionList
    }
    if (parentOptions.nonEmpty) {
      println("[global options]")
      println(parentOptions.mkString("\n"))
    }

    if (subCommands.nonEmpty) {
      println("[commands]")

      val maxCommandNameLen = subCommands.map(_.name.length).max
      val format            = " %%-%ds\t%%s".format(math.max(10, maxCommandNameLen))
      // Show sub commend lists
      subCommands.foreach { c =>
        println(format.format(c.name, c.description))
      }
    }
  }

  def createOptionHelpMessage = {
    val optionList = createOptionList
    val b          = new StringBuilder
    if (optionList.nonEmpty) {
      b.append("[options]\n")
      b.append(optionList.mkString("\n") + "\n")
    }
    b.result
  }

  def createOptionList: Seq[String] = {
    val optDscr: Seq[(CLOption, String)] = for (o <- optionList) yield {
      val prefixes = o.prefixes
      val hasShort = prefixes.exists(_.length == 2)
      val hasAlias = prefixes.exists(_.length > 2)
      val l        = new StringBuilder
      l.append(prefixes.mkString(", "))

      if (o.takesArgument) {
        if (hasAlias) {
          l append ":"
        } else if (hasShort) {
          l append " "
        }
        l append "[%s]".format(o.param.name.toUpperCase)
      }
      (o, l.toString)
    }

    val optDscrLenMax =
      if (optDscr.isEmpty) {
        0
      } else {
        optDscr.map(_._2.length).max
      }

    def genDescription(opt: CLOption) = {
      opt.annot.description()
    }

    val s = for (x <- optDscr) yield {
      val paddingLen = optDscrLenMax - x._2.length
      val padding    = Array.fill(paddingLen)(" ").mkString
      " %s%s  %s".format(x._2, padding, genDescription(x._1))
    }
    s
  }
}

import wvlet.airframe.surface.reflect._

object ClassLauncher {

  def apply(surface: Surface, name: String, description: String): ClassLauncher = {
    val methods = SurfaceFactory.methodsOf(surface)
    import wvlet.airframe.surface.reflect._
    // Register sub command functions marked with [[wvlet.airframe.opts.command]] annotation
    val subCommands = for (m <- methods; c <- m.findAnnotationOf[command]) yield {
      new LocalMethodLauncher(m, c)
    }
    new ClassLauncher(surface, name, description, subCommands)
  }
}

/**
  * Command definition using a class.
  *
  * Constructor parameters becomes command-line option parameters, and functions
  * annotated with {{@command}} will be sub commands.
  *
  * @param surface
  * @param name
  * @param description
  * @param subCommands
  */
private[opts] class ClassLauncher(surface: Surface,
                                  val name: String,
                                  val description: String,
                                  val subCommands: Seq[Launcher])
    extends Launcher {

  def add(subCommandName: String, launcher: Launcher): Launcher = {
    new ClassLauncher(surface, this.name, this.description, subCommands :+ launcher)
  }

  override def optionParser = OptionParser(surface)

  override private[opts] def findDefaultCommand: Option[MethodSurface] = {
    SurfaceFactory
      .methodsOf(surface)
      .find { m =>
        import wvlet.airframe.surface.reflect._
        m.findAnnotationOf[defaultCommand].isDefined
      }
  }

  override def execute(stack: List[LauncherInstance], args: Seq[String], showHelp: Boolean): LauncherResult = {
    val result = optionParser.parse(args.toArray)
    debug(result)
    val obj       = result.buildObject(surface)
    val nextStack = LauncherInstance(this, obj) :: stack

    val showHelpMessage = result.showHelp | showHelp

    if (result.unusedArgument.isEmpty) {
      // This Launcher is a leaf (= no more sub commands)
      if (showHelpMessage) {
        // Show the help message
        printHelp(stack)
        LauncherResult(nextStack, None)
      } else {
        // Run the default command
        findDefaultCommand
          .map { defaultCommand =>
            defaultCommand.call(obj)
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
    }
  }

  override def usage = {
    surface
      .findAnnotationOf[command]
      .map(_.usage)
      .find(_.nonEmpty)
      .getOrElse(defaultUsage)
  }
}

private[opts] class LocalMethodLauncher(methodSurface: MethodSurface, method: command) extends Launcher {
  override def name: String                                              = methodSurface.name
  override def subCommands: Seq[Launcher]                                = Seq.empty
  override def add(subCommandName: String, launcher: Launcher): Launcher = ???

  override private[opts] def findDefaultCommand: Option[MethodSurface] = None

  override def optionParser = new OptionParser(methodSurface)

  override def execute(stack: List[LauncherInstance], args: Seq[String], showHelp: Boolean): LauncherResult = {
    val result = optionParser.parse(args.toArray)
    val parentObj = stack.headOption.map(_.instance).getOrElse {
      throw new IllegalStateException("parent should not be empty")
    }
    val showHelpMessage = result.showHelp | showHelp

    if (showHelpMessage) {
      printHelp(stack)
      LauncherResult(stack, None)
    } else {
      if (result.unusedArgument.nonEmpty) {
        throw new IllegalArgumentException(
          s"Unknown command-line arguments are found: ${result.unusedArgument.mkString(", ")}")
      }
      try {
        val m            = new MethodCallBuilder(methodSurface, parentObj.asInstanceOf[AnyRef])
        val methodResult = result.build(m).execute
        LauncherResult(stack, Some(methodResult))
      } catch {
        case e: InvocationTargetException => throw e.getTargetException
        case other: Throwable             => throw other
      }
    }
  }

  override def description = {
    Some(method.description())
      .map(x => x)
      .find(_.nonEmpty)
      .getOrElse("")
  }

  override def usage = {
    val argLine = Some(method.description())
      .map(x => x)
      .find(_.nonEmpty)
      .getOrElse(defaultUsage)
    s"${methodSurface.name} ${argLine}"
  }
}
