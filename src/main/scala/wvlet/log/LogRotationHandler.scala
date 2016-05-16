package wvlet.log

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.logging.ErrorManager
import java.util.{logging => jl}

import ch.qos.logback.core.ContextBase
import ch.qos.logback.core.encoder.EncoderBase
import ch.qos.logback.core.rolling.{RollingFileAppender, SizeAndTimeBasedFNATP, TimeBasedRollingPolicy}
import wvlet.log.LogFormatter.AppLogFormatter

import scala.util.{Failure, Success, Try}

object LogRotationHandler {

  /**
    * Encoding log string as UTF-8
    */
  private[LogRotationHandler] class StringEncoder extends EncoderBase[String] {
    override def close(): Unit = {
      outputStream.flush()
    }
    override def doEncode(event: String): Unit = {
      outputStream.write(event.getBytes(StandardCharsets.UTF_8))
      outputStream.flush()
    }
  }
}

/**
  * Log rotation handler
  */
class LogRotationHandler(fileName: String,
                         maxNumberOfFiles: Int,
                         maxSizeInBytes: Long,
                         formatter: LogFormatter = AppLogFormatter,
                         logFileExt: String = ".log",
                         tempFileExt: String = ".tmp"
                        ) extends jl.Handler {

  import LogRotationHandler._

  recoverTempFiles(fileName)
  setFormatter(formatter)

  private val fileAppender = {
    val context = new ContextBase

    val fileAppender = new RollingFileAppender[String]()
    val rollingPolicy = new TimeBasedRollingPolicy[String]
    val triggeringPolicy = new SizeAndTimeBasedFNATP[String]

    rollingPolicy.setContext(context)
    rollingPolicy.setFileNamePattern(s"${fileName}-%d{yyyy-MM-dd}.%i.log.gz")
    rollingPolicy.setMaxHistory(maxNumberOfFiles)
    rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy)
    rollingPolicy.setParent(fileAppender)
    rollingPolicy.start()

    triggeringPolicy.setContext(context)
    triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy)
    triggeringPolicy.setMaxFileSize(maxSizeInBytes.toString)
    triggeringPolicy.start()

    fileAppender.setContext(context)
    fileAppender.setFile(fileName)
    fileAppender.setAppend(true)
    fileAppender.setEncoder(new StringEncoder)
    fileAppender.setRollingPolicy(rollingPolicy)
    fileAppender.start()

    fileAppender
  }

  override def flush() {}

  override def publish(record: jl.LogRecord): Unit = {
    if (isLoggable(record)) {

      val f = Option(getFormatter).getOrElse(formatter)
      Try(f.format(record)) match {
        case Success(message) =>
          Try(fileAppender.doAppend(s"${message}\n")) match {
            case Success(x) =>
              // do nothing
            case Failure(e: Exception) =>
              reportError(null, e, ErrorManager.WRITE_FAILURE)
          }
        case Failure(e: Exception) =>
          reportError(null, e, ErrorManager.FORMAT_FAILURE)
      }
    }
  }

  override def close(): Unit = {
    Try(fileAppender.stop) match {
      case Success(x) =>
        // do nothing
      case Failure(e: Exception) =>
        reportError(null, e, ErrorManager.CLOSE_FAILURE)
    }
  }

  private def recoverTempFiles(logPath: String) {
    // Recover orphaned temp files
    for {
      logPathFile <- Option(new File(logPath).getParentFile)
      fileList <- Option(logPathFile.listFiles)
      tempFile <- fileList.filter(_.getName.endsWith(tempFileExt))
    } {
      val newName = tempFile.getName().substring(0, tempFile.getName().length() - tempFileExt.size)
      val newFile = new File(tempFile.getParent, newName + logFileExt)

      if (!tempFile.renameTo(newFile)) {
        reportError(s"Failed to rename temp file ${tempFile} to ${newFile}", null, ErrorManager.OPEN_FAILURE)
      }
    }
  }
}
