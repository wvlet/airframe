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

import java.io.{ByteArrayOutputStream, PrintStream}
import java.util.{logging => jul}

import wvlet.log.LogFormatter._
import wvlet.log.LogLevel.LogOrdering
import wvlet.log.LoggerTest.capture

object LoggerTest {
  def capture[U](body: => U): String = {
    val buf = new ByteArrayOutputStream()
    Console.withErr(buf) {
      body
    }
    buf.toString
  }
}

class MyAppClass extends LogSupport {
  capture {
    error("error message")
    warn("warn message")
    info("info message")
    debug("debug message")
    trace("trace message")

    info(null)
    info("""This is a multi-line
        |log message!""".stripMargin)
    info(Seq(1, 2, 3, 4))

    warn("stack trace test", new Exception("stack trace test"))
  }
}

trait Sample

object Sample extends LogSupport {
  self =>
  def loggerName: String = logger.getName
}

/**
  *
  */
class LoggerTest extends Spec {
  scalaJsSupport

  override protected def beforeAll: Unit = {
    Logger("wvlet.log").setLogLevel(LogLevel.TRACE)
  }

  override protected def afterAll: Unit = {
    Logger("wvlet.log").setLogLevel(LogLevel.INFO)
  }

  def `use leaf logger name`: Unit = {
    val l = Logger("leaf")
    assert(l.getName == "leaf")
    l.debug("leaf logger")
  }

  def `have access to root logger`: Unit = {
    val current = Logger.getDefaultLogLevel
    println(s"current log level: ${current}")
    Logger.resetDefaultLogLevel
    assert(Logger.rootLogger.getLogLevel == LogEnv.defaultLogLevel)
    Logger.setDefaultLogLevel(current)
  }

  def `create logger from class`: Unit = {
    val l = Logger.of[MyAppClass]
    assert(l.getName == "wvlet.log.MyAppClass")
  }

  def `display log messages`: Unit = {
    debug("logging test")
    new MyAppClass
  }

  def `accept java.util.logging.LogRecord`: Unit = {
    SourceCodeLogFormatter.format(new jul.LogRecord(jul.Level.INFO, "format test"))
  }

  def `support simple log format`: Unit = {
    Logger.setDefaultFormatter(SimpleLogFormatter)
    new MyAppClass
  }

  def `support app log format`: Unit = {
    Logger.setDefaultFormatter(AppLogFormatter)
    new MyAppClass
  }

  def `support source code log format`: Unit = {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)
    new MyAppClass
  }

  def `support intellij format`: Unit = {
    Logger.setDefaultFormatter(IntelliJLogFormatter)
    new MyAppClass
  }

  def `support tsv format`: Unit = {
    Logger.setDefaultFormatter(TSVLogFormatter)
    new MyAppClass
  }

  def `can create local logger`: Unit = {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)
    capture {
      val l = Logger("org.sample")
      info(s"logger name: ${l.getName}")
      l.info("hello logger")
    }
  }

  def `be able to change log levels`: Unit = {
    capture {
      val l = Logger("org.sample")
      l.setLogLevel(LogLevel.TRACE)
      assert(l.getLogLevel == LogLevel.TRACE)
      l.resetLogLevel
      l.clear
    }
  }

  def `support logging methods`: Unit = {
    capture {
      val l = Logger("org.sample")
      l.trace("trace log test")
      l.debug("debug log test")
      l.info("info log test")
      l.warn("warn log test")
      l.error("error log test")
    }
  }

  def `display exception stack traces`: Unit = {
    capture {
      val e = new Exception("exception test")
      warn("Running stack trace tests")
      warn(e)
      warn(new Error("error test"))
      trace("error log test", e)
      debug("error log test", e)
      info("error log test", e)
      warn("error log test", e)
      error("error log test", e)

      val l = Logger("org.sample")
      l.warn(e)
      l.trace("error log test", e)
      l.debug("error log test", e)
      l.info("error log test", e)
      l.warn("error log test", e)
      l.error("error log test", e)
    }
  }

  def `support having a concrete logger`: Unit = {
    val t = new LocalLogSupport {
      info("hello")
    }
  }

  def `use succinct name when used with anonymous trait`: Unit = {
    if (LogEnv.isScalaJS) {
      pending
    } else {
      val l = new Sample with LogSupport {
        self =>
        assert(self.logger.getName == ("wvlet.log.Sample"))
      }
    }
  }

  def `Remove $ from object name`: Unit = {
    val o = Sample
    assert(o.loggerName == "wvlet.log.Sample")
  }

  def `clear parent handlers`: Unit = {
    try {
      val myHandler = new ConsoleLogHandler(LogFormatter.TSVLogFormatter)

      val l = Logger("wvlet.log.Sample")
      val p = Logger("wvlet")
      p.clearHandlers
      p.addHandler(myHandler)
      val r = Logger.rootLogger
      r.clearHandlers
      r.addHandler(myHandler)

      assert(p.getHandlers == Seq(myHandler))
      assert(r.getHandlers == Seq(myHandler))

      // Clean up handlers
      l.clearAllHandlers
      assert(l.getHandlers == Seq.empty)
      assert(p.getHandlers == Seq.empty)

      // Ignore this assertion for CI stability
      // assert(r.getHandlers == Seq.empty)
    } finally {
      Logger.clearAllHandlers
      Logger.setDefaultFormatter(SourceCodeLogFormatter)
    }
  }

  def `support java.util.LogLevel`: Unit = {
    for (
      l <- Seq(
        jul.Level.ALL,
        jul.Level.SEVERE,
        jul.Level.WARNING,
        jul.Level.FINE,
        jul.Level.CONFIG,
        jul.Level.FINER,
        jul.Level.FINEST,
        jul.Level.OFF
      )
    ) {
      LogLevel(l)
    }
  }

  def `parse string log levels`: Unit = {
    val logLevels = LogLevel.values.map(_.name.toLowerCase())

    // string to LogLevel
    for (l <- logLevels) {
      val level = LogLevel(l)
    }

    // Use INFO when unknown log level is given
    assert(LogLevel("unknown-loglevel") == LogLevel.INFO)

    // Test unapply
    val l1 = LogLevel.unapply("info")
    assert(l1.isDefined == true)
    val l2 = LogLevel.unapply("unknown-loglevel")
    assert(l2.isEmpty)
  }

  def `be able to sort LogLevels`: Unit = {
    val sorted = LogLevel.values.sorted(LogOrdering)
    sorted.sliding(2).forall(s => s(0) < s(1))
  }

  def `support flush and close`: Unit = {
    val h = new ConsoleLogHandler(SourceCodeLogFormatter)
    h.publish(LogRecord(new jul.LogRecord(jul.Level.FINE, "console log handler test")))
    h.flush()
    h.close()
  }
}
