package wvlet.log

import java.util.{logging => jl}

class ConsoleLogHandler(formatter: LogFormatter) extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    System.err.println(formatter.format(record))
  }
  override def flush(): Unit = Console.flush()
  override def close(): Unit = {}
}

