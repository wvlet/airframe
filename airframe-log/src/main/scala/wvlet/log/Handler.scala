package wvlet.log

import java.io.{PrintStream, PrintWriter}
import java.util.{logging => jl}

/**
  * Output log to stderr
  *
  * @param formatter
  */
class ConsoleLogHandler(formatter: LogFormatter, out: PrintStream = LogEnv.defaultConsoleOutput) extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    out.println(formatter.format(record))
  }
  override def flush(): Unit = System.err.flush()
  override def close(): Unit = {}
}

/**
  * Handlers for discarding logs
  */
object NullHandler extends jl.Handler {
  override def flush(): Unit = {
    // do nothing
  }
  override def publish(record: jl.LogRecord): Unit = {
    // do nothing
  }
  override def close(): Unit = {
    // do nothing
  }
}

/**
  * Handlers for storing log messages as a sequence. This is useful for debugging.
  *
  * @param formatter
  */
class BufferedLogHandler(formatter: LogFormatter) extends jl.Handler {
  private val buf = Seq.newBuilder[String]

  override def flush(): Unit = {}
  override def publish(record: jl.LogRecord): Unit =
    synchronized {
      buf += formatter.format(record)
    }
  override def close(): Unit = {
    // do nothing
  }

  def logs: Seq[String] = buf.result()

  def clear: Unit = {
    buf.clear()
  }
}
