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
package wvlet.airframe.log

import java.io.PrintStream
import wvlet.airframe.log.LogFormatter.SourceCodeLogFormatter

private[log] object LogEnv extends LogEnvBase:

  override def initLogManager(): Unit = {
    // do nothing by default
  }

  override def isScalaJS: Boolean                        = false
  override def defaultLogLevel: LogLevel                 = LogLevel.INFO
  override def defaultHandler: java.util.logging.Handler = new ConsoleLogHandler(SourceCodeLogFormatter)
  override def defaultConsoleOutput: PrintStream         = System.err

  /**
    * @param cl
    * @return
    */
  override def getLoggerName(cl: Class[?]): String =
    var name = cl.getName

    val pos = name.indexOf("$")
    if pos > 0 then
      // Remove trailing $xxx
      name = name.substring(0, pos)
    name
  override def scheduleLogLevelScan: Unit      = {}
  override def stopScheduledLogLevelScan: Unit = {}

  /**
    * Scan the default log level file only once. To periodically scan, use scheduleLogLevelScan
    */
  override def scanLogLevels: Unit = {}

  /**
    * Scan the specified log level file
    *
    * @param loglevelFileCandidates
    */
  override def scanLogLevels(loglevelFileCandidates: Seq[String]): Unit = {}

  override def registerJMX: Unit = {}

  /**
    */
  override def unregisterJMX: Unit = {}
