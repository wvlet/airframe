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
package wvlet.airspec

import sbt.testing.{Fingerprint, TaskDef}
import wvlet.airspec.Framework.{AirSpecClassFingerPrint, AirSpecObjectFingerPrint}
import wvlet.airspec.runner.{AirSpecEventHandler, AirSpecLogger, AirSpecTaskRunner}
import wvlet.log.LogSupport
import wvlet.airspec.runner.AirSpecSbtRunner.AirSpecConfig

import scala.util.Try

/**
  */
object AirSpecLauncher extends LogSupport {
  def main(args: Array[String]): Unit = {
    args.length match {
      case 0 =>
        info(s"airspec: [command]")
      case _ =>
        val cmd = args(0)
        cmd match {
          case "-h" | "help" | "--help" =>
            printHelp
          case "test" =>
            run(args.tail)
          case _ =>
            warn(s"Unknown command: ${cmd}")
        }
    }
  }

  private def printHelp: Unit = {
    println(s"""[commands]
               |test       run AirSpec tests
               |help       show this message""".stripMargin)
  }

  private def run(args: Array[String]): Unit = {
    args.length match {
      case 0 =>
        warn(s"No test is specified")
      case _ =>
        val testClassFullName = args(0)
        info(s"Run tests in ${testClassFullName}")
        val cl = compat.getContextClassLoader

        val fingerprint = compat.getFingerprint(testClassFullName, cl).getOrElse {
          val msg =
            s"Class ${testClassFullName} extending AirSpec is not found"
          warn(msg)
          throw new IllegalArgumentException(msg)
        }

        val taskDef = new TaskDef(
          testClassFullName,
          fingerprint,
          true,
          Array.empty
        )
        val config = AirSpecConfig(args.tail)
        val runner = new AirSpecTaskRunner(
          taskDef,
          config,
          new AirSpecLogger(),
          new AirSpecEventHandler(),
          cl
        )
        runner.runTask
    }

  }

}
