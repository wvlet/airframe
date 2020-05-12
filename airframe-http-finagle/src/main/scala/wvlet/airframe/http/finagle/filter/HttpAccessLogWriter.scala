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
package wvlet.airframe.http.finagle.filter
import java.util.logging.Level

import wvlet.airframe.codec.MessageCodec
import wvlet.log.{AsyncHandler, LogFormatter, LogRecord, LogRotationHandler}

case class HttpAccessLogConfig(
    fileName: String = "log/http_access.log",
    maxFiles: Int = 100,
    maxSize: Long = 100 * 1024 * 1024
)

/**
  *
  */
trait HttpAccessLogWriter {
  def write(log: Map[String, Any]): Unit
}

object HttpAccessLogWriter {

  def default = new JSONHttpAccessLogWriter()

  /**
    * Write access logs to a file using a JSON format. This writer supports automatic log file rotation.
    * @param httpAccessLogConfig
    */
  class JSONHttpAccessLogWriter(httpAccessLogConfig: HttpAccessLogConfig = HttpAccessLogConfig())
      extends HttpAccessLogWriter {

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
        fileName = httpAccessLogConfig.fileName,
        maxNumberOfFiles = httpAccessLogConfig.maxFiles,
        maxSizeInBytes = httpAccessLogConfig.maxSize,
        JSONLogFormatter
      )
    )

    override def write(log: Map[String, Any]): Unit = {
      // Generate one-liner JSON log
      val json = mapCodec.toJson(log)
      asyncLogHandler.publish(new java.util.logging.LogRecord(Level.INFO, json))
    }
  }
}
