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

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.duration.Duration

/**
  *
  */
class LogLevelScannerTest extends WordSpec with Matchers with BeforeAndAfter {

  before {
    // Ensure stopping log level scanner
    LogLevelScanner.stopScheduledLogLevelScan
  }

  after {
    LogLevelScanner.stopScheduledLogLevelScan
  }

  def withScanner[U](config:LogLevelScannerConfig)(f: => U) : U = {
    val scanner = new LogLevelScanner
    try {
      scanner.setConfig(config)
      val lastScanCount = LogLevelScanner.scanCount.get
      scanner.start
      // Wait the first scan
      while(LogLevelScanner.scanCount.get == lastScanCount) {
        Thread.sleep(1000)
      }
      f
    }
    finally {
      scanner.stop
    }
  }

  "LogLevelScanner" should {

    "scan log levels only once" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.WARN)
      l.getLogLevel shouldBe LogLevel.WARN
      // Load log-test.properties
      val lastScanCount = LogLevelScanner.scanCount.get()
      LogLevelScanner.scanLogLevels
      // Wait the first scan
      while(LogLevelScanner.scanCount.get == lastScanCount) {
        Thread.sleep(1000)
      }
      l.getLogLevel shouldBe LogLevel.DEBUG
    }

    "scan loglevels" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.WARN)
      l.getLogLevel shouldBe LogLevel.WARN

      // Load log-test.properties
      withScanner(LogLevelScannerConfig(LogLevelScanner.DEFAULT_LOGLEVEL_FILE_CANDIDATES, Duration(500, TimeUnit.MILLISECONDS))) {
        l.getLogLevel shouldBe LogLevel.DEBUG
      }
    }

    "load another loglevel file" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.WARN)
      l.getLogLevel shouldBe LogLevel.WARN

      withScanner(LogLevelScannerConfig(Seq("wvlet/log/custom-log.properties"), Duration(500, TimeUnit.MILLISECONDS))) {
        l.getLogLevel shouldBe LogLevel.ERROR
      }
    }

    "load invalid loglevel file safely" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.TRACE)

      withScanner(LogLevelScannerConfig(Seq("wvlet/log/invalid-loglevel.properties"), Duration(500, TimeUnit.MILLISECONDS))) {
        // Should ignore unknown log level string
        l.getLogLevel shouldBe LogLevel.TRACE
      }
    }
  }
}
