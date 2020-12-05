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
  inline def warn(inline message: Any, inline cause: Throwable): Unit  = ${
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
  protected def error(message: Any): Unit                     = ???
  protected def error(message: Any, cause: Throwable): Unit   = ???
  protected def warn(message: Any): Unit                      = ???
  protected def warn(message: Any, cause: Throwable): Unit    = ???
  protected def info(message: Any): Unit                      = ???
  protected def info(message: Any, cause: Throwable): Unit    = ???
  protected def debug(message: Any): Unit                     = ???
  protected def debug(message: Any, cause: Throwable): Unit   = ???
  protected def trace(message: Any): Unit                     = ???
  protected def trace(message: Any, cause: Throwable): Unit   = ???
  protected def logAt(logLevel: LogLevel, message: Any): Unit = ???
}

private[log] object LoggerMacros {
  import scala.quoted._

  private def sourcePos(using q: Quotes): Expr[wvlet.log.LogSource] = {
    import q.reflect._
    val pos = Position.ofMacroExpansion
    val line = Expr(pos.startLine)
    val column = Expr(pos.endColumn)
    val src = pos.sourceFile
    val srcPath: java.nio.file.Path = src.jpath
    val path = Expr(srcPath.toFile.getPath)
    val fileName = Expr(srcPath.getFileName().toString)
    '{ wvlet.log.LogSource(${path}, ${fileName}, ${line}, ${column}) }
  }

  def errorImpl(logger: Expr[Logger], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.ERROR)) {
        ${logger}.log(wvlet.log.LogLevel.ERROR, ${sourcePos}, ${message})
      }
    }
  }

  def warnImpl(logger: Expr[Logger], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.WARN)) {
        ${logger}.log(wvlet.log.LogLevel.WARN, ${sourcePos}, ${message})
      }
    }
  }

  def infoImpl(logger: Expr[Logger], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.INFO)) {
        ${logger}.log(wvlet.log.LogLevel.INFO, ${sourcePos}, ${message})
      }
    }
  }

  def debugImpl(logger: Expr[Logger], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.DEBUG)) {
        ${logger}.log(wvlet.log.LogLevel.DEBUG, ${sourcePos}, ${message})
      }
    }
  }

  def traceImpl(logger: Expr[Logger], message:Expr[Any])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.TRACE)) {
        ${logger}.log(wvlet.log.LogLevel.TRACE, ${sourcePos}, ${message})
      }
    }
  }

  def errorWithCauseImpl(logger: Expr[Logger], message:Expr[Any], cause: Expr[Throwable])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.ERROR)) {
        ${logger}.logWithCause(wvlet.log.LogLevel.ERROR, ${sourcePos}, ${message}, ${cause})
      }
    }
  }

  def warnWithCauseImpl(logger: Expr[Logger], message:Expr[Any], cause: Expr[Throwable])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.WARN)) {
        ${logger}.logWithCause(wvlet.log.LogLevel.WARN, ${sourcePos}, ${message}, ${cause})
      }
    }
  }

  def infoWithCauseImpl(logger: Expr[Logger], message:Expr[Any], cause: Expr[Throwable])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.INFO)) {
        ${logger}.logWithCause(wvlet.log.LogLevel.INFO, ${sourcePos}, ${message}, ${cause})
      }
    }
  }

  def debugWithCauseImpl(logger: Expr[Logger], message:Expr[Any], cause: Expr[Throwable])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.DEBUG)) {
        ${logger}.logWithCause(wvlet.log.LogLevel.DEBUG, ${sourcePos}, ${message}, ${cause})
      }
    }
  }

  def traceWithCauseImpl(logger: Expr[Logger], message:Expr[Any], cause: Expr[Throwable])(using q: Quotes): Expr[Unit] = {
    '{
      if(${logger}.isEnabled(wvlet.log.LogLevel.TRACE)) {
        ${logger}.logWithCause(wvlet.log.LogLevel.TRACE, ${sourcePos}, ${message}, ${cause})
      }
    }
  }
}


