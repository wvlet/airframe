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
import wvlet.airframe.spec.runner.AirSpecTask.AirSpecEvent
import wvlet.airframe.spec.spi.AirSpecException
import wvlet.log.{LogFormatter, LogRecord, Logger}

/**
  *
  */
private[spec] class AirSpecLogger(sbtLoggers: Array[sbt.testing.Logger]) extends AnsiColorPalette {
  private val useAnciColor = sbtLoggers.forall(_.ansiCodesSupported())

  private val airSpecLogger = Logger("wvlet.airframe.spec.AirSpec")
  airSpecLogger.setFormatter(LogFormatter.BareFormatter)

  def withColor(colorEsc: String, s: String) = {
    if (useAnciColor)
      s"${colorEsc}${s}${RESET}"
    else
      s
  }

  private def info(m: String): Unit = {
    airSpecLogger.info(m)
  }

  private def warn(m: String): Unit = {
    val msg = withColor(YELLOW, m)
    airSpecLogger.warn(msg)
  }

  private def error(m: String): Unit = {
    val msg = withColor(BRIGHT_RED, m)
    airSpecLogger.error(msg)
  }

  def logSpecName(specName: String): Unit = {
    info(s"${withColor(BRIGHT_GREEN, specName)}${withColor(GRAY, ":")}")
  }

  def logEvent(e: AirSpecEvent): Unit = {
    val (baseColor, showStackTraces) = e.status match {
      case Status.Success                  => (GREEN, false)
      case Status.Failure                  => (RED, false) // Do not show the stack trace for assertion failures
      case Status.Error                    => (RED, true)
      case Status.Skipped                  => (BRIGHT_GREEN, false)
      case Status.Canceled                 => (YELLOW, false)
      case Status.Pending | Status.Ignored => (BRIGHT_YELLOW, false)
    }

    def elapsedTime: String = {
      withColor(GRAY, ElapsedTime.succinctNanos(e.durationNanos).toString)
    }

    def statusLabel(statusLabel: String): String = {
      s"<< ${withColor(WHITE, statusLabel)}"
    }

    def errorLocation(e: AirSpecException): String = {
      withColor(BLUE, s"(${e.code})")
    }

    val prefix = {
      s"${withColor(GRAY, " -")} ${withColor(baseColor, e.fullyQualifiedName)} ${elapsedTime}"
    }
    val tail = e.status match {
      case Status.Success => ""
      case _ if e.throwable.isDefined =>
        val ex = e.throwable.get()
        ex match {
          case se: AirSpecException =>
            s" ${statusLabel(se.statusLabel)}: ${withColor(baseColor, se.message)} ${errorLocation(se)}"
          case _ =>
            s" ${statusLabel("error")}: ${withColor(baseColor, ex.getMessage())}"
        }
      case _ =>
        ""
    }
    info(s"${prefix}${tail}")

    if (showStackTraces) {
      val ex         = wvlet.airframe.spec.compat.findCause(e.throwable.get())
      val stackTrace = LogFormatter.formatStacktrace(ex)
      error(stackTrace)
    }
  }
}
