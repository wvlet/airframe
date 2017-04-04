Release Notes
====

## 1.2.1 (Upgrade only for Scala.js)
- Add JSConsoleLogHandler for coluful loggin in browser console.
- Add wvlet.log.setDefaultLogLevel(level) for setting a log level in js console.
- Using a custom build of scala-js-java-logging for fixing a problem: https://github.com/scala-js/scala-js-java-logging/pull/12

## 1.2
- Support Scala.js
  - The usage is same. Just add `wvlet.log.LogSupport` in your traits.
  - limitations 
     - LogLevelScanner, LogRotateHandler, AsyncHandler are avilable only for ScalaJVM
     - Because reflection is missing in Scala.js, the logger name of complex traits may not be resolved properly. In this case, call `val logger = wvlet.log.Logger.of(name)` to use a proper logger name.

## 1.1
- Add AsyncHandler for logging heavy log writing process in a background thread
- Add FileHandler 
- Support Scala 2.12.0

## 1.0
- The first major release.
- Migrated from wvlet repository.
- 2016-09-13: Add Scala 2.12.0-M5 support

## 0.23
- Terminate log scanner thread automatically
- Suppress sbt and scalatest related stack trace messages

## 0.22
- Add Logger.scheduleLogLevelScan

## 0.17
- Improved test coverage of wvlet-log

## 0.16
- Avoid using auto-generated annonymous trait name for logger name of LogSupport trait
- Exclude $ from Scala object logger name

## 0.8
- Fix logger methods

## 0.4
- Add LogRotationHandler
- (since 0.1) Add various ANSI color logging LogFormatter

## 0.1
- Added wvlet-log, a handly logging library
