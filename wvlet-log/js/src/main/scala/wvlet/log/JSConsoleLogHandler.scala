package wvlet.log

import scala.scalajs.js.Dynamic.global
import java.util.{logging => jl}

/**
  *
  */
class JSConsoleLogHandler() extends jl.Handler {

  def cssOf(l:LogLevel) : String = l match {
    case LogLevel.ERROR => "color:#D32F2F"
    case LogLevel.WARN => "color:#E64A19"
    case LogLevel.INFO =>  "color:#0097A7"
    case LogLevel.DEBUG => "color:#388E3C"
    case LogLevel.TRACE => "color:#7B1FA2"
    case _ => ""
  }

  override def flush(): Unit = {}

  override def publish(record: jl.LogRecord): Unit = {
    record match {
      case r: LogRecord =>
        val ts = LogTimestampFormatter.formatTimestamp(r.getMillis)
        val level = f"${r.level.name}%5s"
        val logLevelCSS = cssOf(r.level)
        val loc =
          r.source
          .map(source => s"- (${source.fileLoc})")
          .getOrElse("")

        import scala.scalajs.js.DynamicImplicits.truthValue
        if (global.selectDynamic("console")) {
          global.console.log(
            s"""%c${ts} %c${level} %c[${r.leafLoggerName}] %c${r.message} %c${loc}""",
            "color:#5C6BC0", // timestamp
            logLevelCSS,
            "color:#78909C", // logger name
            logLevelCSS,     // log message
            "color:#B0BEC5" // loc
          )
        }
      case _ =>
        publish(LogRecord(record))
    }
  }

  override def close(): Unit = {}
}
