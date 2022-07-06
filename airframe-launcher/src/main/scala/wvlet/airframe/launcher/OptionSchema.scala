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
import wvlet.airframe.launcher.OptionParser.{CLArgItem, CLArgument, CLOption, OptionParserResult}
import wvlet.airframe.surface.{MethodSurface, Surface}
import wvlet.log.LogSupport

/**
  * Schema of the command line options
  */
sealed trait OptionSchema extends LogSupport {
  val options: Seq[CLOption]
  val args: Seq[CLArgItem] // must be sorted by arg.index in ascending order

  protected lazy val symbolTable = {
    var h = Map[String, CLOption]()
    options.foreach { case opt: CLOption =>
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

  override def toString: String = "options:[%s], args:[%s]".format(options.mkString(", "), args.mkString(", "))
}

object ClassOptionSchema extends LogSupport {
  import wvlet.airframe.surface.reflect._

  /**
    * Create an option schema from a given class definition
    */
  def apply(surface: Surface, path: Path = Path.current, argIndexOffset: Int = 0): ClassOptionSchema = {
    var argCount = argIndexOffset
    val o        = Seq.newBuilder[CLOption]
    val a        = Seq.newBuilder[CLArgItem]
    for (p <- surface.params) {
      val nextPath = path / p.name

      val optAnnot = p.findAnnotationOf[option]
      val argAnnot = p.findAnnotationOf[argument]

      // @option
      optAnnot.foreach { opt => o += CLOption(nextPath, opt, p) }

      // @argument
      argAnnot.foreach { arg =>
        a += CLArgument(nextPath, arg, argCount, p)
        argCount += 1
      }

      if (optAnnot.isEmpty || argAnnot.isEmpty) {
        // The parameter might be a nested object
        val nested = ClassOptionSchema(p.surface, nextPath, argCount)
        o ++= nested.options
        a ++= nested.args
        argCount += nested.args.length
      }
    }
    new ClassOptionSchema(surface, o.result(), a.result().sortBy(x => x.argIndex))
  }
}

/**
  * OptionSchema created from a class definition
  *
  * @param surface
  */
class ClassOptionSchema(val surface: Surface, val options: Seq[CLOption], val args: Seq[CLArgItem])
    extends OptionSchema {}

object MethodOptionSchema {
  import wvlet.airframe.surface.reflect._

  def apply(method: MethodSurface, path: Path = Path.current, argIndexOffset: Int = 0): MethodOptionSchema = {
    // TODO Merge this method with ClassOptionSchema.apply as the logic is almost the same
    val o = Seq.newBuilder[CLOption]
    val a = Seq.newBuilder[CLArgItem]

    var argCount = argIndexOffset
    for (p <- method.args) {
      val nextPath = path / p.name
      // Find options
      val optAnnot = p.findAnnotationOf[option]
      val argAnnot = p.findAnnotationOf[argument]

      optAnnot.foreach { opt =>
        o += CLOption(Path(p.name), opt, p)
      }
      argAnnot.foreach { arg =>
        a += CLArgument(Path(p.name), arg, argCount, p)
        argCount += 1
      }

      if (optAnnot.isEmpty || argAnnot.isEmpty) {
        // The method argument might be a nested object
        val nested = ClassOptionSchema(p.surface, nextPath, argCount)
        o ++= nested.options
        a ++= nested.args
        argCount += nested.args.length
      }
    }
    new MethodOptionSchema(method, o.result(), a.result().sortBy(_.argIndex))
  }
}

/**
  * OptionSchema created from a method definition
  *
  * @param method
  */
class MethodOptionSchema(
    private[launcher] val method: MethodSurface,
    val options: Seq[CLOption],
    val args: Seq[CLArgItem]
) extends OptionSchema
