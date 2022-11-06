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

import wvlet.airframe.codec.MessageCodec
import wvlet.log.{AsyncHandler, LogFormatter, LogRecord, LogRotationHandler}

class JSONHttpLogWriter(config: HttpLoggerConfig) extends HttpLogWriter {
  private val mapCodec = MessageCodec.of[Map[String, Any]]

  object JSONLogFormatter extends LogFormatter {
    override def formatLog(r: LogRecord): String = {
      val m = r.getMessage
      m
    }
  }

  // Use an async handler to perform logging in a background thread
  private val asyncLogHandler = new AsyncHandler(
    new LogRotationHandler(
      fileName = config.fileName,
      maxNumberOfFiles = config.maxFiles,
      maxSizeInBytes = config.maxSize,
      formatter = JSONLogFormatter,
      logFileExt = ".json"
    )
  )

  override def write(log: Map[String, Any]): Unit = {
    // Generate one-liner JSON log
    // TODO: Handle too large log data (e.g., binary data)
    val json = mapCodec.toJson(log)
    asyncLogHandler.publish(new java.util.logging.LogRecord(java.util.logging.Level.INFO, json))
  }

  override def close(): Unit = {
    asyncLogHandler.close()
  }
}
