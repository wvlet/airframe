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
    q"if ($logger.isErrorEnabled) $logger.error(formatLog($message))"
  }

  def errorLogWithCause(c: Context)(message: c.Expr[Any], e: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isErrorEnabled) $logger.error(formatLog($message), e)"
  }

  def warnLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isWarnEnabled) $logger.warn(formatLog($message))"
  }

  def warnLogWithCause(c: Context)(message: c.Expr[Any], e: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isWarnEnabled) $logger.warn(formatLog($message), e)"
  }


  def infoLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isInfoEnabled) $logger.info(formatLog($message))"
  }

  def infoLogWithCause(c: Context)(message: c.Expr[Any], e: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isInfoEnabled) $logger.info(formatLog($message), e)"
  }


  def debugLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isDebugEnabled) $logger.debug(formatLog($message))"
  }

  def debugLogWithCause(c: Context)(message: c.Expr[Any], e: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isDebugEnabled) $logger.debug(formatLog($message), e)"
  }

  def traceLog(c: Context)(message: c.Expr[Any]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isTraceEnabled) $logger.trace(formatLog($message))"
  }

  def traceLogWithCause(c: Context)(message: c.Expr[Any], e: c.Expr[Throwable]) = {
    import c.universe._
    val logger = q"${c.prefix}.logger"
    q"if ($logger.isTraceEnabled) $logger.trace(formatLog($message), e)"
  }

}
