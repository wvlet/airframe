package wvlet.log

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
  *
  */
object LogMacros {

  def errorLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.ERROR"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def errorLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.ERROR"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}), Some(${cause}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def warnLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.WARN"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def warnLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.WARN"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}), Some(${cause}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def infoLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.INFO"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def infoLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.INFO"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}), Some(${cause}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def debugLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.DEBUG"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def debugLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.DEBUG"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}), Some(${cause}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def traceLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.TRACE"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }

  def traceLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"wvlet.log.LogLevel.TRACE"
    val pos = c.enclosingPosition
    val record = q"wvlet.log.LogRecord($level, ${pos.source.path}, ${pos.line}, ${pos.column}, formatLog(${message}), Some(${cause}))"
    q"if ($logger.isEnabled($level)) $logger.log(${record})"
  }


}
