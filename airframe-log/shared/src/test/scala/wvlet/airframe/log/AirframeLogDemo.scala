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

import java.util.logging

import wvlet.airspec.AirSpec
import wvlet.log.LogFormatter._
import wvlet.log.{LocalLogSupport, LogFormatter, LogLevel, LogSupport, NullHandler}

/**
  */
class AirframeLogDemo extends AirSpec {
  scalaJsSupport

  class NoOutputHandler(formatter: LogFormatter) extends java.util.logging.Handler {
    override def publish(record: logging.LogRecord): Unit = {
      val msg = formatter.format(record)
    }
    override def flush(): Unit = {}
    override def close(): Unit = {}
  }

  protected def log(formatter: LogFormatter): Unit = {
    logger.resetHandler(new NoOutputHandler(formatter))
    logger.info(s"[${formatter.getClass.getSimpleName.replaceAll("\\$", "")}]:")
    try {
      logger.info("info log")
      logger.debug("debug log")
      logger.trace("trace log")
      logger.warn("warn log")
      logger.error("error log", new Throwable("exception test"))
    } finally {
      logger.clearHandlers
    }
  }

  def `show logging examples`: Unit = {
    logger.resetHandler(new NoOutputHandler(SourceCodeLogFormatter))
    try {
      logger.setLogLevel(LogLevel.ALL)

      info("Hello wvlet-log!")
      debug("airframe-log adds fancy logging to your Scala applications.")
      trace("You can see the source code location here ==>")
      error("That makes easy to track your application behavior")
      logger.resetHandler(new NoOutputHandler(IntelliJLogFormatter))
      warn("And also, customizing log format is easy")
      info("This is the log format suited to IntelliJ IDEA")
      debug("This format adds links to the source code ->")
      logger.resetHandler(new NoOutputHandler(SourceCodeLogFormatter))
      info("airframe-log uses Scala macro to output log messages only when necessary")
      error("It can also show the stack trace", new Exception("Test message"))
      info("Usage is simple")
      warn("Just add wvlet.log.LogSupport trait to your application")
    } finally {
      logger.clearHandlers
    }
  }

  def `show log format examples`: Unit = {
    val name = Thread.currentThread().getName
    Thread.currentThread().setName("thread-1")
    try {
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

  def `ANSI color paleter`: Unit = {
    // Just for improving the test coverage
    object MyColor extends AnsiColorPalette {}
  }
}
