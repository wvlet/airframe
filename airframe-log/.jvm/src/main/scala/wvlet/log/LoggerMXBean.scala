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

/**
  */
trait LoggerMXBean {
  def getLogLevel(loggerName: String): String
  def setLogLevel(loggerName: String, logLevel: String): Unit

  def getDefaultLogLevel(): String
  def setDefaultLogLevel(logLevel: String): Unit
}

object LoggerJMX extends LoggerMXBean {
  def getLogLevel(loggerName: String): String = {
    Logger(loggerName).getLogLevel.name
  }
  def setLogLevel(loggerName: String, logLevel: String): Unit = {
    val l = Logger(loggerName)
    l.setLogLevel(LogLevel(logLevel))
  }
  override def getDefaultLogLevel(): String = Logger("").getLogLevel.toString
  override def setDefaultLogLevel(logLevel: String): Unit = {
    val l = Logger("")
    l.setLogLevel(LogLevel(logLevel))
  }
}
