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
package wvlet.airframe.fluentd
import wvlet.airframe._
import wvlet.airframe.fluentd.FluentdLoggerTest.{
  Logger1,
  Logger2,
  Logger3,
  LoggerFactory1,
  LoggerFactory2,
  LoggerFactory3
}
import wvlet.airspec.AirSpec
import wvlet.log.LogLevel
import wvlet.log.io.IOUtil

/**
  */
class FluentdLoggerTest extends AirSpec {
  test("should use Fluency as a Fluentd client") {
    val d = fluentd
      .withFluentdLogger()
      .noLifeCycleLogging

    d.build[MetricLoggerFactory] { f =>
      val logger = f.getLogger
    // Just test the client initialization
    // More complete test can be found at FluencyTest
    }
  }

  test("should support console logging") {
    val d =
      fluentd.withDebugConsoleLogging.noLifeCycleLogging

    d.build[MetricLogger] { f => f.emit("data", Map("id" -> 1, "event" -> "GET")) }
  }

  test("generate multiple loggers") {
    val d =
      newDesign
        .bind[Logger1].toInstance(new ConsoleLogger(Some("l1"), LogLevel.DEBUG))
        .bind[Logger2].toInstance(new ConsoleLogger(Some("l2"), LogLevel.DEBUG))
        .bind[Logger3].toInstance(new ConsoleLogger(Some("l3"), LogLevel.DEBUG).withTagPrefix("mytag"))
        .noLifeCycleLogging

    d.withSession { s =>
      val f1 = s.build[LoggerFactory1]
      val f2 = s.build[LoggerFactory2]
      val f3 = s.build[LoggerFactory3]

      f1.getLogger.emit("a", Map("value" -> 1))
      f2.getLogger.emit("a", Map("value" -> 1))
      f3.getLogger.emit("a", Map("value" -> 1))
    }
  }

  test("fluentd logger") {
    // sanity test
    IOUtil.withResource(Fluentd.newFluentdLogger()) { f =>
      //
      f.withTagPrefix("system")
    }

    IOUtil.withResource(Fluentd.newFluentdLogger(tagPrefix = "mymetric")) { f =>
      //
    }
  }

  test("td logger") {
    // sanity test
    IOUtil.withResource(Fluentd.newTDLogger(apikey = "xxxxx")) { td =>
      //
    }

    IOUtil.withResource(Fluentd.newTDLogger(apikey = "xxxxx", tagPrefix = "mymetric")) { td =>
      //
    }

    fluentd.withTDLogger("xxx").noLifeCycleLogging.build[MetricLogger] { l =>
      //
    }
  }
}

object FluentdLoggerTest {
  type Logger1 = MetricLogger
  type Logger2 = MetricLogger
  type Logger3 = MetricLogger

  class LoggerFactory1(l1: Logger1) extends MetricLoggerFactory(l1)
  class LoggerFactory2(l2: Logger2) extends MetricLoggerFactory(l2)
  class LoggerFactory3(l3: Logger3) extends MetricLoggerFactory(l3)
}
