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

import wvlet.log.LogFormatter.{appendStackTrace, highlightLog, withColor}
import wvlet.log.LogLevel.{DEBUG, ERROR, INFO, TRACE, WARN}
import wvlet.log.{LogFormatter, LogLevel, LogRecord}

/**
  *
  */
object AirSpecLogFormatter extends LogFormatter {

  def highlightLog(level: LogLevel, message: String): String = {
    val color = level match {
      case ERROR => Console.RED
      case WARN  => Console.YELLOW
      case INFO  => Console.GREEN
      case DEBUG => Console.WHITE
      case TRACE => Console.MAGENTA
      case _     => Console.RESET
    }
    withColor(color, message)
  }

  override def formatLog(r: LogRecord): String = {
    val log = s"${highlightLog(r.level, r.getMessage)}"
    appendStackTrace(log, r)
  }
}
