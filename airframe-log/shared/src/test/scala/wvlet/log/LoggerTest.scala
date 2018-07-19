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

import org.scalatest.Tag
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

  override def beforeAll: Unit = {
    Logger.setDefaultLogLevel(LogLevel.TRACE)
  }

  override def afterAll: Unit = {
    Logger.setDefaultLogLevel(LogLevel.INFO)
  }

  "logger" should {
    "use leaf logger name" in {
      val l = Logger("leaf")
      l.getName shouldBe "leaf"
      l.info("leaf logger")
    }

    "have access to root logger" in {
      val current = Logger.getDefaultLogLevel
      println(s"current log level: ${current}")
      Logger.resetDefaultLogLevel
      Logger.rootLogger.getLogLevel shouldBe LogEnv.defaultLogLevel
      Logger.setDefaultLogLevel(current)
    }

    "create logger from class" taggedAs (Tag("app")) in {
      val l = Logger.of[MyAppClass]
      l.getName shouldBe "wvlet.log.MyAppClass"
    }

    "display log messages" taggedAs (Tag("app")) in {
      debug("logging test")
      new MyAppClass
    }

    "accept java.util.logging.LogRecord" in {
      SourceCodeLogFormatter.format(new jul.LogRecord(jul.Level.INFO, "format test"))
    }

    "support simple log format" in {
      Logger.setDefaultFormatter(SimpleLogFormatter)
      new MyAppClass
    }

    "support app log format" in {
      Logger.setDefaultFormatter(AppLogFormatter)
      new MyAppClass
    }

    "support source code log format" in {
      Logger.setDefaultFormatter(SourceCodeLogFormatter)
      new MyAppClass
    }

    "support intellij format" in {
      Logger.setDefaultFormatter(IntelliJLogFormatter)
      new MyAppClass
    }

    "support tsv format" in {
      Logger.setDefaultFormatter(TSVLogFormatter)
      new MyAppClass
    }

    "can create local logger" in {
      Logger.setDefaultFormatter(SourceCodeLogFormatter)
      capture {
        val l = Logger("org.sample")
        info(s"logger name: ${l.getName}")
        l.info("hello logger")
      }
    }

    "be able to change log levels" in {
      capture {
        val l = Logger("org.sample")
        l.setLogLevel(LogLevel.TRACE)
        l.getLogLevel shouldBe LogLevel.TRACE
        l.resetLogLevel
        l.clear
      }
    }

    "support logging methods" in {
      capture {
        val l = Logger("org.sample")
        l.trace("trace log test")
        l.debug("debug log test")
        l.info("info log test")
        l.warn("warn log test")
        l.error("error log test")
      }
    }

    "display exception stack traces" in {
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

    "support having a concrete logger" in {
      val t = new LocalLogSupport {
        info("hello")
      }
    }

    "use succinct name when used with anonymous trait" in {
      if (LogEnv.isScalaJS) {
        pending
      } else {
        val l = new Sample with LogSupport {
          self =>
          self.logger.getName shouldBe ("wvlet.log.Sample")
        }
      }
    }

    "Remove $ from object name" taggedAs (Tag("app")) in {
      val o = Sample
      o.loggerName shouldBe "wvlet.log.Sample"
    }

    "clear parent handlers" in {
      try {
        val myHandler = new ConsoleLogHandler(LogFormatter.TSVLogFormatter)

        val l = Logger("wvlet.log.Sample")
        val p = Logger("wvlet")
        p.clearHandlers
        p.addHandler(myHandler)
        val r = Logger.rootLogger
        r.clearHandlers
        r.addHandler(myHandler)

        p.getHandlers shouldBe Seq(myHandler)
        r.getHandlers shouldBe Seq(myHandler)

        // Clean up handlers
        l.clearAllHandlers
        l.getHandlers shouldBe Seq.empty
        p.getHandlers shouldBe Seq.empty
        r.getHandlers shouldBe Seq.empty

      } finally {
        Logger.clearAllHandlers
        Logger.setDefaultFormatter(SourceCodeLogFormatter)
      }
    }
  }

  "LogLevel" should {
    "support java.util.LogLevel" in {
      for (l <- Seq(jul.Level.ALL,
                    jul.Level.SEVERE,
                    jul.Level.WARNING,
                    jul.Level.FINE,
                    jul.Level.CONFIG,
                    jul.Level.FINER,
                    jul.Level.FINEST,
                    jul.Level.OFF)) {
        LogLevel(l)
      }
    }

    "parse string log levels" in {
      val logLevels = LogLevel.values.map(_.name.toLowerCase())

      // string to LogLevel
      for (l <- logLevels) {
        val level = LogLevel(l)
      }

      // Use INFO when unknown log level is given
      LogLevel("unknown-loglevel") shouldBe LogLevel.INFO

      // Test unapply
      val l1 = LogLevel.unapply("info")
      l1.isDefined shouldBe true
      val l2 = LogLevel.unapply("unknown-loglevel")
      l2.isDefined shouldNot be(true)
    }

    "be able to sort LogLevels" in {
      val sorted = LogLevel.values.sorted(LogOrdering)
      sorted.sliding(2).forall(s => s(0) < s(1))
    }
  }

  "ConsoleLogHandler" should {
    "support flush and close" in {
      val h = new ConsoleLogHandler(SourceCodeLogFormatter)
      h.publish(LogRecord(new jul.LogRecord(jul.Level.INFO, "console log handler test")))
      h.flush()
      h.close()
    }
  }
}
