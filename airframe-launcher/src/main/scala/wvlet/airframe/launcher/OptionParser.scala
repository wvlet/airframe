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

//--------------------------------------
//
// OptionParser.scala
// Since: 2012/07/06 4:05 PM
//
//--------------------------------------

import wvlet.airframe.control.CommandLineTokenizer
import wvlet.airframe.launcher.StringTree.{Leaf, SeqLeaf}
import wvlet.airframe.surface._
import wvlet.airframe.surface.reflect.{GenericBuilder, ObjectBuilder, Path, ReflectSurfaceFactory}
import wvlet.log.{LogSupport, Logger}

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
  * Creates option parsers
  */
object OptionParser extends LogSupport {
  def tokenize(line: String): Array[String] =
    CommandLineTokenizer.tokenize(line)

  def apply(surface: Surface): OptionParser = {
    val schema = ClassOptionSchema(surface)
    assert(schema != null)
    new OptionParser(schema)
  }

  private[launcher] def splitPrefixes(prefix: String): Seq[String] = {
    for (p <- prefix.split(",").toSeq) yield {
      if (p.startsWith("--") || p.startsWith("-")) {
        p
      } else {
        throw new IllegalArgumentException(s"Invalid prefix ${prefix} (not beginning with - or --)")
      }
    }
  }

  /**
    * Option -> value mapping result
    */
  sealed abstract class OptionMapping {
    def path: (Path, StringTree)
  }
  case class OptSetFlag(opt: CLOption) extends OptionMapping {
    def path = opt.path -> Leaf("true")
  }
  case class OptMapping(opt: CLOption, value: String) extends OptionMapping {
    def path = opt.path -> Leaf(value)
  }
  case class OptMappingMultiple(opt: CLOption, value: Seq[String]) extends OptionMapping {
    def path = opt.path -> SeqLeaf(value.map(Leaf(_)))
  }
  case class ArgMapping(opt: CLArgItem, value: String) extends OptionMapping {
    def path = opt.path -> Leaf(value)
  }
  case class ArgMappingMultiple(opt: CLArgument, value: Seq[String]) extends OptionMapping {
    def path = opt.path -> SeqLeaf(value.map(Leaf(_)))
  }

  /**
    * command-line option
    */
  sealed trait CLOptionItem {
    def path: Path
    def takesArgument: Boolean          = false
    def takesMultipleArguments: Boolean = false
  }

  abstract class CLOptionItemBase(val param: Parameter) extends CLOptionItem {
    override def takesMultipleArguments: Boolean = {
      import wvlet.airframe.surface.reflect.ReflectTypeUtil._
      val t: Class[_] = param.surface.rawType
      isArrayCls(t) || isSeq(t)
    }
  }

  trait CLArgItem extends CLOptionItem {
    def path: Path
    def name: String
    def argIndex: Int
  }

  /**
    * CommandTrait line option and the associated class parameter
    *
    * @param annot
    * @param param
    */
  case class CLOption(path: Path, annot: option, override val param: Parameter) extends CLOptionItemBase(param) {
    // validate prefixes
    val prefixes: Seq[String] = splitPrefixes(annot.prefix())
    override def takesArgument: Boolean = {
      val s = param.surface
      val typeSurface = if (s.isOption) {
        s.typeArgs(0)
      } else {
        s
      }
      // Boolean argument (e.g., --flag) will be set without an argument (e.g., --flat=true is unnecessary)
      typeSurface != Primitive.Boolean
    }
  }

  /**
    * CommandTrait line argument type and the associated class parameter
    *
    * @param arg
    * @param param
    */
  case class CLArgument(path: Path, arg: argument, argIndex: Int, override val param: Parameter)
      extends CLOptionItemBase(param)
      with CLArgItem {
    def name: String =
      if (arg.name.isEmpty) {
        param.name
      } else {
        arg.name
      }
  }

  case class OptionParserResult(parseTree: StringTree, unusedArgument: Array[String], val showHelp: Boolean)
      extends LogSupport {
    override def toString: String = {
      s"OptionParserResult(${parseTree}, unused:[${unusedArgument.mkString(",")}], showHelp:${showHelp})"
    }

    def buildObject(surface: Surface): Any = {
      val b = ObjectBuilder(surface)
      build(b).build
    }

    def buildObjectWithFilter[A](surface: Surface, filter: String => Boolean): Any = {
      val b = ObjectBuilder(surface)
      trace(s"build from parse tree: ${parseTree}")
      for ((path, value) <- parseTree.dfs if filter(path.last)) {
        b.set(path, value)
      }
      b.build.asInstanceOf[A]
    }

    def build[B <: GenericBuilder](builder: B): B = {
      trace(s"build from parse tree: ${parseTree}")
      for ((path, value) <- parseTree.dfs) {
        builder.set(path, value)
      }
      builder
    }
  }
}

