package wvlet.log

import java.util.{logging => jl}

import wvlet.log.JSConsoleLogHandler.JSLogColorPalette

import scala.scalajs.js.Dynamic.global

class JSConsoleLogHandler(logColorPalette: JSLogColorPalette = JSConsoleLogHandler.DEFAULT_COLOR_PALETTE)
    extends jl.Handler {
  override def publish(record: jl.LogRecord): Unit = {
    record match {
      case r: LogRecord =>
        val ts          = LogTimestampFormatter.formatTimestamp(r.getMillis)
        val level       = f"${r.level.name}%5s"
        val logLevelCSS = logColorPalette.cssOf(r.level)
        val loc =
          r.source.map(source => s"- (${source.fileLoc})").getOrElse("")

        import scala.scalajs.js.DynamicImplicits.truthValue
        if (global.selectDynamic("console")) {
          global.console.log(
            s"""%c${ts} %c${level} %c[${r.leafLoggerName}] %c${r.message} %c${loc}""",
            s"color:${logColorPalette.timestamp}", // timestamp
            logLevelCSS,
            s"color:${logColorPalette.loggerName}",  // logger name
            logLevelCSS,                             // log message
            s"color:${logColorPalette.codeLocation}" // loc
          )
        }
      case _ =>
        publish(LogRecord(record))
    }
  }

  override def flush(): Unit = {}
  override def close(): Unit = {}
}

object JSConsoleLogHandler {
  val DEFAULT_COLOR_PALETTE: JSLogColorPalette = new JSLogColorPalette()

  def apply(): JSConsoleLogHandler           = new JSConsoleLogHandler(DEFAULT_COLOR_PALETTE)
  def apply(colorPalette: JSLogColorPalette) = new JSConsoleLogHandler(colorPalette)

  case class JSLogColorPalette(
      error: String = "#D32F2F",
      warn: String = "#E64A19",
      info: String = "#0097A7",
      debug: String = "#388E3C",
      trace: String = "#7B1FA2",
      timestamp: String = "#5C6BC0",
      loggerName: String = "#78909C",
      codeLocation: String = "#B0BEC5",
      default: String = ""
  ) {
    def cssOf(l: LogLevel): String = l match {
      case LogLevel.ERROR => s"color:${error}"
      case LogLevel.WARN  => s"color:${warn}"
      case LogLevel.INFO  => s"color:${info}"
      case LogLevel.DEBUG => s"color:${debug}"
      case LogLevel.TRACE => s"color:${trace}"
      case _              => default
    }
  }
}
