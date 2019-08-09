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
import wvlet.airframe.log.AnsiColorPalette
import wvlet.airframe.metrics.ElapsedTime
import wvlet.airframe.spec.compat
import wvlet.airframe.spec.runner.AirSpecTask.AirSpecEvent
import wvlet.airframe.spec.spi.AirSpecException

/**
  *
  */
private[spec] class AirSpecLogger(sbtLoggers: Array[sbt.testing.Logger]) extends AnsiColorPalette {

  private val useAnciColor = sbtLoggers.forall(_.ansiCodesSupported())

  def withColor(colorEsc: String, s: String) = {
    if (useAnciColor)
      s"${colorEsc}${s}${RESET}"
    else
      s
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
    val msg = withColor(YELLOW, e.getMessage)
    log(_.warn(msg))
  }

  private def error(e: Throwable): Unit = {
    val msg = withColor(RED, e.getMessage)
    log(_.error(msg))
  }

  def logSpecName(specName: String): Unit = {
    info(s"${withColor(BRIGHT_GREEN, specName)}${withColor(GRAY, ":")}")
  }

  def dash: String = {
    withColor(GRAY, " -")
  }

  def logEvent(e: AirSpecEvent): Unit = {
    e.status match {
      case Status.Success =>
        info(s"${dash} ${withColor(GREEN, e.fullyQualifiedName)} ${elapsedTime(e.durationNanos)}")
      case other =>
        reportError(e)
    }
  }

  private def elapsedTime(nanos: Long): String = {
    withColor(GRAY, ElapsedTime.succinctNanos(nanos).toString)
  }

  private def statusLabel(e: AirSpecException): String = {
    s"<< ${withColor(WHITE, e.statusLabel)}"
  }

  private def errorReportPrefix(baseColor: String, testName: String): String = {
    s"${dash} ${withColor(baseColor, testName)}"
  }

  private def errorLocation(e: AirSpecException): String = {
    withColor(BLUE, s"(${e.code})")
  }

  private def formatError(baseColor: String, e: AirSpecEvent, ex: AirSpecException): String = {
    s"${errorReportPrefix(baseColor, e.fullyQualifiedName)} ${statusLabel(ex)} ${errorLocation(ex)} ${elapsedTime(e.durationNanos)}"
  }

  private def reportError(event: AirSpecEvent): Unit = {
    val testName     = event.fullyQualifiedName
    val e            = event.throwable.get()
    val status       = event.status
    val elapsedNanos = event.durationNanos

    val cause = compat.findCause(e)
    cause match {
      case e: AirSpecException =>
        status match {
          case Status.Failure =>
            info(formatError(RED, event, e))
            error(e)
          case Status.Error =>
            info(formatError(RED, event, e))
            error(e)
          case Status.Pending | Status.Canceled | Status.Skipped =>
            info(
              s"${errorReportPrefix(YELLOW, testName)} ${statusLabel(e)}: ${e.message} ${errorLocation(e)} ${elapsedTime(elapsedNanos)}")
          case _ =>
            info(formatError(YELLOW, event, e))
            warn(e)
        }
      case other =>
        info(
          s"${errorReportPrefix(RED, testName)} << ${withColor(WHITE, "error")} ${other.getMessage} ${elapsedTime(elapsedNanos)}")
    }
  }
}
