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
  *
  */
class LogLevelScannerTest extends Spec {

  override def afterAll(): Unit = {
    Logger.stopScheduledLogLevelScan
  }

  "LogLevelScanner" should {
    "scan loglevels" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.WARN)
      // Load log-test.properties
      Logger.scheduleLogLevelScan

      // Wait the first scan
      Thread.sleep(1000)
      l.getLogLevel shouldBe LogLevel.DEBUG
    }

    "load another loglevel file" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.WARN)

      Logger.scheduleLogLevelScan(Seq("wvlet/log/custom-log.properties"), Duration(15, TimeUnit.SECONDS))
      Thread.sleep(1000)
      l.getLogLevel shouldBe LogLevel.ERROR
    }

    "load invalid loglevel file safely" in {
      val l = Logger("wvlet.log.test")
      l.setLogLevel(LogLevel.TRACE)

      Logger.scheduleLogLevelScan(Seq("wvlet/log/invalid-loglevel.properties"), Duration(15, TimeUnit.SECONDS))
      Thread.sleep(1000)

      // Should ignore unknown log level string
      l.getLogLevel shouldBe LogLevel.TRACE
    }
  }
}
