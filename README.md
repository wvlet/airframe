# wvlet-log  [![Gitter Chat][gitter-badge]][gitter-link] [![CircleCI](https://circleci.com/gh/wvlet/log.svg?style=svg)](https://circleci.com/gh/wvlet/log) [![Coverage Status][coverall-badge]][coverall-link]

[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/log/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/log?branch=master


wvlet-log is a library for adding fancy logging to your Scala application:


![screenshot](docs/wvlet-log.png)

**Features**:
- Based on JVM's built-in `java.util.logging` library. No need to add custom binding jars (e.g., logback-classic in slf4j)
- Developer friendly: 
  - Simple to use: Just add `wvlet.log.LogSupport` trait to your code.
  - You can see the **source code locations** (line number and pos) of log messages.
  - Easy to customize your own log format and log levels *inside* the code. No external XML configuration is required.
  - log levels can be changed at ease with the periodic log-level scanner.
- Production ready
  - Scala macro based logging code generation to instantiate log message only when necessary.  
  - Built-in log file rotation handler.
  - You can also change the log level through the standard JMX interface for `java.util.logging`.  


## Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/)

```scala
libraryDependencies += "org.wvlet" %% "wvlet-log" % "(version)"
```

### Using LogSupport trait

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

### Configuring log levels

If `Logger.scheduleLogLevelScan` is called, wvlet-log periodically scans log-level properties file (default every 1 minute) to configure logger levels:

```scala
improt wvlet.log.Logger

## Scan log files 
Logger.scheduleLogLevelScan
```

***log.properties*** example:
```
# You can use all, trace, debug, info, warn, error, info, all as log level
wvlet.airframe=debug
org.eclipse.jetty=info
org.apache.http=info
com.amazonaws=info
```
The format follows [Java Properties file format](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html#load(java.io.Reader)).


In default, loglevel file will be found in this order:  

 1. `log-test.properties` in the classpath. 
 1. If 1. is not found, use `log.properties` in the classpath.

To change the log file path, you can use `Logger.scheduleLogLevelScan(file paths, duration)`.

In debugging your application, create `src/test/resources/log-test.properties` file, and
call `Logger.scheduleLogLevelScan` before running test cases. This is useful for quickly checking the log messages. 

### Customizing log format

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

See also other examples in [LogFormat.scala](src/main/scala/wvlet/log/LogFormat.scala).

### Using with slf4j

If you are using slf4j, jsut add `slf4j-jdk14` to your dependency. The log message from slf4j will be sent to wvlet-log:
```
libraryDependencies += "org.slf4j" % "slf4j-jdk14" % "1.7.21"
```

### Writing and rotating logs with files 

To write and rotate your logs, use `LogRotationHandler`:
```
logger.resetHandler(new LogRotationHandler(
    fileName = "your.log",
    maxNumberOfFiles = 100, // rotate up to 100 log files
    maxSizeInBytes = 100 * 1024 * 1024 // 100MB
    AppLogFormatter // Any log formatter you like
))
```

## Internals

### Scala macro based logging code generation

wvlet-log is efficient since it generate the log message objects only when necessary. 
For example, this logging code:
```scala
debug(s"heavy debug log generation ${obj.toString}")
```
will be translated into the following efficient one with Scala macros:
```scala
if(logger.isDebugEnabled) {
   debug(s"heavy debug log generation ${obj.toString}")
}
```
Log message String generation will not happen unless the debug log is effective. 
Scala macro is also used for finding source code location (LogSource).


## Why it uses java.util.logging instead of slf4j?

*slf4j* is just an API for logging string messages, so there is no way to configure the log levels and log format *within your program*. To use slf4j, you always need to include an slf4j 
binding library, such as *logback-classic*. slf4j's logging configuration is binder-specific (e.g., slf4j-simple, logback-classic, etc.), 
and your application always need to include a dependency to one of the slf4j implementations. There is nothing wrong in it if these slf4j bindings are used properly, but 
third-party libraries often include slf4j bindings as dependencies, and cause unexpected logging behaviour.  

`java.util.logging` is a standard API of Java and no binding library is required, but configuring `java.util.logging` was still difficult and error prone (See an example in [Stack Overflow](http://stackoverflow.com/questions/960099/how-to-set-up-java-logging-using-a-properties-file-java-util-logging)) 
 *wvlet-log* makes thinkgs easier for Scala developers.


## Related Projects
 
- [scala-logging](https://github.com/typesafehub/scala-logging): 
An wrapper of *slf4j* for Scala. This also uses Scala macros to make logging efficient. No built-in source code location format, and you still need some slf4j bindings and its configuration. 

- [twitter/util-logging](https://github.com/twitter/util#logging): This is also an wrapper of `java.util.logging` for Scala, but it doesn't use Scala macros, so you need to use an old sprintf style log generation, or `ifDebug(log)` 
method to avoid expensive log message generation. 

