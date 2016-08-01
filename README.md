wvlet-log
===
wvlet-log helps logging your application behavior. wvlet-log uses the standard `java.util.logging` library,
which is already available in JVM, so it works without adding any dependencies.


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet_log_2.11/)

```
libraryDependencies += "org.wvlet" %% "wvlet-log" % (version)
```


```
improt wvlet.log.LogSupport

object MyApp with LogSupport  {
   info("info log")
   debug("debug log")
}

```

## Customizing log format

You can show the source code location where the log message is generated.

```
import wvlet.log._

object MyApp with LogSupport {
   Logger.setDefaultFormatter(LogFormatter.SourceCodeLogFormatter)

   // This will show [MyApp$] log with source code - (MyApp.scala:7)
   info("log with source code")
}

```

You can also define your own LogFormatter:

```
import wvlet.log.LogFormatter._
object CustomLogFormatter extends LogFormatter {
  override def formatLog(r: LogRecord): String = {
    val log = s"[${highlightLog(r.level, r.leafLoggerName)}] ${highlightLog(r.level, r.getMessage)}"
    appendStackTrace(log, r)
  }
}

Logger.setDefaultFormatter(CustomLogFormatter)

```

## Scala macro based logging code generatio

wvlet-log is efficient since it generate the log message object only when necessary. For example, this logging code:
```
debug("heavy debug log generation ${obj.toString}")

```
will be translated into the following efficient one by using Scala macros:
```
if(logger.isDebugEnabled) {
   debug("heavy debug log generation ${obj.toString}")
}
```


## Why wvlet-log uses `java.util.logging` instead of `slf4j`?

`slf4j` is just an API for logging messages, so you cannot configure log level and its messasge format *within your program*.
 And also, slf4j's logging configruation needs to be binder-specific (e.g., slf4j-simple, logback-core, etc.), and your application always need to include
  a dependency to slf4j implementation.

`java.util.logging` is a standard API and no binding library is required.


You can also redirect the log message to log4j, slf4j, etc.

