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

import scala.concurrent.duration.Duration

/**
  */
class LogLevelScannerTest extends Spec {
  override protected def before: Unit = {
    // Ensure stopping log level scanner
    Logger.stopScheduledLogLevelScan
  }

  override protected def after: Unit = {
    Logger.stopScheduledLogLevelScan
  }

  protected def withScanner[U](config: LogLevelScannerConfig)(f: => U): U = {
    val scanner = new LogLevelScanner
    try {
      val lastScanCount = scanner.scanCount.get
      scanner.setConfig(config)
      scanner.start
      // Wait the first scan
      while (scanner.scanCount.get == lastScanCount) {
        Thread.sleep(15)
      }
      f
    } finally {
      scanner.stop
    }
  }

  def `scan log levels only once`: Unit = {
    val l = Logger("wvlet.log.test")
    l.setLogLevel(LogLevel.WARN)
    assert(l.getLogLevel == LogLevel.WARN)
    // Load log-test.properties
    LogLevelScanner.scanLogLevels
    assert(l.getLogLevel == LogLevel.DEBUG)
  }

  def `scan loglevels`: Unit = {
    val l = Logger("wvlet.log.test")
    l.setLogLevel(LogLevel.WARN)
    assert(l.getLogLevel == LogLevel.WARN)

    // Load log-test.properties
    withScanner(
      LogLevelScannerConfig(LogLevelScanner.DEFAULT_LOGLEVEL_FILE_CANDIDATES, Duration(10, TimeUnit.MILLISECONDS))
    ) {
      assert(l.getLogLevel == LogLevel.DEBUG)
    }
  }

  def `load another loglevel file`: Unit = {
    val l = Logger("wvlet.log.test")
    l.setLogLevel(LogLevel.WARN)
    assert(l.getLogLevel == LogLevel.WARN)

    withScanner(LogLevelScannerConfig(List("wvlet/log/custom-log.properties"), Duration(10, TimeUnit.MILLISECONDS))) {
      assert(l.getLogLevel == LogLevel.ERROR)
    }
  }

  def `load invalid loglevel file safely`: Unit = {
    val l = Logger("wvlet.log.test")
    l.setLogLevel(LogLevel.TRACE)

    withScanner(
      LogLevelScannerConfig(List("wvlet/log/invalid-loglevel.properties"), Duration(10, TimeUnit.MILLISECONDS))
    ) {
      // Should ignore unknown log level string
      assert(l.getLogLevel == LogLevel.TRACE)
    }
  }
}
