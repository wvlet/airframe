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

import wvlet.log.LogFormatter._
import wvlet.airframe.spec.AirSpec

/**
  *
  */
class WvletLog extends AirSpec with LogSupport {
  scalaJsSupport

  protected def log(formatter: LogFormatter): Unit = {
    LogEnv.defaultConsoleOutput.println(s"[${formatter.getClass.getSimpleName.replaceAll("\\$", "")}]:")
    logger.setFormatter(formatter)
    logger.info("info log")
    logger.debug("debug log")
    logger.trace("trace log")
    logger.warn("warn log")
    logger.error("error log", new Throwable("exception test"))
    LogEnv.defaultConsoleOutput.println
  }

  def `show logging examples` {
    logger.setFormatter(SourceCodeLogFormatter)
    logger.setLogLevel(LogLevel.ALL)

    info("Hello wvlet-log!")
    debug("wvlet-log adds fancy logging to your Scala applications.")
    trace("You can see the source code location here ==>")
    error("That makes easy to track your application behavior")
    logger.setFormatter(IntelliJLogFormatter)
    warn("And also, customizing log format is easy")
    info("This is the log format suited to IntelliJ IDEA")
    debug("This format adds links to the source code ->")
    logger.setFormatter(SourceCodeLogFormatter)
    info("wvlet-log uses Scala macro to output log messages only when necessary")
    error("And also it can show the stack trace", new Exception("Test message"))
    info("Usage is simple")
    warn("Just add wvlet.log.LogSupport trait to your application")
  }

  def `show log format examples` {
    val name = Thread.currentThread().getName
    Thread.currentThread().setName("thread-1")
    try {
      LogEnv.defaultConsoleOutput.println
      log(SourceCodeLogFormatter)
      log(SimpleLogFormatter)
      log(AppLogFormatter)
      log(IntelliJLogFormatter)
      log(TSVLogFormatter)
      log(BareFormatter)
    } finally {
      logger.resetLogLevel
      logger.setFormatter(SourceCodeLogFormatter)
      Thread.currentThread().setName(name)
    }
  }
}
