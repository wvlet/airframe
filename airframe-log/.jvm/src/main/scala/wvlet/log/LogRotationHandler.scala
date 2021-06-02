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

import java.io.{File, Flushable}
import java.nio.charset.StandardCharsets
import java.util.logging.ErrorManager
import java.util.{logging => jl}

import ch.qos.logback.core.ContextBase
import ch.qos.logback.core.encoder.EncoderBase
import ch.qos.logback.core.rolling.{RollingFileAppender, SizeAndTimeBasedFNATP, TimeBasedRollingPolicy}
import ch.qos.logback.core.util.FileSize
import wvlet.log.LogFormatter.AppLogFormatter

import scala.util.{Failure, Success, Try}

object LogRotationHandler {

  /**
    * Encoding log string as UTF-8
    */
  private[LogRotationHandler] class StringEncoder extends EncoderBase[String] {
    override def encode(event: String): Array[Byte] = {
      event.getBytes(StandardCharsets.UTF_8)
    }
    override def headerBytes(): Array[Byte] = Array.emptyByteArray
    override def footerBytes(): Array[Byte] = Array.emptyByteArray
  }
}

/**
  * Writing logs to a file without rotation. This is just an wrapper of LogRotationHandler
  * @param fileName
  * @param formatter
  * @param logFileExt
  */
class FileHandler(fileName: String, formatter: LogFormatter = AppLogFormatter, logFileExt: String = ".log")
    extends LogRotationHandler(
      fileName,
      maxNumberOfFiles = Integer.MAX_VALUE,
      maxSizeInBytes = Long.MaxValue,
      formatter = formatter,
      logFileExt = logFileExt
    )

/**
  * Log rotation handler
  */
class LogRotationHandler(
    fileName: String,
    maxNumberOfFiles: Int = 100,      // Rotate up to 100 files
    maxSizeInBytes: Long = 104857600, // 100 MB
    formatter: LogFormatter = AppLogFormatter,
    logFileExt: String = ".log",
    tempFileExt: String = ".tmp"
) extends jl.Handler
    with AutoCloseable
    with Flushable {
  import LogRotationHandler._

  recoverTempFiles(fileName)
  setFormatter(formatter)

  private val fileAppender = {
    val context = new ContextBase

    val fileAppender     = new RollingFileAppender[String]()
    val rollingPolicy    = new TimeBasedRollingPolicy[String]
    val triggeringPolicy = new SizeAndTimeBasedFNATP[String]

    rollingPolicy.setContext(context)
    val fileNameStem =
      if (fileName.endsWith(logFileExt)) fileName.substring(0, fileName.length - logFileExt.length) else fileName
    rollingPolicy.setFileNamePattern(s"${fileNameStem}-%d{yyyy-MM-dd}.%i${logFileExt}.gz")
    rollingPolicy.setMaxHistory(maxNumberOfFiles)
    rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy)
    rollingPolicy.setParent(fileAppender)

    triggeringPolicy.setContext(context)
    triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy)
    triggeringPolicy.setMaxFileSize(new FileSize(maxSizeInBytes))

    fileAppender.setContext(context)
    fileAppender.setFile(fileName)
    fileAppender.setAppend(true)
    fileAppender.setEncoder(new StringEncoder)
    fileAppender.setRollingPolicy(rollingPolicy)

    // RollingPolicy and TriggeringPolicy must be started after configuring the FileAppender
    rollingPolicy.start()
    triggeringPolicy.start()
    fileAppender.start()

    fileAppender
  }

  override def flush(): Unit = {}

  private def toException(t: Throwable) = new Exception(t.getMessage, t)

  override def publish(record: jl.LogRecord): Unit = {
    if (isLoggable(record)) {
      Try(formatter.format(record)) match {
        case Success(message) =>
          Try(fileAppender.doAppend(s"${message}\n")) match {
            case Success(x) =>
            // do nothing
            case Failure(e) =>
              reportError(null, toException(e), ErrorManager.WRITE_FAILURE)
          }
        case Failure(e) =>
          reportError(null, toException(e), ErrorManager.FORMAT_FAILURE)
      }
    }
  }

  override def close(): Unit = {
    Try(fileAppender.stop) match {
      case Success(x) =>
      // do nothing
      case Failure(e) =>
        reportError(null, toException(e), ErrorManager.CLOSE_FAILURE)
    }
  }

  private def recoverTempFiles(logPath: String): Unit = {
    // Recover orphaned temp files
    for {
      logPathFile <- Option(new File(logPath).getParentFile)
      fileList    <- Option(logPathFile.listFiles)
      tempFile    <- fileList.filter(_.getName.endsWith(tempFileExt))
    } {
      val newName = tempFile.getName().substring(0, tempFile.getName().length() - tempFileExt.size)
      val newFile = new File(tempFile.getParent, newName + logFileExt)

      if (!tempFile.renameTo(newFile)) {
        reportError(s"Failed to rename temp file ${tempFile} to ${newFile}", null, ErrorManager.OPEN_FAILURE)
      }
    }
  }
}
