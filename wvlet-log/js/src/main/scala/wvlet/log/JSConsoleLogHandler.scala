package wvlet.log

import scala.scalajs.js.Dynamic.global
import java.util.{logging => jl}

/**
  *
  */
class JSConsoleLogHandler() extends jl.Handler {

  def cssOf(l:LogLevel) : String = l match {
    case LogLevel.ERROR => "color:red"
    case LogLevel.WARN => "color:orange"
    case LogLevel.INFO =>  "color:blue"
    case LogLevel.DEBUG => "color:green"
    case LogLevel.TRACE => "color:magenta"
    case _ => ""
  }

  override def flush(): Unit = {}

  override def publish(record: jl.LogRecord): Unit = {
    record match {
      case r: LogRecord =>
        val ts = LogTimestampFormatter.formatTimestamp(r.getMillis)
        val level = f"${r.level.name}%5s"
        val levelCSS = cssOf(r.level)
        val loc =
          r.source
          .map(source => s"- (${source.fileLoc})")
          .getOrElse("")

        import scala.scalajs.js.DynamicImplicits.truthValue
        if (global.selectDynamic("console")) {
          global.console.log(
            s"""%c${ts} %c${level} %c[${r.leafLoggerName}] %c${r.message} %c${loc}""",
            "color:blue",
            levelCSS,
            "color:gray", // logger name
            "color:black", // message
            "color:blue" // loc
          )
        }
      case _ =>
        publish(LogRecord(record))
    }
  }

  override def close(): Unit = {}
}
