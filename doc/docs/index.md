---
id: index
layout: docs
title: Overview
---

<img style = "float: right; padding: 10px;" width="150px" src="img/logos/airframe_icon_small.png"/>

Airframe is a collection of essential building blocks for writing full-fledged applications in Scala and Scala.js.

- [Release Notes](release-notes.md)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)
- [Presentations and Articles](articles.md)

## Usage Guides

Scala is a quite powerful programming language. One of the benefits of Scala is it can utilize libraries in Java ecosystem. Existing libraries for Java, however, are not always the best choices if you are primarily writing code in Scala. For example, some libraries have performance overhead for converting Java collections into Scala collections, and their interfaces might not be well-suited to using them from Scala, etc.

Airframe has several modules that can replace commonly-used Java libraries to provide the same functionality in Scala. For example, you may have used libraries like slf4j, Google Guice, Jackson, etc. If you are familiar with these libraries, you will be surprised how Airframe can simplify your code compared to using these libraries designed for Java.

In the following sections, we will see several examples of Airframe modules that will enrich your application development experience in Scala.

### Logging

For adding application logging, use [airframe-log](airframe-log.md).

slf4j and log4j are commonly used logging libraries in Java, but they are not fully utilizing the strength of Scala for enhancing log messages. [airframe-log](airframe-log.md) is a new logging library designed for Scala, which is programatically configurable and supports showing the source code locations. Seeing the line number where the debug message is produced will significantly save your time for debugging your applications.

To start logging with airframe-log, just extend `wvlet.log.LogSupport` and use `trace/debug/info/warn/error` logging methods. airframe-log uses Scala Macros to remove the performance overhead for generating debug log messages unless you set `Logger.setDefaultLogLevel(LogLevel.DEBUG)`:

```scala
import wvlet.log.LogSupport

class MyApp extends LogSupport {
  info("Hello airframe-log!")
  
  // If the log level is INFO, this will produce no message and has no performance overhead.
  debug("debug log message")
}
```

