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
import wvlet.log.LogSupport

case class ArgMap(map: Map[String, Any], unusedArgs: Seq[String])

case class LauncherResult(executedModule: Any, result: Option[Any])

/**
  * Process command-line arguments and execute a target command (or method)
  */
class ArgProcessor(context: Launcher) extends LogSupport {

  def process(args: Seq[String]): LauncherResult = {
    info(args.mkString(", "))
    val schema = ClassOptionSchema(context.surface)
    val parser = new OptionParser(schema)
    val result = parser.parse(args.toArray)
    info(result)

    val obj = result.buildObject(context.surface)
    info(s"build object: ${obj}")

    val showHelpMessage = result.showHelp

    if (result.unusedArgument.isEmpty) {
      if (!showHelpMessage) {
        // Run the default command
        context.findDefaultCommand
          .map { defaultCommand =>
            defaultCommand.call(obj)
          }
          .map { x =>
            LauncherResult(obj, Some(x))
          }
          .getOrElse {
            LauncherResult(obj, None)
          }
      } else {
        // Show the help message
        context.printHelp
        LauncherResult(obj, None)
      }

    } else {
      // The first argument should be sub command name
      val subCommandName = result.unusedArgument.head
      context.findSubCommand(subCommandName) match {
        case Some(subCommand) =>
          new ArgProcessor(subCommand).process(result.unusedArgument.tail)
        case None =>
          throw new IllegalArgumentException(s"Unknown sub command: ${subCommandName}")
      }

    }
  }

}
