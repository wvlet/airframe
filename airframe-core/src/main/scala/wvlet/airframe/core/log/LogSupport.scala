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
package wvlet.airframe.core.log

trait LogSupport {
  protected[this] lazy val logger: Logger = Logger(Logger.getLoggerNameOf(this.getClass))

  inline protected def error(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.ERROR) then logger.log(LogLevel.ERROR, LoggerMacros.sourcePos(), message)

  inline protected def warn(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.WARN) then logger.log(LogLevel.WARN, LoggerMacros.sourcePos(), message)

  inline protected def info(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.INFO) then logger.log(LogLevel.INFO, LoggerMacros.sourcePos(), message)

  inline protected def debug(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.DEBUG) then logger.log(LogLevel.DEBUG, LoggerMacros.sourcePos(), message)

  inline protected def trace(inline message: Any): Unit =
    if logger.isEnabled(LogLevel.TRACE) then logger.log(LogLevel.TRACE, LoggerMacros.sourcePos(), message)

  inline protected def logAt(inline logLevel: LogLevel, inline message: Any): Unit =
    if logger.isEnabled(logLevel) then logger.log(logLevel, LoggerMacros.sourcePos(), message)

  inline protected def error(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.ERROR) then
      logger.logWithCause(LogLevel.ERROR, LoggerMacros.sourcePos(), message, cause)

  inline protected def warn(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.WARN) then logger.logWithCause(LogLevel.WARN, LoggerMacros.sourcePos(), message, cause)
  
  inline protected def info(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.INFO) then logger.logWithCause(LogLevel.INFO, LoggerMacros.sourcePos(), message, cause)

  inline protected def debug(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.DEBUG) then
      logger.logWithCause(LogLevel.DEBUG, LoggerMacros.sourcePos(), message, cause)

  inline protected def trace(inline message: Any, inline cause: Throwable): Unit =
    if logger.isEnabled(LogLevel.TRACE) then
      logger.logWithCause(LogLevel.TRACE, LoggerMacros.sourcePos(), message, cause)

}
