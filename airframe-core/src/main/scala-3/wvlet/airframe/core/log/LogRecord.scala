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

case class LogRecord(level: LogLevel, source: Option[LogSource], message: String, cause: Option[Throwable]):
  private val timestamp = System.currentTimeMillis()
  def getMillis: Long = timestamp
  def leafLoggerName: String =
    val name = getLoggerName()
    leafLoggerNameCache.getOrElseUpdate(
      name, {
        name match {
          case null => ""
          case name =>
            val pos = name.lastIndexOf('.')
            if (pos == -1) {
              name
            }
            else {
              name.substring(pos + 1)
            }
        }
      }
    )

