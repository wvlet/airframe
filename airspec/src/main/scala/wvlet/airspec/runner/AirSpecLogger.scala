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
package wvlet.airspec.runner

import java.util.concurrent.TimeUnit

import sbt.testing._
import wvlet.airframe.log.AnsiColorPalette
import wvlet.airframe.metrics.ElapsedTime
import wvlet.airspec.spi.AirSpecFailureBase
import wvlet.log.{LogFormatter, Logger}

private[airspec] case class AirSpecEvent(taskDef: TaskDef,
                                         override val fullyQualifiedName: String,
                                         override val status: Status,
                                         override val throwable: OptionalThrowable,
                                         durationNanos: Long)
    extends Event {
  override def fingerprint(): Fingerprint = taskDef.fingerprint()
  override def selector(): Selector       = taskDef.selectors().head
  override def duration(): Long           = TimeUnit.NANOSECONDS.toMillis(durationNanos)
}

/**
  *
  */
private[airspec] class AirSpecLogger() extends AnsiColorPalette {
  // Always use ANSI color log for Travis because sbt's ansiCodeSupported() returns false even though it can show ANSI colors
  private val useAnciColor = true

  private val airSpecLogger = {
    val l = Logger("wvlet.airspec.runner.AirSpecLogger")
    l.clearAllHandlers
    l.setFormatter(LogFormatter.BareFormatter)
    l
  }

  private[runner] def clearHandler = {
    airSpecLogger.clearHandlers
  }

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

  private def indent(level: Int): String = {
    "    " * level
  }

  def logSpecName(specName: String, indentLevel: Int): Unit = {
    info(s"${indent(indentLevel)}${withColor(BRIGHT_GREEN, specName)}${withColor(GRAY, ":")}")
  }

  def logEvent(e: AirSpecEvent, indentLevel: Int = 0): Unit = {
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

    def errorLocation(e: AirSpecFailureBase): String = {
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
          case se: AirSpecFailureBase =>
            s" ${statusLabel(se.statusLabel)}: ${withColor(baseColor, se.message)} ${errorLocation(se)}"
          case _ =>
            s" ${statusLabel("error")}: ${withColor(baseColor, ex.getMessage())}"
        }
      case _ =>
        ""
    }
    info(s"${indent(indentLevel)}${prefix}${tail}")

    if (showStackTraces) {
      val ex         = wvlet.airspec.compat.findCause(e.throwable.get())
      val stackTrace = LogFormatter.formatStacktrace(ex)
      error(stackTrace)
    }
  }
}
