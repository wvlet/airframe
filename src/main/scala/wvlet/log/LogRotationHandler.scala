package wvlet.log

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.logging.ErrorManager
import java.util.{logging => jl}

import ch.qos.logback.core.ContextBase
import ch.qos.logback.core.encoder.EncoderBase
import ch.qos.logback.core.rolling.{RollingFileAppender, SizeAndTimeBasedFNATP, TimeBasedRollingPolicy}

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
                         logFileExt: String = ".log",
                         tempFileExt: String = ".tmp"
                        ) extends jl.Handler {

  import LogRotationHandler._

  recoverTempFiles(fileName)

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

      Try(getFormatter.format(record)) match {
        case Success(message) =>
          Try(fileAppender.doAppend(message)) match {
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
      case Failure(e: Exception) =>
        reportError(null, e, ErrorManager.CLOSE_FAILURE)
    }
  }

  private def recoverTempFiles(logPath: String) {
    // Recover orphaned temp files
    val logPathFile = new File(logPath).getParentFile
    for {
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
