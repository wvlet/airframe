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


object Logger:
  def apply(fullName: String): Logger =
    val pos = fullName.lastIndexOf('.')
    if pos == -1 then
      new Logger(null, fullName)
    else
      new Logger(Logger(fullName.substring(0, pos)), fullName.substring(pos + 1))

  private[log] def getLoggerNameOf(cl: Class[_]): String = {
    var name = cl.getName

    if (name.endsWith("$")) {
      // Remove trailing $ of Scala Object name
      name = name.substring(0, name.length - 1)
    }

    // When class is an anonymous trait
    if (name.contains("$anon$")) {
      val interfaces = cl.getInterfaces
      if (interfaces != null && interfaces.length > 0) {
        // Use the first interface name instead of the anonymous name
        name = interfaces(0).getName
      }
    }
    name
  }

class Logger(parent: Logger | Null, val name: String) extends Serializable:
  private var _logLevel: LogLevel = LogLevel.INFO

  def logLevel: LogLevel = _logLevel
  def setLogLevel(logLevel: LogLevel): Unit =
    _logLevel = logLevel


  def isEnabled(logLevel: LogLevel): Boolean =
    logLevel.ordinal >= _logLevel.ordinal


  def log(logLevel: LogLevel, source: LogSource, message: Any): Unit = ???


  def logWithCause(logLevel: LogLevel, source: LogSource, message: Any, cause: Throwable): Unit = ???
