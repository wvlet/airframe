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
package wvlet.airframe.spec.runner

import sbt.testing.Status
import wvlet.airframe.spec.compat
import wvlet.airframe.spec.runner.AirSpecTask.AirSpecEvent
import wvlet.airframe.spec.spi.AirSpecException
import wvlet.log.Logger

/**
  *
  */
class AirSpecLogger(sbtLoggers: Array[sbt.testing.Logger]) {

  private val taskLogger = Logger("wvlet.airframe.spec.runner.TaskLogger")
  taskLogger.setFormatter(AirSpecLogFormatter)

  def logSpecName(specName: String): Unit = {
    taskLogger.info(s"${specName}:")
  }

  def logEvent(e: AirSpecEvent): Unit = {
    e.status match {
      case Status.Success =>
        taskLogger.info(s" - ${e.fullyQualifiedName}")
      case other =>
        reportError(e.fullyQualifiedName, e.throwable.get(), e.status)
    }
  }

  private def formatError(baseColor: String, testName: String, status: Status, e: AirSpecException): String = {
    def withColor(colStr: String, s: String): String = {
      s"${Console.RESET}${colStr}${s}${Console.RESET}"
    }

    val statusLabel =
      s"${withColor(Console.WHITE, " - [")}${withColor(baseColor, e.statusLabel)}${withColor(Console.WHITE, "]")}"
    f"${statusLabel}%-51s ${testName} ${withColor(Console.BLUE, s"(${e.code})")}"
  }

  private def reportError(testName: String, e: Throwable, status: Status): Unit = {
    val cause = compat.findCause(e)
    cause match {
      case e: AirSpecException =>
        status match {
          case Status.Failure =>
            taskLogger.error(formatError(Console.RED, testName, status, e))
          case Status.Error =>
            taskLogger.error(formatError(Console.RED, testName, status, e), e)
          case _ =>
            taskLogger.warn(formatError(Console.YELLOW, testName, status, e))
        }
      case other =>
        taskLogger.error(s" - ${testName} -- Error ${other.getMessage}", e)
    }
  }

}