For more background, see also: [Airframe Log: A Modern Logging Library for Scala](https://medium.com/airframe/airframe-log-a-modern-logging-library-for-scala-56fbc2f950bc) 

### Object Serialization 

If you need to store object data to disks, or send them to remote machines (e.g., Spark applications), use [airframe-codec](airframe-codec.md), which is a [MessagePack](https://msgpack.org)-based schema-on-read data serialization library.

[Jackson](https://github.com/FasterXML/jackson) is a JSON-based data serialization library and supports mapping between JSON and classes. To control the mapping to objects, you need to add `@JSONProperty` annotation and configure ObjectMapper.

[airframe-codec](airframe-codec.md) simplifies this process so that you can use case classes in Scala without any annotations. For producing compact binaries of your data, it also supports [MessagePack](https://msgpack.org) format as well as JSON. 

```scala
case class Person(id:Int, name:String)

// Create a codec for serializing your model classes
val codec = MessageCodec.of[Person]

// Serialize in JSON or MessagePack
val a = Person(1, "Ann")
val json = codec.toJson()       // {"id":1,"name":"Ann"}
val msgpack = codec.toMsgPack() // MessagePack ArrayValue: [1,"name"]

// Deserialize from JSON or MessagePack
codec.fromJson(json)       // Person(1, "Ann")
codec.fromMsgPack(msgpack) // Person(1, "Ann")
```

#### Schema-On-Read Conversion

[airframe-codec](airframe-codec.md) adjusts input data types according to the target object types.
This schema-on-read data conversion is quite powerful for mapping various types of input data (e.g., CSV, JSON, etc.) into Scala case classes.

```scala
val json = """{"id":"2", "name":"Bob"}"""

// "2" (String) value will be converted to 2 (Int)   
codec.fromJson(json) // Person(2, "Bob") 
```

![schema](img/airframe-codec/schema-on-read.png)

### Querying JSON and MessagePack Data 

[airframe-codec](airframe-codec.md) can be used for extracting data from JSON and MessagePack data. For example, if you have the following JSON data:

```json
[
  {"id":1, "name":"xxx", "address":["aaa", "bbb", ...]},
  {"id":2, "name":"yyy", "address":["ccc", "ddd", ...]}
]
```

You can extract only the names and the addresses from this JSON as follows: 

```scala
case class AddressQuery(name:String, address:Seq[String])

MessageCodec.of[Seq[AddressQuery]].fromJson(json)
// This extracts:
//   Seq(AddressQuery("xxx", Seq("aaa","bbb")), AddressQuery("yyy", Seq["ccc","ddd"]))
``` 

### Building Web Servers and Clients




### Dependency Injection


- [DI Library Comparison](comparison.md)

### Google Guava



### Retry and Rate Control


### Command-Line Parser






## build.sbt

Airframe is a collection of essential libraries. Add necessary modules for your applications to your `libraryDependencies` setting in __build.sbt__ file.

[![maven central](https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22
) [![airframe](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
)](https://index.scala-lang.org/wvlet/airframe) 


**build.sbt**
```scala
val AIRFRAME_VERSION="(version)"

# For Scala 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"               % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"         % AIRFRAME_VERSION, // MessagePack-based schema-on-read codec
  "org.wvlet.airframe" %% "airframe-config"        % AIRFRAME_VERSION, // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-control"       % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %% "airframe-http"          % AIRFRAME_VERSION, // HTTP REST API
  "org.wvlet.airframe" %% "airframe-http-finagle"  % AIRFRAME_VERSION, // HTTP server (Finagle backend)
  "org.wvlet.airframe" %% "airframe-http-recorder" % AIRFRAME_VERSION, // HTTP recorder and replayer
  "org.wvlet.airframe" %% "airframe-jmx"           % AIRFRAME_VERSION, // JMX monitoring
  "org.wvlet.airframe" %% "airframe-jdbc"          % AIRFRAME_VERSION, // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-json"          % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %% "airframe-launcher"      % AIRFRAME_VERSION, // Command-line program launcher
  "org.wvlet.airframe" %% "airframe-log"           % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %% "airframe-metrics"       % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %% "airframe-msgpack"       % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %% "airframe-surface"       % AIRFRAME_VERSION, // Object surface inspector
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %%% "airframe-codec"   % AIRFRAME_VERSION, // MessagePack-based schema-on-read codec
  "org.wvlet.airframe" %%% "airframe-json"    % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %%% "airframe-log"     % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %%% "airframe-msgpack" % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %%% "airframe-metrics" % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %%% "airframe-surface" % AIRFRAME_VERSION, // Object surface inspector
)
```


## Airframe Modules

Airframe has several modules for kick starting your application development in Scala.


- [airframe](airframe-di.md)
  - Scala-friendly dependency injection library.
- [airspec](airspec.md)
  - A functional testing framework for Scala.
- [ariframe-canvas](airframe-canvas.md)
  - Off-heap memory buffer
- [airframe-config](airframe-config.md)
  - YAML-based configuration reader & provider.
- [airframe-control](airframe-control.md)
  - Utilities for controlling code flows with loan pattern, retry logic, parallelization, etc.
- [airframe-fluentd](airframe-fluentd.md)
  - MetricLogger for sending logs to [fluentd](https://www.fluentd.org) or [Treasure Data](https://www.treasuredata.com)
- [airframe-http](airframe-http.md)
  - A light-weight HTTP server builder, based on Finagle
- [airframe-http-recorder](airframe-http-recorder.md)
  - A handly HTTP recorder and replayer for HTTP server development
- [airframe-jdbc](airframe-jdbc.md)
  - Reusable JDBC connection pool.
- [airframe-jmx](airframe-jmx.md)
  - Enable runtime application monitoring through JMX.
- [airframe-json](airframe-json.md)
  - Pure-Scala JSON parser.
- [airframe-launcher](airframe-launcher.md)
  - Command line parser and launcher.
- [airframe-metrics](airframe-metrics.md)
  - Human-readable representation of times, time ranges, and data sizes.
- [airframe-msgpack](airframe-msgpack.md)
  - Pure-scala MessagePack reader and writer
- [airframe-surface](airframe-surface.md)
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](airspec.md)
  - A simple base trait for using ScalaTest.
- [airframe-sql](airframe-sql.md)
  - SQL parser

### Companion sbt plugins

We also have developed sbt plugins for packaging and publishing your projects:

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating a distributable package or [docker image](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack)
  of your program.

- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.

