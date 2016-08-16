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

import java.util.{logging => jul}

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, _}
import wvlet.log.LogFormatter._
import wvlet.log.LogLevel.LogOrdering

trait Spec extends WordSpec with ShouldMatchers with BeforeAndAfter with BeforeAndAfterAll with LogSupport {
  logger.resetHandler(new ConsoleLogHandler(SourceCodeLogFormatter))
}

class MyAppClass extends LogSupport {
  error("error message")
  warn("warn message")
  info("info message")
  debug("debug message")
  trace("trace message")

  info(null)
  info(
    """This is a multi-line
      |log message!""".stripMargin)
  info(Seq(1, 2, 3, 4))
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

  override def beforeAll {
    Logger.setDefaultLogLevel(LogLevel.TRACE)
  }

  override def afterAll {
    Logger.setDefaultLogLevel(LogLevel.INFO)
  }

  "logger" should {
    "display log messages" in {
      info("logging test")
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
      val l = Logger("org.sample")
      info(s"logger name: ${l.getName}")
      l.info("hello logger")
    }

    "be able to change log levels" in {
      val l = Logger("org.sample")
      l.setLogLevel(LogLevel.TRACE)
      l.getLogLevel shouldBe LogLevel.TRACE
      l.resetLogLevel
      l.clear
    }

    "support logging methods" in {
      val l = Logger("org.sample")
      l.trace("trace")
      l.debug("debug")
      l.info("info")
      l.warn("warn")
      l.error("error")
    }

    "display exception stack traces" in {
      val e = new Exception("exception test")
      warn("Running stack trace tests")
      trace("error", e)
      debug("error", e)
      info("error", e)
      warn("error", e)
      error("error", e)

      val l = Logger("org.sample")
      l.trace("error", e)
      l.debug("error", e)
      l.info("error", e)
      l.warn("error", e)
      l.error("error", e)
    }

    "support having a concrete logger" in {
      val t = new LocalLogSupport {
        info("hello")
      }
    }

    "use succinct name when used with anonymous trait" in {
      val l = new Sample with LogSupport {
        this.logger.getName shouldBe ("wvlet.log.Sample")
      }
    }

    "Remove $ from object name" in {
      val o = Sample
      o.loggerName shouldBe "wvlet.log.Sample"
    }
  }

  "LogLevel" should {
    "support java.util.LogLevel" in {
      for (l <- Seq(jul.Level.ALL, jul.Level.SEVERE, jul.Level.WARNING, jul.Level.FINE, jul.Level.CONFIG, jul.Level.FINER, jul.Level.FINEST,
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
      LogLevel.unapply("info") shouldBe 'defined
      LogLevel.unapply("unknown-loglevel") shouldNot be('defined)
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
