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

import java.io.{File, FileReader}
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import wvlet.log.LogLevelScanner.ScannerState
import wvlet.log.io.{IOUtil, Resource}
import wvlet.log.io.IOUtil._

import scala.concurrent.duration.Duration

object LogLevelScanner {
  private val logger = Logger("wvlet.log.LogLevelScanner")

  /**
    * Set log levels using a given Properties file
    *
    * @param file Properties file
    */
  def setLogLevels(file: File) {
    val logLevels = new Properties()
    withResource(new FileReader(file)) { in =>
      logLevels.load(in)
    }
    Logger.setLogLevels(logLevels)
  }

  val DEFAULT_LOGLEVEL_FILE_CANDIDATES = {
    Seq("log-test.properties", "log.properties")
  }

  /**
    * Scan the default log level file only once. To periodically scan, use scheduleLogLevelScan
    */
  def scanLogLevels {
    scanLogLevels(DEFAULT_LOGLEVEL_FILE_CANDIDATES)
  }

  /**
    * Scan the specified log level file
    *
    * @param loglevelFileCandidates
    */
  def scanLogLevels(loglevelFileCandidates: Seq[String]) {
    LogLevelScanner.scan(loglevelFileCandidates, None)
  }

  /**
    * Run the default LogLevelScanner every 1 minute
    */
  def scheduleLogLevelScan {
    scheduleLogLevelScan(LogLevelScannerConfig(DEFAULT_LOGLEVEL_FILE_CANDIDATES, Duration(1, TimeUnit.MINUTES)))
  }

  private[log] lazy val logLevelScanner: LogLevelScanner = new LogLevelScanner

  /**
    * Schedule the log level scanner with the given configuration.
    */
  def scheduleLogLevelScan(config: LogLevelScannerConfig) {
    logLevelScanner.setConfig(config)
    logLevelScanner.start
  }

  /**
    * Schedule the log level scanner with the given interval
    */
  def scheduleLogLevelScan(duration: Duration) {
    scheduleLogLevelScan(LogLevelScannerConfig(DEFAULT_LOGLEVEL_FILE_CANDIDATES, duration))
  }

  /**
    * Terminate the log-level scanner thread. The thread will remain in the system until
    * the next log scan schedule. This is for reusing the thread if scheduleLogLevelScan is called again in a short duration, and
    * reduce the overhead of creating a new thread.
    */
  def stopScheduledLogLevelScan {
    logLevelScanner.stop
  }

  /**
    * @param logLevelFileCandidates
    * @param lastScannedMillis
    * @return updated last scanned millis
    */
  private[log] def scan(logLevelFileCandidates: Seq[String], lastScannedMillis: Option[Long]): Option[Long] = {
    try {
      val logFileURL = logLevelFileCandidates.toStream.flatMap(f => Resource.find(f)).headOption
      logFileURL
        .map { url =>
          url.getProtocol match {
            case "file" =>
              val f            = new File(url.toURI)
              val lastModified = f.lastModified()
              if (lastScannedMillis.isEmpty || lastScannedMillis.get < lastModified) {
                LogLevelScanner.setLogLevels(f)
                Some(System.currentTimeMillis())
              } else {
                lastScannedMillis
              }
            case other if lastScannedMillis.isEmpty =>
              // non file resources found in the class path is stable, so we only need to read it once
              IOUtil.withResource(url.openStream()) { in =>
                val p = new Properties
                p.load(in)
                Logger.setLogLevels(p)
                Some(System.currentTimeMillis())
              }
            case _ =>
              None
          }
        }
        .getOrElse {
          lastScannedMillis
        }
    } catch {
      case e: Throwable =>
        // We need to use the native java.util.logging.Logger since the logger macro cannot be used within the same project
        logger.wrapped.log(LogLevel.WARN.jlLevel, s"Error occurred while scanning log properties: ${e.getMessage}", e)
        lastScannedMillis
    }
  }

  private[log] sealed trait ScannerState
  private[log] object RUNNING  extends ScannerState
  private[log] object STOPPING extends ScannerState
  private[log] object STOPPED  extends ScannerState

}

case class LogLevelScannerConfig(logLevelFileCandidates: Seq[String],
                                 scanInterval: Duration = Duration(1, TimeUnit.MINUTES))

import wvlet.log.LogLevelScanner._

private[log] class LogLevelScanner extends Guard { scanner =>
  private val config: AtomicReference[LogLevelScannerConfig] = new AtomicReference(LogLevelScannerConfig(Seq.empty))
  private val configChanged                                  = newCondition
  private[log] val scanCount                                 = new AtomicLong(0)

  def getConfig: LogLevelScannerConfig = config.get()
  def setConfig(config: LogLevelScannerConfig) {
    guard {
      val prev = this.config.get()
      if (prev.logLevelFileCandidates != config.logLevelFileCandidates) {
        lastScannedMillis = None
      }
      this.config.set(config)
      configChanged.signalAll()
    }
  }

  private val state = new AtomicReference[ScannerState](STOPPED)

  def start {
    guard {
      state.compareAndSet(STOPPING, RUNNING)
      if (state.compareAndSet(STOPPED, RUNNING)) {
        // Create a new thread if the previous thread is terminated
        new LogLevelScannerThread().start
      }
    }
  }

  def stop {
    guard {
      state.set(STOPPING)
    }
  }

  private var lastScheduledMillis: Option[Long] = None
  private var lastScannedMillis: Option[Long]   = None

  private def run {
    // We need to exit here so that the thread can be automatically discarded after the scan interval has passed
    // Otherwise, the thread remains in the classloader(s) if used for running test cases
    while (!state.compareAndSet(STOPPING, STOPPED)) {
      // Periodically run
      val currentTimeMillis  = System.currentTimeMillis()
      val scanIntervalMillis = getConfig.scanInterval.toMillis
      if (lastScheduledMillis.isEmpty || currentTimeMillis - lastScheduledMillis.get > scanIntervalMillis) {
        val updatedLastScannedMillis = scan(getConfig.logLevelFileCandidates, lastScannedMillis)
        scanCount.incrementAndGet()
        guard {
          lastScannedMillis = updatedLastScannedMillis
        }
        lastScheduledMillis = Some(currentTimeMillis)
      }
      // wait until next scheduled time
      val sleepTime = scanIntervalMillis - math.max(
        0,
        math.min(scanIntervalMillis, currentTimeMillis - lastScheduledMillis.get))
      guard {
        if (configChanged.await(sleepTime, TimeUnit.MILLISECONDS)) {
          // awaken due to config change
        }
      }
    }
  }

  private class LogLevelScannerThread extends Thread {
    setName("WvletLogLevelScanner")
    // Enable terminating JVM without shutting down this executor
    setDaemon(true)

    override def run(): Unit = {
      scanner.run
    }
  }
}
