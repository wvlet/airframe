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
package wvlet.airframe.http.internal

import wvlet.airframe.http.{HttpLogger, HttpLoggerConfig}
import wvlet.log.{AsyncHandler, LogFormatter, LogRecord, LogRotationHandler}

import java.util.concurrent.atomic.AtomicBoolean

/**
  * Write access logs to a file with automatic log file rotation.
  *
  * @param httpAccessLogConfig
  */
class LogRotationHttpLogger(val config: HttpLoggerConfig) extends HttpLogger {
  private val closed = new AtomicBoolean(false)

  object MessageOnlyLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      // Do not output exception stack trace because it's already in the log entry Map[String, Any]
      val m = r.getMessage
      m
    }
  }

  // Use an async handler to perform logging in a background thread
  private val asyncLogHandler = new AsyncHandler(
    new LogRotationHandler(
      fileName = config.logFileName,
      maxNumberOfFiles = config.maxNumFiles,
      maxSizeInBytes = config.maxFileSize,
      formatter = MessageOnlyLogFormatter,
      logFileExt = config.logFileExtension
    )
  )

  override protected def writeInternal(log: Map[String, Any]): Unit = {
    val msg = config.logFormatter(log)
    if (!closed.get()) {
      asyncLogHandler.publish(new java.util.logging.LogRecord(java.util.logging.Level.INFO, msg))
    }
  }

  override def close(): Unit = {
    if (closed.compareAndSet(false, true)) {
      asyncLogHandler.close()
    }
  }
}
