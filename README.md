wvlet-log
===
wvlet-log helps logging your application behavior. wvlet-log uses the standard `java.util.logging` library,
which is already available in JVM, so it works without adding any dependencies.


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/)

```scala
libraryDependencies += "org.wvlet" %% "wvlet-log" % "(version)"
```

### LogSupport trait

The most convenient way to use wvlet-log is adding `LogSupport` to your class:

```scala
import wvlet.log.LogSupport

object MyApp extends LogSupport  {
   info("info log")
   debug("debug log")
}
```

The logger name will be determined from your class name (e.g., `MyApp`).

Alternatively you can load `Logger` instance manually:

```scala
import wvlet.log.Logger

class YourApp {
   private val logger = Logger.of[YourApp]
}
```


## Customizing log format

You can show the source code location where the log message is generated:

```scala
import wvlet.log._

object MyApp with LogSupport {
   Logger.setDefaultFormatter(LogFormatter.SourceCodeLogFormatter)

   info("log with source code")
}

```
This code will show:
```
[MyApp$] log with source code - (MyApp.scala:6)
```

You can also define your own LogFormatter:

```scala
import wvlet.log.LogFormatter._
object CustomLogFormatter extends LogFormatter {
  override def formatLog(r: LogRecord): String = {
    val log = s"[${highlightLog(r.level, r.leafLoggerName)}] ${highlightLog(r.level, r.getMessage)}"
    appendStackTrace(log, r)
  }
}

Logger.setDefaultFormatter(CustomLogFormatter)
```

See also other examples in <wvlet-log/src/main/scala/wvlet/log/LogFormatter.scala>.


## Internals

### Scala macro based logging code generation

wvlet-log is efficient since it generate the log message object only when necessary. For example, this logging code:
```scala
debug(s"heavy debug log generation ${obj.toString}")
```
will be translated into the following efficient one by using Scala macros:
```scala
if(logger.isDebugEnabled) {
   debug(s"heavy debug log generation ${obj.toString}")
}
```
Log message String generation will not occure unless debug log is effective. Scala macro is also used for finding source code location (LogSource).


## Why wvlet-log uses `java.util.logging` instead of `slf4j`?

`slf4j` is just an API for logging messages, so you cannot configure log level and its messasge format *within your program*.
 And also, slf4j's logging configruation needs to be binder-specific (e.g., slf4j-simple, logback-core, etc.), and your application always need to include
  a dependency to slf4j implementation. `java.util.logging` is a standard API and no binding library is required.

You can also redirect wvlet-log message to log4j, slf4j, etc.

