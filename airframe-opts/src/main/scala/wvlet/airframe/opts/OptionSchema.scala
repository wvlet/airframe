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
package wvlet.airframe.opts
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

/**
  * Schema of the command line options
  */
trait OptionSchema extends LogSupport {

  val options: Seq[CLOption]
  val args: Seq[CLArgItem] // must be sorted by arg.index in ascending order

  protected lazy val symbolTable = {
    var h = Map[String, CLOption]()
    options.foreach {
      case opt: CLOption =>
        for (p <- opt.prefixes) {
          h += p -> opt
        }
    }
    h
  }

  def apply(name: String): CLOption = symbolTable.apply(name)

  def findOption(name: String): Option[CLOption] = symbolTable.get(name)
  def findFlagOption(name: String): Option[CLOption] = {
    findOption(name) filterNot (_.takesArgument)
  }
  def findOptionNeedsArg(name: String): Option[CLOption] = {
    findOption(name) filter (_.takesArgument)
  }

  def findArgumentItem(argIndex: Int): Option[CLArgItem] = {
    if (args.isDefinedAt(argIndex)) Some(args(argIndex)) else None
  }

  def description: String
  def usage: String

  protected def defaultUsage: String = {
    val l = for (a <- args) yield {
      a.name
    }
    l.map("[%s]".format(_)).mkString(" ")
  }

  override def toString = "options:[%s], args:[%s]".format(options.mkString(", "), args.mkString(", "))
}

object ClassOptionSchema extends LogSupport {
  import wvlet.airframe.surface.reflect._

  /**
    * Create an option schema from a given class definition
    */
  def apply(surface: Surface, path: Path = Path.current, argIndexOffset: Int = 0): ClassOptionSchema = {
    var argCount = 0
    val o        = Array.newBuilder[CLOption]
    val a        = Array.newBuilder[CLArgItem]
    for (p <- surface.params) {
      val nextPath = path / p.name

      val optAnnot = p.findAnnotationOf[option]
      val argAnnot = p.findAnnotationOf[argument]

      // @option
      optAnnot.map { opt =>
        o += new CLOption(nextPath, opt, p)
      }

      // @argument
      argAnnot.map { arg =>
        a += new CLArgument(nextPath, arg, argIndexOffset + argCount, p)
        argCount += 1
      }

      if (optAnnot.isEmpty || argAnnot.isEmpty) {
        // The parameter might be a nested parameter object
        val nested = ClassOptionSchema(p.surface, nextPath, argCount)
        o ++= nested.options
        a ++= nested.args
        argCount += nested.args.length
      }
    }
    new ClassOptionSchema(surface, o.result.toSeq, a.result().toSeq.sortBy(x => x.argIndex))
  }
}

/**
  * OptionSchema created from a class definition
  *
  * @param surface
  */
class ClassOptionSchema(val surface: Surface, val options: Seq[CLOption], val args: Seq[CLArgItem])
    extends OptionSchema {

  def description = {
    surface.rawType.getDeclaredAnnotations
      .collectFirst {
        case c: command => c.description
      }
      .getOrElse("")
  }

  override def usage = {
    surface.rawType.getDeclaredAnnotations
      .collectFirst {
        case c: command if !c.usage.isEmpty => c.usage
      }
      .getOrElse(defaultUsage)
  }

}

/**
  * OptionSchema created from a method definition
  *
  * @param method
  */
class MethodOptionSchema(method: MethodSurface) extends OptionSchema {
  import wvlet.airframe.surface.reflect._
  val options =
    for (p <- method.args; opt <- p.findAnnotationOf[option]) yield new CLOption(Path(p.name), opt, p)

  val args = {
    var argCount = -1
    val l = for (p <- method.args; arg <- p.findAnnotationOf[argument]) yield {
      (new CLArgument(Path(p.name), arg, { argCount += 1; argCount }, p)).asInstanceOf[CLArgItem]
    }
    l.sortBy(x => x.argIndex)
  }

  def description = {
    method match {
      case m: ReflectMethodSurface if m.getMethod.isDefined =>
        m.getMethod.get.getDeclaredAnnotations
          .collectFirst {
            case c: command => c.description
          }
          .getOrElse("")
      case _ => ""
    }
  }

  override def usage = {
    val argLine =
      method match {
        case m: ReflectMethodSurface if m.getMethod.isDefined =>
          m.getMethod.get.getDeclaredAnnotations
            .collectFirst {
              case c: command if !c.usage.isEmpty => c.usage
            }
            .getOrElse(defaultUsage)
        case _ => defaultUsage
      }
    "%s %s".format(method.name, argLine)
  }

}
