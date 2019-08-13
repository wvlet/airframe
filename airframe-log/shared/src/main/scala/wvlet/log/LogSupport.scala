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

import scala.language.experimental.macros

/**
  * Trait for adding logging methods (error, warn, info, debug and trace) to your class
  */
trait LogSupport extends LoggingMethods with LazyLogger

/**
  * Trait for adding logging methods and an initialized logger instance
  */
trait LocalLogSupport extends LoggingMethods with LocalLogger

/**
  * Trait for adding a local logger instance to your class
  */
trait LazyLogger {
  protected[this] lazy val logger: Logger = Logger(LogEnv.getLoggerName(this.getClass))
}

/**
  * Trait for adding an initialized logger instance to your class
  */
trait LocalLogger {
  protected[this] val logger: Logger = Logger(LogEnv.getLoggerName(this.getClass))
}

trait LoggingMethods extends Serializable {
  import LogMacros._

  protected def error(message: Any): Unit = macro errorLog
  protected def error(message: Any, cause: Throwable): Unit = macro errorLogWithCause

  protected def warn(message: Any): Unit = macro warnLog
  protected def warn(message: Any, cause: Throwable): Unit = macro warnLogWithCause

  protected def info(message: Any): Unit = macro infoLog
  protected def info(message: Any, cause: Throwable): Unit = macro infoLogWithCause

  protected def debug(message: Any): Unit = macro debugLog
  protected def debug(message: Any, cause: Throwable): Unit = macro debugLogWithCause

  protected def trace(message: Any): Unit = macro traceLog
  protected def trace(message: Any, cause: Throwable): Unit = macro traceLogWithCause

  protected def logAt(logLevel: LogLevel, message: Any): Unit = macro logAtImpl
}

trait PublicLoggingMethods extends Serializable { p =>
  import LogMacros._
  protected[this] def logger: Logger
}