/**
  * CommandTrait-line argument parser
  *
  * @author
  *   leo
  */
class OptionParser(val schema: OptionSchema) extends LogSupport {
  def this(m: MethodSurface) = this(MethodOptionSchema(m))

  import OptionParser._

  /**
    * Parse the command-line arguments
    *
    * @param args
    * @return
    *   parse result
    */
  def parse(args: Array[String]): OptionParserResult = {
    def findMatch[T](p: Regex, s: String): Option[Match] = p.findFirstMatchIn(s)

    def group(m: Match, group: Int): Option[String] = {
      if (m.start(group) != -1) Some(m.group(group)) else None
    }

    case class Flag(opt: CLOption, remaining: List[String])
    case class WithArg(opt: CLOption, v: String, remaining: List[String])

    // case object for pattern matching of options
    object OptionFlag {
      private val pattern = """^(-{1,2}\w+)""".r

      def unapply(s: List[String]): Option[Flag] = {
        findMatch(pattern, s.head) flatMap { m =>
          val symbol = m.group(1)
          schema.findFlagOption(symbol) map { Flag(_, s.tail) }
        }
      }
    }

    // case object for pattern matching of options that take arguments
    object OptionWithArgument {
      private val pattern = """^(-{1,2}\w+)([:=](\w+))?""".r

      def unapply(s: List[String]): Option[WithArg] = {
        findMatch(pattern, s.head) flatMap { m =>
          val symbol       = m.group(1)
          val immediateArg = group(m, 3)
          schema.findOptionNeedsArg(symbol) map { opt =>
            if (immediateArg.isEmpty) {
              if (s.tail.isEmpty) {
                throw new IllegalArgumentException("Option %s needs an argument" format opt)
              } else {
                val remaining = s.tail
                WithArg(opt, remaining.head, remaining.tail)
              }
            } else {
              WithArg(opt, immediateArg.get, s.tail)
            }
          }
        }
      }
    }

    // Hold mapping, option -> args ...
    val optionValues =
      collection.mutable.Map[CLOptionItem, ArrayBuffer[String]]()
    val unusedArguments = new ArrayBuffer[String]

    val logger = Logger("traverse")

    def traverseArg(l: List[String]): Unit = {
      var argIndex = 0

      logger.trace(f"index:${argIndex}%d, remaining:$l%s")

      def appendOptionValue(ci: CLOptionItem, value: String): Unit = {
        val holder = optionValues.getOrElseUpdate(ci, new ArrayBuffer[String]())
        holder += value
      }

      // Process command line arguments
      var continue  = true
      var remaining = l
      while (continue && !remaining.isEmpty) {
        val next = remaining match {
          case OptionFlag(m) => {
            appendOptionValue(m.opt, "true")
            m.remaining
          }
          case OptionWithArgument(m) => {
            appendOptionValue(m.opt, m.v)
            m.remaining
          }
          case e :: rest => {
            schema.findArgumentItem(argIndex) match {
              case Some(ai) => {
                appendOptionValue(ai, e)
                if (!ai.takesMultipleArguments) {
                  argIndex += 1
                }
              }
              case None =>
                unusedArguments += e
            }
            rest
          }
          case Nil => List() // end of arguments
        }
        remaining = next
      }
    }

    traverseArg(args.toList)

    val mapping: Seq[OptionMapping] = {
      val m = optionValues.collect {
        case (c: CLOption, values) =>
          if (c.takesArgument) {
            if (c.takesMultipleArguments) {
              OptMappingMultiple(c, values.toSeq)
            } else {
              OptMapping(c, values(0))
            }
          } else {
            OptSetFlag(c)
          }
        case (a: CLArgument, values) =>
          if (a.takesMultipleArguments) {
            ArgMappingMultiple(a, values.toSeq)
          } else {
            ArgMapping(a, values(0))
          }
      }
      m.toSeq
    }

    val holder = StringTree(for (m <- mapping) yield m.path)
    trace(s"parse tree: $holder")
    val showHelp = mapping.collectFirst {
      case c @ OptSetFlag(o) if o.annot.isHelp => c
    }.isDefined
    new OptionParserResult(holder, unusedArguments.toArray, showHelp)
  }

  def optionList: Seq[CLOption] = {
    schema.options
  }
}
