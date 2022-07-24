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

/**
  */
trait LoggerBase { self: Logger =>

  inline def error(inline message: Any): Unit = ${ LoggerMacros.errorImpl('this, 'message) }
  inline def warn(inline message: Any): Unit  = ${ LoggerMacros.warnImpl('this, 'message) }
  inline def info(inline message: Any): Unit  = ${ LoggerMacros.infoImpl('this, 'message) }
  inline def debug(inline message: Any): Unit = ${ LoggerMacros.debugImpl('this, 'message) }
  inline def trace(inline message: Any): Unit = ${ LoggerMacros.traceImpl('this, 'message) }

  inline def error(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.errorWithCauseImpl('this, 'message, 'cause)
  }
  inline def warn(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.warnWithCauseImpl('this, 'message, 'cause)
  }

  inline def info(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.infoWithCauseImpl('this, 'message, 'cause)
  }

  inline def debug(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.debugWithCauseImpl('this, 'message, 'cause)
  }

  inline def trace(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.traceWithCauseImpl('this, 'message, 'cause)
  }
}

/**
  */
trait LoggingMethods extends Serializable {
  protected def logger: Logger

  inline protected def error(inline message: Any): Unit = ${ LoggerMacros.errorImpl('logger, 'message) }
  inline protected def warn(inline message: Any): Unit  = ${ LoggerMacros.warnImpl('logger, 'message) }
  inline protected def info(inline message: Any): Unit  = ${ LoggerMacros.infoImpl('logger, 'message) }
  inline protected def debug(inline message: Any): Unit = ${ LoggerMacros.debugImpl('logger, 'message) }
  inline protected def trace(inline message: Any): Unit = ${ LoggerMacros.traceImpl('logger, 'message) }
  inline protected def logAt(inline logLevel: LogLevel, inline message: Any): Unit = ${
    LoggerMacros.logImpl('logger, 'logLevel, 'message)
  }

  inline protected def error(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.errorWithCauseImpl('logger, 'message, 'cause)
  }
  inline protected def warn(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.warnWithCauseImpl('logger, 'message, 'cause)
  }
  inline protected def info(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.infoWithCauseImpl('logger, 'message, 'cause)
  }
  inline protected def debug(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.debugWithCauseImpl('logger, 'message, 'cause)
  }
  inline protected def trace(inline message: Any, inline cause: Throwable): Unit = ${
    LoggerMacros.traceWithCauseImpl('logger, 'message, 'cause)
  }
}

private[log] object LoggerMacros {
  import scala.quoted._

  private def sourcePos(using q: Quotes): Expr[wvlet.log.LogSource] = {
    import q.reflect._
    val pos                         = Position.ofMacroExpansion
    val line                        = Expr(pos.startLine)
    val column                      = Expr(pos.endColumn)
    val src                         = pos.sourceFile
    val srcPath: java.nio.file.Path = java.nio.file.Paths.get(src.path)
    // Do not include the full source code paths for reducing the compiled binary size
    // val path                        = Expr(srcPath.toFile.getPath)
    val fileName = Expr(srcPath.getFileName().toString)
    '{ wvlet.log.LogSource("", ${ fileName }, ${ line } + 1, ${ column }) }
  }

  def logImpl(logger: Expr[Logger], logLevel: Expr[LogLevel], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(${ logLevel })) {
        ${ logger }.log(${ logLevel }, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def errorImpl(logger: Expr[Logger], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.ERROR)) {
        ${ logger }.log(wvlet.log.LogLevel.ERROR, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def warnImpl(logger: Expr[Logger], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.WARN)) {
        ${ logger }.log(wvlet.log.LogLevel.WARN, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def infoImpl(logger: Expr[Logger], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.INFO)) {
        ${ logger }.log(wvlet.log.LogLevel.INFO, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def debugImpl(logger: Expr[Logger], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.DEBUG)) {
        ${ logger }.log(wvlet.log.LogLevel.DEBUG, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def traceImpl(logger: Expr[Logger], message: Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.TRACE)) {
        ${ logger }.log(wvlet.log.LogLevel.TRACE, ${ sourcePos(using q) }, ${ message })
      }
    }
  }

  def errorWithCauseImpl(logger: Expr[Logger], message: Expr[Any], cause: Expr[Throwable])(using
      q: Quotes
  ): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.ERROR)) {
        ${ logger }.logWithCause(wvlet.log.LogLevel.ERROR, ${ sourcePos(using q) }, ${ message }, ${ cause })
      }
    }
  }

  def warnWithCauseImpl(logger: Expr[Logger], message: Expr[Any], cause: Expr[Throwable])(using
      q: Quotes
  ): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.WARN)) {
        ${ logger }.logWithCause(wvlet.log.LogLevel.WARN, ${ sourcePos(using q) }, ${ message }, ${ cause })
      }
    }
  }

  def infoWithCauseImpl(logger: Expr[Logger], message: Expr[Any], cause: Expr[Throwable])(using
      q: Quotes
  ): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.INFO)) {
        ${ logger }.logWithCause(wvlet.log.LogLevel.INFO, ${ sourcePos(using q) }, ${ message }, ${ cause })
      }
    }
  }

  def debugWithCauseImpl(logger: Expr[Logger], message: Expr[Any], cause: Expr[Throwable])(using
      q: Quotes
  ): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.DEBUG)) {
        ${ logger }.logWithCause(wvlet.log.LogLevel.DEBUG, ${ sourcePos(using q) }, ${ message }, ${ cause })
      }
    }
  }

  def traceWithCauseImpl(logger: Expr[Logger], message: Expr[Any], cause: Expr[Throwable])(using
      q: Quotes
  ): Expr[Unit] = {
    '{
      if (${ logger }.isEnabled(wvlet.log.LogLevel.TRACE)) {
        ${ logger }.logWithCause(wvlet.log.LogLevel.TRACE, ${ sourcePos(using q) }, ${ message }, ${ cause })
      }
    }
  }
}
