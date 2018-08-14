---
layout: docs
title: Release Notes
---

# Release Notes

## 0.53
- Publish Scala 2.12 only jvm projects ([97550b2](https://github.com/wvlet/airframe/commit/97550b2))
- Add Simple HTTP REST framework ([#206](https://github.com/wvlet/airframe/issues/206)) ([1467934](https://github.com/wvlet/airframe/commit/1467934))
- Bump sbt version to 1.2.1, also upgrading sbt plugins ([#205](https://github.com/wvlet/airframe/issues/205)) ([e5d2e1a](https://github.com/wvlet/airframe/commit/e5d2e1a))
- Upgrade to sbt 1.2.0 ([#202](https://github.com/wvlet/airframe/issues/202)) ([382eda6](https://github.com/wvlet/airframe/commit/382eda6))
- Upgrade sbt plugins ([#201](https://github.com/wvlet/airframe/issues/201)) ([7bfe82b](https://github.com/wvlet/airframe/commit/7bfe82b))
- [#198](https://github.com/wvlet/airframe/issues/198): Show global options as well to help messages ([#199](https://github.com/wvlet/airframe/issues/199)) ([16c2b66](https://github.com/wvlet/airframe/commit/16c2b66))

## 0.52
 - Support Scala 2.13.0-M4

## 0.51
 - airframe-log: Add Logger.clearAllHandlers (#195)

## 0.50
 - airframe-control: Add a code for retrying code execution
 - airframe-stream: Add a simple SQL parser/interpreter for stream processing https://github.com/wvlet/airframe/projects/1

## 0.49
 - __Important__: `bind[X]`, `bind[X].to[Y]`, etc. now use singleton bindings by default. This is because most of the use cases expect binding
 the same objects to traits. If you need to explicitly instantiate objects for each binding, use `bindInstance[X]` or `bind[X].toInstanceOf[Y]`.
 - Add a handy method to start a session and build an object: `Design.build[A]{ a => ... }`.
 - Support eager singleton initialization with `Design.buildProduction[A]` and `Design.withProductionSession`.
 - (internal) Upgrade to sbt 1.1.6, Scala.js 0.6.23, scalajs-java-logging 0.1.4

## 0.48
 - Java 9/10 support
 - airframe-msgpack. Fixes a bug in enocoding Timestamp (Instant) values
 - airframe DI: Enhanced warning/error messages upon missing dependencies.

## 0.47
 - airframe-msgpack: Add a pure Scala [MessagePack](https://msgpack.org) implementation

## 0.46
 - Add airframe-bootstrap to define application modules that can be configured with airframe-config and DI
 - Upgrade to Scala 2.12.6

## 0.45
 - airframe-codec: Support `List[X]` coding
 - airframe-surface: Support `Parameter.getDefaultValue` and `Parameter.get(obj)` in Scala.js  

## 0.44
 - airframe-metrics: Require minus sign to represent last time ranges (e.g., -7d, -1h, etc.)
 - airframe: Enable `bind[Session]` binding to retrieve the current session

## 0.43
 - airframe-log: Set a default log handler (JSConsoleLogHandler) for Scala.js for convenience

## 0.42
 - airframe-log: Add an option to totally disable logging code generation.

## 0.41
 - Fix a singleton binding bug: If `bind[A].toSingletonOf[B]` is used and A is non-abstract trait, A was used instead of B.

## 0.40
 - airframe-opts: Fixes [#147](https://github.com/wvlet/airframe/issues/147) when reading the default values of nested options

## 0.39
 - Add a unified scaladoc for all modules

## 0.38
 - airframe-tablet: Using json4s-native instead of play-json for Scala 2.13.0-M3 support

## 0.37
 - [airframe-tablet](airframe-tablet.html) Add play-json based JSONCodec to enable transformation between JSON <-> MessagePack <-> Object.
 - Temporarily remove Scala 2.13.0-M2 build. Now preparing 2.13.0-M3 build in [#143](https://github.com/wvlet/airframe/pull/143)

## 0.36
 - Fixes Logger.scanLoglevels to use given log level files appropriately [#141](https://github.com/wvlet/airframe/pull/141)

## 0.35
 - Minor fixes to project strucutures and build scripts.

## 0.34
 - No major change in terms of features, API.
 - Airframe now uses [sbt-dnyver](https://github.com/dwijnand/sbt-dynver) for auto versioning.
 - For each commit in the master branch, you can find a snapshot version from https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/
   - For example, [airframe-log_2.12/0.33.1+12-682c4e07-SNAPSHOT](https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/airframe-log_2.12/0.33.1+12-682c4e07-SNAPSHOT/), means that this version is based on the version `0.33.1` and `12` commits away from the previous version tag, and using git revision `682c4e07`.
 - The release versions will be like `0.34` as usual and available from Maven central.

## 0.33
 - Add [airframe-jdbc](airframe-jdbc.html), a reusable JDBC connection pool implementation.  

## 0.32
 - Fix ObjectCodec for map type value [#131](https://github.com/wvlet/airframe/pull/131)

## 0.31
 - Support recursive types

## 0.30
 - *airframe-metrics*: Add support for Scala.js (DataSize, ElapsedTime)
 - Add ElapsedTime for succinct duration representation

## 0.29
 - Add airframe-codec, a [MessgePack](https://msgpack.org)-based schema-on-read data transcoder.

## 0.28
 - Add support for Scala 2.13.0-M2

## 0.27
 - aiframe-config: Fixes nested case class binding [#108](https://github.com/wvlet/airframe/issues/110)
 - Dropped support of Scala.js + Scala 2.11 combination. Scala.js libraries will support only Scala 2.12 (or higher) in upcoming releases

## 0.26
 - Add an initial version of [airframe-tablet](https://github.com/wvlet/airframe/tree/master/tablet), a [MessagePack](https://msgpack.org) based data transcoder

## 0.25
 - **airframe-log** Upgrade the internal log rotation library to logback-core 1.2.3
 - Upgrade to Scala 2.12.4 and sbt 1.0.3. 

## 0.24
 - Add [airframe-metrics](airframe-metrics.html)

## 0.23
 - Moved to `org.wvlet.airframe` organization because Airframe will have more modules in future.
 - Added airframe-spec (a common library for testing airframe modules)

## 0.22
 - Add Logger.clearAllHandlers to clean-up existing log handlers set by third-party libraries
 - Apply scalafmt code style 

## 0.21
 - Add [airframe-opts](https://github.com/wvlet/airframe/tree/master/airframe-opts) command line parser.

## 0.20
 - Migrated [airframe-log](https://github.com/wvlet/airframe/tree/master/airframe-log) from [wvlet-log](https://github.com/wvlet/log) because 
these are often commonly used.

## 0.19
 - Add [airframe-jmx](https://github.com/wvlet/airframe/tree/master/airframe-jmx) to expose object parameters through JMX

## 0.18
 - Add beforeShutdown, onInject lifecycle hooks
 - Support `@PostConstruct` and `@PreDestroy` JSR-250 annotations (Scala.js is not supported)
 - Fix to call onInit, onStart only once for singletons

## 0.17
 - Upgrade to Scala 2.12.3, Scala.js 0.6.19
 - Add Design.add
 - Fix a bug, onStart lifecycle hook is not called when a session is already started
 - Fix to use default constructor argument values for constructor bindings
   - Note: Scala.js does not support default parameter binding yet
 
## 0.16
 - Support reading Map type values in YamlReader (airframe config)

## 0.15
 - Surface will be gererated by using runtime-type information. This improves the compilation speed in Scala JVM projects.
   - Scala.js version still uses compile-time surface generation
 - Interface change: `Surface.of[X]` to `surface.of[X]`.
   - This is for providing different implementations of Surface for ScalaJVM and Scala.js.
 - Fixed an issue when tagged type is used inside constructor parameters.
 - Fixed an issue that lifecycle hooks are wrongly called when debug level logging is used.
 - Merged wvlet-config as airframe-config. This module is useful for creating configuration objects from YAML file.

## 0.14
 - [Surface](https://github.com/wvlet/airframe/tree/master/surface) is now a part of Airframe
 - Added RuntimeSurface for the convenience of creating Surface from runtime-type information (reflect.universe.Type)
 - Added ObjectBuilder to build objects from Surface information

## 0.13
 - Add tagged binding support

## 0.12
 - Airframe is now refrection-free.
   - Using a reflection free [Surface](https://github.com/wvlet/surface) instaed of [ObjectSchema](https://github.com/wvlet/object-schema)
   - Cyclic dependencies now can be found at compile-time thanks to Surface
 - Initial Scala.js support
 - Split Travis build process for the matrix of Scala 2.12/2.11, ScalaJVM/JS
 - Add shortcut for life cycle events: bind[X].onInit/onStart/onShutdown
 - Deprecated tagged type binding. Instead you can use type alias bind[(type alias)].
 - Deprecated @PreDestroy, @PostConstruct lifecycle binding because it complicates the order of life cycle hooks when mixed in
 - Deprecated passing Session as a class parameter or method return value. 
 - Simplified Scala macro codes

## 0.10
 - Session now implements AutoClosable
 - Add Design.withSession{ session => ... } to close Session at ease 

## 0.9
 - Add Scala 2.12 support

## 0.8
 - Split the project into two sub projects: airframe and airframe-macro. However, you only need airframe.jar for using Airframe.
 - Improved the performance by replacing run-time code generation to compile-time code generation #25
 - Upgrade to wvlet-log 1.0 
 - Upgrade wvlet-obj 0.26 (support type aliases)
 - Allow binding to type aliases

## 0.7
 - Fix lifecycle management of objects generated by bindSingleton

## 0.6
 - Add bindSingleton[X]

## 0.5 
 - Fix a bug in FILO order init/shutdown when objects are singleton
 - Add toProvider/toSingletonProvider/toEagerSingletonProvider
 - Add Design.remove[X]

## 0.4 
 - Improved binding performance
 - Fix FIFO lifecycle hook executor
 - Improved injection logging

## 0.3 
 - Support bind(ObjectType).toXXX 

## 0.2
 - Add lifecycle manageer
 - Reorganize Session, Design classes
 - Test coverage improvement
 - Depreacted Design.build[X]. Use Design.newSession.build[X]

## 0.1
 - Migrated from wvlet-inject
