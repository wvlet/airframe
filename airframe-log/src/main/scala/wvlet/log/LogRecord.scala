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
package wvlet.log

import java.util.{logging => jl}

/**
  * Source code location where the log is
  *
  * @param fileName
  * @param line
  * @param col
  */
case class LogSource(private val filePath: String, fileName: String, line: Int, col: Int) {
  def fileLoc = s"${fileName}:${line}"
}

object LogRecord {
  def apply(record: jl.LogRecord): LogRecord = {
    val l = LogRecord(LogLevel(record.getLevel), None, record.getMessage, Option(record.getThrown))
    l.setLoggerName(record.getLoggerName)
    l
  }

  def apply(level: LogLevel, source: LogSource, message: String): LogRecord = {
    LogRecord(level, Some(source), message, None)
  }

  def apply(level: LogLevel, source: LogSource, message: String, cause: Throwable): LogRecord = {
    LogRecord(level, Some(source), message, Some(cause))
  }

  private[log] val leafLoggerNameCache = collection.mutable.Map[String, String]()
}

import wvlet.log.LogRecord._

case class LogRecord(level: LogLevel, source: Option[LogSource], message: String, cause: Option[Throwable])
    extends jl.LogRecord(level.jlLevel, message) {
  cause.foreach(setThrown(_))

  def leafLoggerName: String = {
    val name = getLoggerName
    leafLoggerNameCache.getOrElseUpdate(
      name, {
        name match {
          case null => ""
          case name =>
            val pos = name.lastIndexOf('.')
            if (pos == -1) {
              name
            } else {
              name.substring(pos + 1)
            }
        }
      }
    )
  }
}
