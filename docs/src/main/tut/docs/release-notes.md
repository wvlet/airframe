---
layout: docs
title: Release Notes
---

# Release Notes

## 0.73
-  airfarme-opts is redesigned as [airframe-launcher](https://wvlet.org/airframe/docs/airframe-launcher.html) Option parser v2 ([#295](https://github.com/wvlet/airframe/issues/295)) [[a270128](https://github.com/wvlet/airframe/commit/a270128)]
-  Support netsted offset in time window ([#296](https://github.com/wvlet/airframe/issues/296)) [[0c6e218](https://github.com/wvlet/airframe/commit/0c6e218)]
-  Add untruncate notation `)` to offset of time window [[3d479be](https://github.com/wvlet/airframe/commit/3d479be)]
-  Add wvlet.airframe.log.init for using default log configurations ([#291](https://github.com/wvlet/airframe/issues/291)) [[a08d15d](https://github.com/wvlet/airframe/commit/a08d15d)]
-  Use fluentd-standalone 1.2.6.1, which supports Scala 2.13.0-M5 ([#289](https://github.com/wvlet/airframe/issues/289)) [[ed7012b](https://github.com/wvlet/airframe/commit/ed7012b)]

## 0.72
-  Support Scala 2.13.0-M5 ([#232](https://github.com/wvlet/airframe/issues/232)) [[c31a4da](https://github.com/wvlet/airframe/commit/c31a4da)]
   - Notice: Due to a [bug](https://github.com/scala/bug/issues/11192) of Scala 2.13.0-M5, serilization of Design objects may not work when using sbt with Scala 2.13.0-M5. Using `fork in Test := true` is a workaround.

## 0.71
-  Added [airframe-fluentd](https://wvlet.org/airframe/docs/airframe-fluentd.html) for sending object-based metrics to Fluentd. ([#286](https://github.com/wvlet/airframe/issues/286)) [[28a70c3](https://github.com/wvlet/airframe/commit/28a70c3)]

## 0.70
-  airframe-control: Add a shell command launcher ([#287](https://github.com/wvlet/airframe/issues/287)) [[1339caf](https://github.com/wvlet/airframe/commit/1339caf)]
-  Upgrade to sbt 1.2.6 ([#285](https://github.com/wvlet/airframe/issues/285)) [[614dba6](https://github.com/wvlet/airframe/commit/614dba6)]
-  Fix typos in docs

## 0.69
-  surface: [#270](https://github.com/wvlet/airframe/issues/270): Move wvlet.surface -> wvlet.airframe.surface ([#272](https://github.com/wvlet/airframe/issues/272)) [[5ca3143](https://github.com/wvlet/airframe/commit/5ca3143)]
-  docs: Add note on scalafmt [[6867642](https://github.com/wvlet/airframe/commit/6867642)]
-  docs: Add [airframe-opts](https://wvlet.org/airframe/docs/airframe-launcher.html) docs [[dd4ecb4](https://github.com/wvlet/airframe/commit/dd4ecb4)]
-  internal: Enable using ctrl+c in sbt console [[143053b](https://github.com/wvlet/airframe/commit/143053b)]

## 0.68
-  Support JDK11 ([#265](https://github.com/wvlet/airframe/issues/265)) [[7e8799d](https://github.com/wvlet/airframe/commit/7e8799d)]
-  Use openjdk because we will mostly use this free JDK versions ([#266](https://github.com/wvlet/airframe/issues/266)) [[fd7f641](https://github.com/wvlet/airframe/commit/fd7f641)]
-  Upgrade to Scala 2.12.7 for Travis builds [[ce10f55](https://github.com/wvlet/airframe/commit/ce10f55)]
-  Remove snapshot repo resolver to speedup dependency download ([#267](https://github.com/wvlet/airframe/issues/267)) [[247fa28](https://github.com/wvlet/airframe/commit/247fa28)]

## 0.67
-  Add bindFactory[A => B] ([#262](https://github.com/wvlet/airframe/issues/262)) [[a38375f](https://github.com/wvlet/airframe/commit/a38375f)]
  - Usage: [Factory Binding](https://wvlet.org/airframe/docs/use-cases.html#factory-binding)
  - Simplified the implementation of AirframeSession
- Fixes typo, misspelled methods, etc. Thanks [Kazuhiro Sera](https://github.com/seratch)  
   - Fix [#256](https://github.com/wvlet/airframe/issues/256) by limiting the visibility of Design.blanc ([#259](https://github.com/wvlet/airframe/issues/259)) [[f0ce25a](https://github.com/wvlet/airframe/commit/f0ce25a)]
   - Fix typo, misspelling words, unnecessary imports ([#258](https://github.com/wvlet/airframe/issues/258)) [[b22f4cf](https://github.com/wvlet/airframe/commit/b22f4cf)]
   - Fix typo in http,finagle modules ([#254](https://github.com/wvlet/airframe/issues/254)) [[b008335](https://github.com/wvlet/airframe/commit/b008335)]
   - Use the deprecated annotation with proper attributes ([#257](https://github.com/wvlet/airframe/issues/257)) [[0843f79](https://github.com/wvlet/airframe/commit/0843f79)]
-  Bump sbt from 1.2.1 to 1.2.3 ([#255](https://github.com/wvlet/airframe/issues/255)) [[428a284](https://github.com/wvlet/airframe/commit/428a284)]
-  Add links to Medium blogs [[4d13933](https://github.com/wvlet/airframe/commit/4d13933)]

## 0.66
- Add Finagle startup helper ([#252](https://github.com/wvlet/airframe/issues/252)) [[d3b4767](https://github.com/wvlet/airframe/commit/d3b4767)]
  - Add airframe-http-finagle examples ([#254](https://github.com/wvlet/airframe/issues/253)) [[4698ba5](https://github.com/wvlet/airframe/commit/4698ba5)]
- [#249](https://github.com/wvlet/airframe/issues/249): Do not create factories for path-dependent types ([#250](https://github.com/wvlet/airframe/issues/250)) [[d2433bb](https://github.com/wvlet/airframe/commit/d2433bb)]

## 0.65
- Seamless integration with airframe-config and Design: https://wvlet.org/airframe/docs/airframe-config.html#using-with-airframe
  - `import wvlet.airframe.config._` to use newly added binding methods: `bindConfig[X]`, `bindConfigFromYaml[X](yamlFile)`, etc. ([#248](https://github.com/wvlet/airframe/issues/248)) [[379332b](https://github.com/wvlet/airframe/commit/379332b)]
  - Deprecated airframe-bootstrap
- Allow setting log levels using wvlet.logger JMX MBean ([#247](https://github.com/wvlet/airframe/issues/247)) [[962d6c2](https://github.com/wvlet/airframe/commit/962d6c2)]

## 0.64
- Support higher kinded types more naturally ([#244](https://github.com/wvlet/airframe/issues/244)) [[e21baef](https://github.com/wvlet/airframe/commit/e21baef)]

## 0.63
- Support higher-kinded type bindings. ([#242](https://github.com/wvlet/airframe/issues/242)) [[cfe35ed](https://github.com/wvlet/airframe/commit/cfe35ed)]
- Upgrade to scala.js 0.6.25 ([#243](https://github.com/wvlet/airframe/issues/243)) [[fc91387](https://github.com/wvlet/airframe/commit/fc91387)]
- Change withoutLifeCycleLogging -> noLifeCycleLogging [[501b88b](https://github.com/wvlet/airframe/commit/501b88b)]

## 0.62
- Add unpackBytes for compatibility ([#240](https://github.com/wvlet/airframe/issues/240)) [[ecd8367](https://github.com/wvlet/airframe/commit/ecd8367)]
- Truncate to duration unit when the exact date is given to the offset ([#239](https://github.com/wvlet/airframe/issues/239)) [[68d497e](https://github.com/wvlet/airframe/commit/68d497e)]

## 0.61
- Support bind[Design] to reference the original design ([#237](https://github.com/wvlet/airframe/issues/237)) [[7d362c9](https://github.com/wvlet/airframe/commit/7d362c9)]

## 0.60
- Added Design.withoutLifeCycleLogging, Design.withProductionMode, Design.withLazyMode
  - [#233](https://github.com/wvlet/airframe/issues/233): Allow disabling LifeCycleLogger ([#234](https://github.com/wvlet/airframe/issues/234)) [[4d2c80a](https://github.com/wvlet/airframe/commit/4d2c80a)]
- Support Future[X] return type in http-finagle ([#230](https://github.com/wvlet/airframe/issues/230)) [[3642cc6](https://github.com/wvlet/airframe/commit/3642cc6)]
- Upgrade to scalafmt 1.5.1 ([#231](https://github.com/wvlet/airframe/issues/231)) [[da84852](https://github.com/wvlet/airframe/commit/da84852)]

## 0.59
- Do not escape forward slashes in JSONStrings ([#229](https://github.com/wvlet/airframe/issues/229)) [[66f77f6](https://github.com/wvlet/airframe/commit/66f77f6)]
- Fix a bug when reading None in OptionCodec ([#228](https://github.com/wvlet/airframe/issues/228)) [[cda0d36](https://github.com/wvlet/airframe/commit/cda0d36)]

## 0.58
- use canonical names for arguments of method surface on MethodCallBuilder ([#227](https://github.com/wvlet/airframe/issues/227)) [[33cad91](https://github.com/wvlet/airframe/commit/33cad91)]

## 0.57
- Move wvlet.config to wvlet.airframe.config ([#226](https://github.com/wvlet/airframe/issues/226)) [[e103a89](https://github.com/wvlet/airframe/commit/e103a89)]
- Allow setting object codec factory for generating natural JSON responses ([#224](https://github.com/wvlet/airframe/issues/224)) [[0b42960](https://github.com/wvlet/airframe/commit/0b42960)]
- Use private[this] to optimize json parsing ([#225](https://github.com/wvlet/airframe/issues/225)) [[8432af8](https://github.com/wvlet/airframe/commit/8432af8)]

## 0.56
- airframe-http-finagle enhancement ([#223](https://github.com/wvlet/airframe/issues/223)) [[6d8b361](https://github.com/wvlet/airframe/commit/6d8b361)]
- Support quarter (q) duration ([#220](https://github.com/wvlet/airframe/issues/220)) [[fa56875](https://github.com/wvlet/airframe/commit/fa56875)]

## 0.55
- airframe-http: Add rest mapping ([#207](https://github.com/wvlet/airframe/issues/207)) [[43bcb0a](https://github.com/wvlet/airframe/commit/43bcb0a)]
  - Support HTTPRequest binding, request body to custom class binding
- airframe-json performance optimization ([#214](https://github.com/wvlet/airframe/issues/214)) [[7b83bea](https://github.com/wvlet/airframe/commit/7b83bea)]
  - Optimize JSON object builder ([#216](https://github.com/wvlet/airframe/issues/216)) [[ac3ba86](https://github.com/wvlet/airframe/commit/ac3ba86)]
- Drop jdk9 build for time saving [[dc96c00](https://github.com/wvlet/airframe/commit/dc96c00)]

## 0.54
- Add airframe-json. A fast JSON parser for Scala and Scala.js. This also supports JSON pull parsing.
  - Optimize number parser ([#213](https://github.com/wvlet/airframe/issues/213)) ([fd89e62](https://github.com/wvlet/airframe/commit/fd89e62))
  - JSON parse performance improvement ([#212](https://github.com/wvlet/airframe/issues/212)) ([897ca38](https://github.com/wvlet/airframe/commit/897ca38))
  - Use airframe-json for JSONCodec ([#211](https://github.com/wvlet/airframe/issues/211)) ([61b9ba7](https://github.com/wvlet/airframe/commit/61b9ba7))
  - Add airframe-json ([#208](https://github.com/wvlet/airframe/issues/208)) ([6aab1fb](https://github.com/wvlet/airframe/commit/6aab1fb))
- Add release automation script ([eeb4d2b](https://github.com/wvlet/airframe/commit/eeb4d2b))

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
 - airframe-launcher: Fixes [#147](https://github.com/wvlet/airframe/issues/147) when reading the default values of nested options

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
 - Add [airframe-launcher](https://github.com/wvlet/airframe/tree/master/airframe-opts) command line parser.

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
