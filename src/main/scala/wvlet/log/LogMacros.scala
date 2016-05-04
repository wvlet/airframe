package wvlet.log

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
  *
  */
private[log] object LogMacros {

  private class MacroHelper[C <: Context](val c: C) {
    import c.universe._

    def log(level: c.universe.Tree, message: c.universe.Tree): c.universe.Tree = {
      val logger = q"this.logger"
      val pos = c.enclosingPosition
      val l = q"${level}"
      val record = q"wvlet.log.LogRecord(${l}, wvlet.log.LogSource(${pos.source.path}, ${pos.source.file.name}, ${pos.line}, ${
        pos.column
      }), formatLog(${message}))"
      q"if ($logger.isEnabled($l)) $logger.log(${record})"
    }

    def logWithCause(level: c.universe.Tree, message: c.universe.Tree, cause: c.universe.Tree): c.universe.Tree = {
      val logger = q"this.logger"
      val pos = c.enclosingPosition
      val l = q"${level.asInstanceOf[c.universe.Select]}"
      val record = q"wvlet.log.LogRecord(${l}, wvlet.log.LogSource(${pos.source.path}, ${pos.source.file.name}, ${pos.line}, ${
        pos.column
      }), formatLog(${message}), Some(${cause}))"
      q"if ($logger.isEnabled($l)) $logger.log(${record})"
    }

  }

  def errorLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.ERROR", message)
  }

  def errorLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.ERROR", message, cause)
  }

  def warnLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.WARN", message)
  }

  def warnLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.WARN", message, cause)
  }

  def infoLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.INFO", message)
  }

  def infoLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.INFO", message, cause)
  }

  def debugLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.DEBUG", message)
  }

  def debugLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.DEBUG", message, cause)
  }

  def traceLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.TRACE", message)
  }

  def traceLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.TRACE", message, cause)
  }

}
