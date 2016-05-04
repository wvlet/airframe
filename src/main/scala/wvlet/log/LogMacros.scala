package wvlet.log

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
  *
  */
object LogMacros {

  class MacroHelper(val c: Context) {
    import c.universe._

    def log[L, M](level:L, message:M) = {
      val logger = q"this.logger"
      val pos = c.enclosingPosition
      val l = q"${level.asInstanceOf[c.universe.Select]}"
      val record = q"wvlet.log.LogRecord(${l}, LogSource(${pos.source.path}, ${pos.source.file.name}, ${pos.line}, ${pos.column}), formatLog(${message.asInstanceOf[c.Expr[Any]]}))"
      q"if ($logger.isEnabled($l)) $logger.log(${record})"
    }
  }

  def errorLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    new MacroHelper(c).log(q"wvlet.log.LogLevel.ERROR", message).asInstanceOf[c.Tree]
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
    new MacroHelper(c).log(q"wvlet.log.LogLevel.WARN", message).asInstanceOf[c.Tree]
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
    new MacroHelper(c).log(q"wvlet.log.LogLevel.INFO", message).asInstanceOf[c.Tree]
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
    new MacroHelper(c).log(q"wvlet.log.LogLevel.DEBUG", message).asInstanceOf[c.Tree]
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
    new MacroHelper(c).log(q"wvlet.log.LogLevel.TRACE", message).asInstanceOf[c.Tree]
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
