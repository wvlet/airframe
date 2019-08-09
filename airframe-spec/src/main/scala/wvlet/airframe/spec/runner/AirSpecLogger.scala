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

/**
  *
  */
private[spec] class AirSpecLogger(sbtLoggers: Array[sbt.testing.Logger]) {

  def withColor(prefix: String, s: String) = {
    s"${prefix}${s}${Console.RESET}"
  }

  private def log(body: sbt.testing.Logger => Unit) {
    for (l <- sbtLoggers) {
      body(l)
    }
  }

  private def info(m: String): Unit = {
    log(_.info(m))
  }

  private def warn(e: Throwable): Unit = {
    val msg = withColor(Console.YELLOW, e.getMessage)
    log(_.warn(msg))
  }

  private def error(e: Throwable): Unit = {
    val msg = withColor(Console.RED, e.getMessage)
    log(_.error(msg))
  }

  def logSpecName(specName: String): Unit = {
    info(s"${withColor(Console.CYAN, specName)}:")
  }

  def logEvent(e: AirSpecEvent): Unit = {
    e.status match {
      case Status.Success =>
        info(s" ${withColor(Console.WHITE, "-")} ${withColor(Console.CYAN, e.fullyQualifiedName)}")
      case other =>
        reportError(e.fullyQualifiedName, e.throwable.get(), e.status)
    }
  }

  private def statusLabel(e: AirSpecException): String = {
    s"<< ${withColor(Console.WHITE, e.statusLabel)}"
  }

  private def errorReportPrefix(baseColor: String, testName: String): String = {
    s" ${withColor(Console.WHITE, "-")} ${withColor(baseColor, testName)}"
  }

  private def errorLocation(e: AirSpecException): String = {
    withColor(Console.BLUE, s"(${e.code})")
  }

  private def formatError(baseColor: String, testName: String, status: Status, e: AirSpecException): String = {
    s"${errorReportPrefix(baseColor, testName)} ${statusLabel(e)} ${errorLocation(e)}"
  }

  private def reportError(testName: String, e: Throwable, status: Status): Unit = {
    val cause = compat.findCause(e)
    cause match {
      case e: AirSpecException =>
        status match {
          case Status.Failure =>
            info(formatError(Console.RED, testName, status, e))
            error(e)
          case Status.Error =>
            info(formatError(Console.RED, testName, status, e))
            error(e)
          case Status.Pending | Status.Canceled | Status.Skipped =>
            info(s"${errorReportPrefix(Console.YELLOW, testName)} ${statusLabel(e)}: ${e.message} ${errorLocation(e)}")
          case _ =>
            info(formatError(Console.YELLOW, testName, status, e))
            warn(e)
        }
      case other =>
        info(s" ${errorReportPrefix(Console.RED, testName)} - ${withColor(Console.WHITE, "error")} ${other.getMessage}")
        error(other)
    }
  }
}
