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
package wvlet.airframe.http

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpLogger.{ConsoleHttpLogger, InMemoryHttpLogger}
import wvlet.log.LogSupport

/**
  * Interface for writing HTTP request/response logs
  */
trait HttpLogger extends AutoCloseable {
  def config: HttpLoggerConfig

  def write(log: Map[String, Any]): Unit
}

/**
  * Http logger configuration
  */
case class HttpLoggerConfig(
    // The log file name. The default is log/http_access.json
    logFileName: String = "log/http_access.json",
    /**
      * Case-insensitive list of HTTP headers that need to be excluded from the logs. For example, Authorization,
      * ProxyAuthorization, Cookie headers will be removed by default
      */
    excludeHeaders: Set[String] = HttpLogger.defaultExcludeHeaders,
    // A filter for customizing the log contents
    logFilter: Map[String, Any] => Map[String, Any] = identity,
    // A formatter for converting log entries Map[String, Any] into a string line. The default behavior is producing JSON lines
    logFormatter: Map[String, Any] => String = HttpLogger.jsonFormatter,
    // The max number of log files to preserve in the local disk
    maxNumFiles: Int = 100,
    // The max file size for log rotation. The default is 100MB
    maxFileSize: Long = 100 * 1024 * 1024
) {
  def logFileExtension: String = {
    logFileName.lastIndexOf(".") match {
      case -1  => ""
      case pos => logFileName.substring(pos)
    }
  }

  def withLogFileName(fileName: String): HttpLoggerConfig = this.copy(logFileName = fileName)

  /**
    * Add request/response headers to exclude from logging. Case-insensitive match will be used.
    */
  def addExcludeHeaders(excludeHeaders: Set[String]): HttpLoggerConfig =
    this.copy(excludeHeaders = this.excludeHeaders ++ excludeHeaders)

  /**
    * Set a log filter for customizing log contents
    */
  def withLogFilter(newLogFilter: Map[String, Any] => Map[String, Any]): HttpLoggerConfig = {
    this.copy(logFilter = logFilter.andThen(newLogFilter))
  }

  def withLogFormatter(formatter: Map[String, Any] => String): HttpLoggerConfig = this.copy(logFormatter = formatter)
  def withMaxNumFiles(maxNumFiles: Int): HttpLoggerConfig                       = this.copy(maxNumFiles = maxNumFiles)
  def withMaxFileSize(maxFileSize: Long): HttpLoggerConfig                      = this.copy(maxFileSize = maxFileSize)

  /**
    * A log writer that writes logs to an in-memory buffer. Use this only for testing purpose.
    */
  def inMemoryLogger: HttpLogger = new InMemoryHttpLogger(this)

  /**
    * A log writer that writes logs to the console with debug-level logs
    */
  def consoleLogger: HttpLogger = new ConsoleHttpLogger(this)

}

object HttpLogger extends LogSupport {
  def defaultExcludeHeaders: Set[String] = Set(
    HttpHeader.Authorization,
    HttpHeader.ProxyAuthorization,
    HttpHeader.Cookie
  )

  def emptyLogger(inputConfig: HttpLoggerConfig): HttpLogger = new HttpLogger {
    override def config: HttpLoggerConfig           = inputConfig
    override def write(log: Map[String, Any]): Unit = {}
    override def close(): Unit                      = {}
  }

  /**
    * In-memory log writer for testing purpose. Not for production use.
    */
  class InMemoryHttpLogger(val config: HttpLoggerConfig) extends HttpLogger {
    private val logs = Seq.newBuilder[Map[String, Any]]

    def getLogs: Seq[Map[String, Any]] = logs.result()

    def clear(): Unit = {
      logs.clear()
    }

    override def write(log: Map[String, Any]): Unit = {
      synchronized {
        logs += log
      }
    }

    override def close(): Unit = {
      // no-op
    }
  }

  private val mapCodec = MessageCodec.of[Map[String, Any]]
  def jsonFormatter: Map[String, Any] => String = { (log: Map[String, Any]) =>
    mapCodec.toJson(log)
  }

  class ConsoleHttpLogger(val config: HttpLoggerConfig) extends HttpLogger {
    override def write(log: Map[String, Any]): Unit = {
      val msg = config.logFormatter(log)
      logger.debug(msg)
    }

    override def close(): Unit = {}
  }
}
