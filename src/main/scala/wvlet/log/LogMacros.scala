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
    val level = q"java.util.logging.Level.SEVERE"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message))"
  }

  def errorLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.SEVERE"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message), $cause)"
  }

  def warnLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.WARNING"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message))"
  }

  def warnLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.WARNING"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message), $cause)"
  }

  def infoLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.INFO"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message))"
  }

  def infoLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.INFO"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message), $cause)"
  }

  def debugLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.FINE"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message))"
  }

  def debugLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.FINE"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message), $cause)"
  }

  def traceLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.FINER"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message))"
  }

  def traceLogWithCause(c: Context)(message: c.Expr[Any], cause: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"this.logger"
    val level = q"java.util.logging.Level.FINER"
    q"if ($logger.isLoggable($level)) $logger.log($level, formatLog($message), $cause)"
  }

}
