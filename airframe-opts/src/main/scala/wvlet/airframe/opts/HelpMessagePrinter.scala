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
import wvlet.airframe.opts.OptionParser.{CLArgItem, CLOption}
import wvlet.log.LogSupport

/**
  *
  */
class HelpMessagePrinter extends LogSupport {

  def render(commandName: String, argumentList: String, description: String, optionList: String): String = {
    s"""|usage: ${commandName} ${argumentList}
        |  ${description}
        |${optionList}""".stripMargin
  }

  protected def defaultUsage(args: Seq[CLArgItem]): String = {
    val l = for (a <- args) yield {
      a.name
    }
    l.map(x => s"[${x}").mkString(" ")
  }

  def printHelp(stack: List[Launcher[_]] = Nil): Unit = {
    trace("print usage")

    val l = stack.head

    // Show basic usage
    println(
      render(commandName = l.name,
             argumentList = l.usage,
             description = l.description,
             optionList = createOptionHelpMessage(l)))

    // Show parent options
    val parentOptions = stack.tail.flatMap { x =>
      x.optionList
    }
    if (parentOptions.nonEmpty) {
      println("[global options]")
      println(createOptionList(parentOptions).mkString("\n"))
    }

    if (l.subCommands.nonEmpty) {
      println("[commands]")

      val maxCommandNameLen = l.subCommands.map(_.name.length).max
      val format            = " %%-%ds\t%%s".format(math.max(10, maxCommandNameLen))
      // Show sub commend lists
      l.subCommands.foreach { c =>
        println(format.format(c.name, c.description))
      }
    }
  }

  def helpHeader(l: Launcher[_]): String = {
    ""
  }

  private def createOptionHelpMessage(l: Launcher[_]) = {
    val optionList = createOptionList(l.optionList)
    val b          = new StringBuilder
    if (optionList.nonEmpty) {
      b.append("[options]\n")
      b.append(optionList.mkString("\n") + "\n")
    }
    b.result
  }

  private def createOptionList(optionList: Seq[CLOption]): Seq[String] = {
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
