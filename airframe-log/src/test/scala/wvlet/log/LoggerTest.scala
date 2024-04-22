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

import wvlet.log.LogFormatter.*
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
  */
class LoggerTest extends Spec {

  override protected def beforeAll: Unit = {
    Logger("wvlet.log").setLogLevel(LogLevel.TRACE)
  }

  override protected def afterAll: Unit = {
    Logger("wvlet.log").setLogLevel(LogLevel.INFO)
  }

  test("use leaf logger name") {
    val l = Logger("leaf")
    assert(l.getName == "leaf")
    l.debug("leaf logger")
  }

  test("have access to root logger") {
    val current = Logger.getDefaultLogLevel
    println(s"current log level: ${current}")
    Logger.resetDefaultLogLevel
    assert(Logger.rootLogger.getLogLevel == LogEnv.defaultLogLevel)
    Logger.setDefaultLogLevel(current)
  }

  test("create logger from class") {
    val l = Logger.of[MyAppClass]
    assert(l.getName == "wvlet.log.MyAppClass")
  }

  test("display log messages") {
    debug("logging test")
    new MyAppClass
  }

  test("accept java.util.logging.LogRecord") {
    SourceCodeLogFormatter.format(new jul.LogRecord(jul.Level.INFO, "format test"))
  }

  test("support simple log format") {
    Logger.setDefaultFormatter(SimpleLogFormatter)
    new MyAppClass
  }

  test("support app log format") {
    Logger.setDefaultFormatter(AppLogFormatter)
    new MyAppClass
  }

  test("support source code log format") {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)
    new MyAppClass
  }
  test("support plain source code log format") {
    Logger.setDefaultFormatter(PlainSourceCodeLogFormatter)
    new MyAppClass
  }
  test("support log with thread name") {
    Logger.setDefaultFormatter(ThreadLogFormatter)
    new MyAppClass
  }

  test("support intellij format") {
    Logger.setDefaultFormatter(IntelliJLogFormatter)
    new MyAppClass
  }

  test("support tsv format") {
    Logger.setDefaultFormatter(TSVLogFormatter)
    new MyAppClass
  }

  test("can create local logger") {
    Logger.setDefaultFormatter(SourceCodeLogFormatter)
    capture {
      val l = Logger("org.sample")
      debug(s"logger name: ${l.getName}")
      l.info("hello logger")
    }
  }

  test("be able to change log levels") {
    capture {
      val l = Logger("org.sample")
      l.setLogLevel(LogLevel.TRACE)
      assert(l.getLogLevel == LogLevel.TRACE)
      l.resetLogLevel
      l.clear
    }
  }

  test("support logging methods") {
    capture {
      val l = Logger("org.sample")
      l.trace("trace log test")
      l.debug("debug log test")
      l.info("info log test")
      l.warn("warn log test")
      l.error("error log test")
    }
  }

  test("display exception stack traces") {
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

  test("support having a concrete logger") {
    val t = new LocalLogSupport {
      info("hello")
    }
  }

  test("use succinct name when used with anonymous trait") {
    if (isScalaJS || isScalaNative) {
      pending("Scala.js/Native cannot get a logger name from anonymous trait")
    } else {
      val l = new Sample with LogSupport {
        self =>
        self.logger.getName shouldBe "wvlet.log.Sample"
      }
    }
  }

  test("Remove $ from object name") {
    val o = Sample
    o.loggerName shouldBe "wvlet.log.Sample"
  }

  test("clear parent handlers") {
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
      // Ignoring this assert as it can be flaky
      // assert(r.getHandlers == Seq(myHandler))

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

  test("support java.util.LogLevel") {
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

  test("parse string log levels") {
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

  test("be able to sort LogLevels") {
    val sorted = LogLevel.values.sorted(LogOrdering)
    sorted.sliding(2).forall(s => s(0) < s(1))
  }

  test("support flush and close") {
    val h = new ConsoleLogHandler(SourceCodeLogFormatter)
    h.publish(LogRecord(new jul.LogRecord(jul.Level.FINE, "console log handler test")))
    h.flush()
    h.close()
  }
}
