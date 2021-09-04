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
import java.io.{PrintWriter, StringWriter}

import wvlet.airframe.launcher.OptionParser.{CLArgItem, CLOption}
import wvlet.airframe.log.AnsiColorPalette
import wvlet.log.LogSupport

import scala.io.AnsiColor

/**
  * Interface for printing help messages
  */
trait HelpMessagePrinter {
  def render(
      commandName: String,
      arguments: Seq[CLArgItem],
      oneLineUsage: Option[String],
      description: String,
      options: Seq[CLOption],
      globalOptions: Seq[CLOption],
      subCommands: Seq[CommandLauncher]
  ): String
}

object HelpMessagePrinter extends LogSupport with AnsiColorPalette {
  private def withColor(color: String, msg: String): String = {
    s"${color}${msg}${AnsiColor.RESET}"
  }

  /**
    * Teh default help message printer in this format:
    * {{{
    *   usage: (command name) (command arguments)
    *       (description)
    *
    *   [global options]
    *   ...
    *   [options]
    *   ...
    *
    *   [commands]
    *   ...
    * }}}
    */
  val default = new HelpMessagePrinter {
    override def render(
        commandName: String,
        arguments: Seq[CLArgItem],
        oneLineUsage: Option[String],
        description: String,
        options: Seq[CLOption],
        globalOptions: Seq[CLOption],
        subCommands: Seq[CommandLauncher]
    ): String = {
      val str = new StringWriter()
      val s   = new PrintWriter(str)

      val hasAnyOption = globalOptions.nonEmpty || options.nonEmpty

      // Print one-line command usage
      s.print(s"${withColor(CYAN, "usage")}: ")
      s.println(oneLineUsage.getOrElse {
        val b = Seq.newBuilder[String]
        if (globalOptions.nonEmpty) {
          b += s"[global options]"
        }
        b += commandName
        if (options.nonEmpty) {
          b += s"[${withColor(CYAN, "options")}]"
        }
        if (arguments.nonEmpty) {
          b += arguments.map(x => s" [${x.name}]").mkString
        }
        if (subCommands.nonEmpty) {
          b += s"<${withColor(CYAN, "command name")}>"
        }
        b.result().mkString(" ")
      })
      // Print description
      if (description.nonEmpty) {
        s.println(s"  ${description}")
      }
      if (hasAnyOption) {
        s.println()
      }

      // Print options
      if (globalOptions.nonEmpty) {
        s.println(s"[${withColor(CYAN, "global options")}]")
        s.println(renderOptionList(globalOptions))
      }

      if (options.nonEmpty) {
        s.println(s"[${withColor(CYAN, "options")}]")
        s.println(renderOptionList(options))
      }

      if (subCommands.nonEmpty) {
        s.println("")
        s.println(s"[${withColor(CYAN, "commands")}]")
        s.println(renderCommandList(subCommands))
      }

      s.flush()
      str.toString
    }
  }

  def renderCommandList(commandList: Seq[CommandLauncher]): String = {
    val maxCommandNameLen = commandList.map(x => x.name.length).max
    val format            = s" %-${math.max(10, maxCommandNameLen)}s\t%s"
    // Show sub commend lists
    commandList
      .map { c => format.format(c.name, withColor(BRIGHT_CYAN, c.description)) }
      .mkString("\n")
  }

  def renderOptionList(optionList: Seq[CLOption]): String = {
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

    def genDescription(opt: CLOption): String = {
      withColor(BRIGHT_CYAN, opt.annot.description())
    }

    val s = for (x <- optDscr) yield {
      val paddingLen = optDscrLenMax - x._2.length
      val padding    = Array.fill(paddingLen)(" ").mkString
      " %s%s  %s".format(x._2, padding, genDescription(x._1))
    }
    s.mkString("\n")
  }
}
