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

case class HttpLoggerConfig(
    // The log file name. The default is log/http-access.log
    logFileName: String = "log/http-access.log",
    /**
      * Case-insensitive list of HTTP headers that need to be excluded from the logs. For example, Authorization,
      * ProxyAuthorization, Cookie headers will be removed by default
      */
    excludeHeaders: Set[String] = HttpLogger.defaultExcludeHeaders,
    // A filter for customizing the log contents
    logFilter: Map[String, Any] => Map[String, Any] = identity,
    // The max number of log files to preserve in the local disk
    maxNumFiles: Int = 100,
    // The max file size for log rotation. The default is 100MB
    maxFileSize: Long = 100 * 1024 * 1024
) {
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

  def withMaxNumFiles(maxNumFiles: Int): HttpLoggerConfig  = this.copy(maxNumFiles = maxNumFiles)
  def withMaxFileSize(maxFileSize: Long): HttpLoggerConfig = this.copy(maxFileSize = maxFileSize)
}

object HttpLogger {
  def defaultExcludeHeaders: Set[String] = Set(
    HttpHeader.Authorization,
    HttpHeader.ProxyAuthorization,
    HttpHeader.Cookie
  )
}
