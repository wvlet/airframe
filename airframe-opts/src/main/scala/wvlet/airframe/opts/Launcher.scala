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

import wvlet.log.LogSupport
import wvlet.surface.reflect.{CName, MethodCallBuilder, SurfaceFactory}
import wvlet.surface.{MethodSurface, Surface, Zero}

import scala.reflect.runtime.{universe => ru}

/**
  * Command launcher
  */
object Launcher extends LogSupport {

  def of[A: ru.WeakTypeTag]: Launcher = {
    new Launcher(SurfaceFactory.of[A])
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l = Launcher.of[A]
    l.execute(args)
  }

  sealed trait Command {
    def name: String
    def description: String
    def printHelp: Unit
    def execute[A <: AnyRef](mainObj: A, args: Array[String], showHelp: Boolean): A
  }

  private[Launcher] class CommandDef(val method: MethodSurface, val command: command) extends Command with LogSupport {
    val name        = method.name
    val description = command.description
    def printHelp = {
      val parser = new OptionParser(method)
      parser.printUsage
    }
    def execute[A <: AnyRef](mainObj: A, args: Array[String], showHelp: Boolean): A = {
      trace(s"execute method: $name")
      val parser = new OptionParser(method)
      if (showHelp) {
        parser.printUsage
      } else {
        val r_sub = parser.parse(args)
        r_sub.build(new MethodCallBuilder(method, mainObj.asInstanceOf[AnyRef])).execute
      }
      mainObj
    }
  }

  private[Launcher] case class ModuleRef[A](m: ModuleDef[A]) extends Command with LogSupport {
    def name = m.name
    def printHelp = {
      debug("module help")
      new Launcher(m.moduleSurface).printHelp
    }
    def execute[A <: AnyRef](mainObj: A, args: Array[String], showHelp: Boolean): A = {
      trace(s"execute module: ${m.moduleSurface.name}")
      val result = new Launcher(m.moduleSurface).execute[A](args, showHelp)
      mainObj.asInstanceOf[CommandModule].executedModule = Some((name, result.asInstanceOf[AnyRef]))
      mainObj
    }

    def description = m.description
  }

  private[opts] val commandNameParam = "command name"
}

/**
  * Implement this trait to supply a default command invoked when no command name is specified.
  */
trait DefaultCommand {
  def default: Unit
}

/**
  * Command launcher.
  *
  * {{{
  *
  * class MyCommand(@option(prefix="-h,--help", description="display help", isHelp=true) help:Boolean) {
  *
  *
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
  *
  *
  *
  * }}}
  *
  * @author leo
  */
class Launcher(surface: Surface) extends LogSupport {

  import Launcher._

  lazy private val schema = ClassOptionSchema(surface)

  def execute[A <: AnyRef](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A <: AnyRef](args: Array[String], showHelp: Boolean = false): A = {
    val p = new OptionParser(schema)
    val r = p.parse(args)
    trace(s"parse tree: ${r.parseTree}")
    val mainObj: A = r.buildObjectWithFilter(surface, _ != commandNameParam).asInstanceOf[A]
    val cn: Option[String] =
      (for ((path, value) <- r.parseTree.dfs if path.fullPath == commandNameParam) yield value).toSeq.headOption
    val helpIsOn = r.showHelp || showHelp
    val result = try {
      for (commandName <- cn; c <- findCommand(commandName, mainObj))
        yield c.execute(mainObj, r.unusedArgument, helpIsOn)
    } catch {
      case e: InvocationTargetException => throw e.getTargetException
    }

    if (result.isEmpty) {
      if (helpIsOn) {
        printHelp(p, mainObj)
      } else if (classOf[DefaultCommand].isAssignableFrom(surface.rawType)) {
        // has a default command
        mainObj.asInstanceOf[DefaultCommand].default
      }
    }
    result getOrElse mainObj
  }

  def printHelp: Unit = {
    printHelp(OptionParser(surface), Zero.zeroOf(surface).asInstanceOf[AnyRef])
  }

  def printHelp(p: OptionParser, obj: AnyRef): Unit = {
    trace("print usage")
    p.printUsage

    val lst = commandList ++ moduleList(obj)
    if (!lst.isEmpty) {
      println("[commands]")
      val maxCommandNameLen = lst.map(_.name.length).max
      val format            = " %%-%ds\t%%s".format(math.max(10, maxCommandNameLen))
      lst.foreach { c =>
        println(format.format(c.name, c.description))
      }
    }
  }

  private lazy val commandList: Seq[Command] = {
    import wvlet.surface.reflect._
    trace(s"command class:${surface.name}")
    val methods = SurfaceFactory.methodsOf(surface)
    val lst     = for (m <- methods; c <- m.findAnnotationOf[command]) yield new CommandDef(m, c)
    lst
  }

  def moduleList[A <: AnyRef](mainObj: A): Seq[Command] = {
    if (CommandModule.isModuleClass(mainObj.getClass)) {
      mainObj.asInstanceOf[CommandModule].modules.map(ModuleRef(_))
    } else {
      Seq.empty
    }
  }

  private def findCommand(name: String, mainObj: AnyRef): Option[Command] = {

    def find(name: String): Option[Command] = {
      val cname = CName(name)
      trace(s"trying to find command:$cname")
      commandList.find(e => CName(e.name) == cname)
    }

    def findModule[A <: AnyRef](name: String, mainObj: A): Option[Command] =
      moduleList(mainObj).find(_.name == name)

    find(name) orElse findModule(name, mainObj) orElse {
      warn(s"Unknown command: $name")
      None
    }
  }
}
