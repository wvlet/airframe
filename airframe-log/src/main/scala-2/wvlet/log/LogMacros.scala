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

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
  * Scala macros for generating log output code. This class inserts a code that checkes log level. If the logging is
  * enabled, it sends a log with the source code location where the logging method is called.
  *
  * The log message object will created only if the log level is enabled, so logger.trace(xxx) etc. can be used without
  * any overhead.
  */
private[log] object LogMacros {
  private class MacroHelper[C <: Context](val c: C) {
    import c.universe._

    val disabledLevels: Set[c.Tree] = {
      val SettingsPrefix = "wvlet.log.disable."

      val TRACE: c.Tree = q"wvlet.log.LogLevel.TRACE"
      val DEBUG: c.Tree = q"wvlet.log.LogLevel.DEBUG"
      val INFO: c.Tree  = q"wvlet.log.LogLevel.INFO"
      val WARN: c.Tree  = q"wvlet.log.LogLevel.WARN"
      val ERROR: c.Tree = q"wvlet.log.LogLevel.ERROR"

      c.settings
        .collect { case s if s startsWith SettingsPrefix => s stripPrefix SettingsPrefix }
        .collectFirst {
          case "ALL" | "ERROR" => Set(TRACE, DEBUG, INFO, WARN, ERROR)
          case "WARN"          => Set(TRACE, DEBUG, INFO, WARN)
          case "INFO"          => Set(TRACE, DEBUG, INFO)
          case "DEBUG"         => Set(TRACE, DEBUG)
          case "TRACE"         => Set(TRACE)
        }
        .getOrElse(Set.empty)
    }

    private def disabled(level: c.Tree): Boolean = disabledLevels.exists(_.equalsStructure(level))

    def source = {
      val pos = c.enclosingPosition
      q"wvlet.log.LogSource(${""}, ${pos.source.file.name}, ${pos.line}, ${pos.column})"
    }

    def log(level: c.Tree, message: c.universe.Tree): c.Tree = {
      val logger = q"this.logger"
      if (disabled(level)) q"{}" else q"if ($logger.isEnabled($level)) $logger.log(${level}, ${source}, ${message})"
    }

    def logWithCause(level: c.Tree, message: c.Tree, cause: c.Tree): c.Tree = {
      val logger = q"this.logger"
      if (disabled(level)) q"{}"
      else q"if ($logger.isEnabled($level)) $logger.logWithCause(${level}, ${source}, ${message}, ${cause})"
    }

    def logMethod(level: c.Tree, message: c.universe.Tree): c.Tree = {
      if (disabled(level)) q"{}"
      else q"if (${c.prefix}.isEnabled($level)) ${c.prefix}.log(${level}, ${source}, ${message})"
    }

    def logMethodWithCause(level: c.Tree, message: c.Tree, cause: c.Tree): c.Tree = {
      if (disabled(level)) q"{}"
      else q"if (${c.prefix}.isEnabled($level)) ${c.prefix}.logWithCause(${level}, ${source}, ${message}, ${cause})"
    }
  }

  def logAtImpl(c: Context)(logLevel: c.Tree, message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(logLevel, message)
  }

  def errorLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.ERROR", message)
  }

  def errorLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.ERROR", message, cause)
  }

  def errorLogMethod(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethod(q"wvlet.log.LogLevel.ERROR", message)
  }

  def errorLogMethodWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethodWithCause(q"wvlet.log.LogLevel.ERROR", message, cause)
  }

  def warnLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.WARN", message)
  }

  def warnLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.WARN", message, cause)
  }

  def warnLogMethod(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethod(q"wvlet.log.LogLevel.WARN", message)
  }

  def warnLogMethodWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethodWithCause(q"wvlet.log.LogLevel.WARN", message, cause)
  }

  def infoLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.INFO", message)
  }

  def infoLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.INFO", message, cause)
  }

  def infoLogMethod(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethod(q"wvlet.log.LogLevel.INFO", message)
  }

  def infoLogMethodWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethodWithCause(q"wvlet.log.LogLevel.INFO", message, cause)
  }

  def debugLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.DEBUG", message)
  }

  def debugLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.DEBUG", message, cause)
  }

  def debugLogMethod(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethod(q"wvlet.log.LogLevel.DEBUG", message)
  }

  def debugLogMethodWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethodWithCause(q"wvlet.log.LogLevel.DEBUG", message, cause)
  }

  def traceLog(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).log(q"wvlet.log.LogLevel.TRACE", message)
  }

  def traceLogWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logWithCause(q"wvlet.log.LogLevel.TRACE", message, cause)
  }

  def traceLogMethod(c: Context)(message: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethod(q"wvlet.log.LogLevel.TRACE", message)
  }

  def traceLogMethodWithCause(c: Context)(message: c.Tree, cause: c.Tree): c.Tree = {
    import c.universe._
    new MacroHelper[c.type](c).logMethodWithCause(q"wvlet.log.LogLevel.TRACE", message, cause)
  }
}
