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
import wvlet.airframe.surface.reflect.{CName, MethodCallBuilder, SurfaceFactory}
import wvlet.airframe.surface.{MethodSurface, Surface, Zero}

import scala.reflect.runtime.{universe => ru}

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
case class SubCommand(name: String, launcher: Launcher)

case class Launcher(surface: Surface, name: String, description: String = "", subCommands: Seq[SubCommand] = Seq.empty)
    extends LogSupport {

  import Launcher._

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
    add(name, new Launcher(moduleSurface, name, description))
  }

  def add(subCommandName: String, launcher: Launcher): Launcher = {
    Launcher(surface, this.name, this.description, subCommands :+ SubCommand(subCommandName, launcher))
  }

  private[opts] def findSubCommand(name: String): Option[Launcher] = {
    subCommands.find(_.name == name).map(_.launcher)
  }

  def execute[A <: AnyRef](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A <: AnyRef](args: Array[String], showHelp: Boolean = false): A = {

    // Process args using the
    val argProcessor = new ArgProcessor(this)
    val result       = argProcessor.process(args)

    //for (p <- surface.params) {}

    // TODO
    null.asInstanceOf[A]
  }

  private[opts] def findDefaultCommand: Option[MethodSurface] = {
    SurfaceFactory
      .methodsOf(surface)
      .find { m =>
        import wvlet.airframe.surface.reflect._
        m.findAnnotationOf[defaultCommand].isDefined
      }
  }

  def printHelp: Unit = {
    trace("print usage")
    val p = OptionParser(surface)
    p.printUsage

    // TODO Show sub commend lists
  }

  //    val lst = commandList ++ subCommands
  //    if (!lst.isEmpty) {
  //      println("[commands]")
  //      val maxCommandNameLen = lst.map(_.name.length).max
  //      val format            = " %%-%ds\t%%s".format(math.max(10, maxCommandNameLen))
  //      lst.foreach { c =>
  //        println(format.format(c.name, c.description))
  //      }
  //    }
// }

//  private def commandList: Seq[Command] = {
//    import wvlet.airframe.surface.reflect._
//    trace(s"command class:${surface.name}")
//    val methods = SurfaceFactory.methodsOf(surface)
//    val lst     = for (m <- methods; c <- m.findAnnotationOf[command]) yield new CommandMethod(m, c)
//    lst
//  }

//  private def findCommand(name: String, mainObj: AnyRef): Option[Command] = {
//
//    def find(name: String): Option[Command] = {
//      val cname = CName(name)
//      trace(s"trying to find command:$cname")
//      commandList.find(e => CName(e.name) == cname)
//    }
//
//    def findModule[A <: AnyRef](name: String, mainObj: A): Option[Command] =
//      subCommands.find(x => x.name == name)
//
//    find(name) orElse findModule(name, mainObj) orElse {
//      warn(s"Unknown command: $name")
//      None
//    }
//  }
}

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
    new Launcher(SurfaceFactory.of[A], "")
  }

  def execute[A: ru.WeakTypeTag](argLine: String): A = execute(CommandLineTokenizer.tokenize(argLine))
  def execute[A: ru.WeakTypeTag](args: Array[String]): A = {
    val l = Launcher.of[A]
    l.execute(args)
  }
}

///**
//  * Based trait for managing nested traits
//  */
//sealed trait Command {
//  def name: String
//  def description: String
//  def printHelp: Unit
//  def execute[A <: AnyRef](mainParser: OptionParser, mainObj: A, args: Array[String], showHelp: Boolean): A
//}
//
//private[Launcher] class CommandMethod(val method: MethodSurface, val command: command)
//  extends Command
//    with LogSupport {
//  val name        = method.name
//  val description = command.description
//  def printHelp = {
//    val parser = new OptionParser(method)
//    parser.printUsage
//  }
//  def execute[A <: AnyRef](mainParser: OptionParser, mainObj: A, args: Array[String], showHelp: Boolean): A = {
//    trace(s"execute method: $name")
//    val parser = new OptionParser(method)
//    if (showHelp) {
//      parser.printUsage
//      val globalOptionList = mainParser.createOptionList
//      // Show global options
//      if (globalOptionList.nonEmpty) {
//        println("\n[global options]")
//        println(globalOptionList.mkString("\n"))
//      }
//    } else {
//      val r_sub = parser.parse(args)
//      r_sub.build(new MethodCallBuilder(method, mainObj.asInstanceOf[AnyRef])).execute
//    }
//    mainObj
//  }
//}
//
//private[Launcher] case class CommandModule(surface: Surface, name: String, description: String)
//  extends Command
//    with LogSupport {
//  def printHelp = {
//    debug("module help")
//    new Launcher(surface, name).printHelp
//  }
//  def execute[A <: AnyRef](mainParser: OptionParser, mainObj: A, args: Array[String], showHelp: Boolean): A = {
//    trace(s"execute module: ${name}")
//    val result = new Launcher(surface, name).execute[A](args, showHelp)
//    mainObj
//  }
//}
//
//private[opts] val commandNameParam = "command name"
