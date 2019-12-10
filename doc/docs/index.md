---
id: index
layout: docs
title: Overview
---

<img style = "float: right; padding: 10px;" width="150px" src="img/logos/airframe_icon_small.png"/>

Airframe is a collection of essential building blocks for writing full-fledged applications in Scala and Scala.js.

- [Release Notes](release-notes.md)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)
- [Articles](articles.md)


## Guide for Java Library Users

Airframe has several modules that can be a better replacement of commonly used Java libraries (e.g., Guice, Jackson, etc.) to provide the same functionality in Scala.    

### Logging

- [airframe-log](airframe-log.md)

slf4j and log4j are commonly used logging libraries in Java ecosystem, but they are not fully utilizing the strength of Scala for enhancing log messages. 
[airframe-log](airframe-log.md) is a new logging library designed for Scala, which can be programatically configured and supports logging source code locations, etc.

See also the blog article for more background: [Airframe Log: A Modern Logging Library for Scala](https://medium.com/airframe/airframe-log-a-modern-logging-library-for-scala-56fbc2f950bc) 

### Object Serialization 

[Jackson](https://github.com/FasterXML/jackson) is a JSON-based data serialization library and supports mapping between data objects. To configure mapping data objects, you need to add `@JSONProperty` annotation and configure ObjectMapper.



- [airframe-codec](airframe-codec.md)
  - [MessagePack](https://msgpack.org) based Schema-on-Read data transcoder, which can be used for object serialization and deserialization. 
- [airframe-json](airframe-json.md)
  - Pure-Scala JSON parser.
- [airframe-msgpack](airframe-msgpack.md)
  - Pure-scala MessagePack reader and writer



### Dependency Injection


- [DI Library Comparison](comparison.md)

### Google Guava







## Usage

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
- [airframe-launcher](airframe-launcher.md)
  - Command line parser and launcher.
- [airframe-metrics](airframe-metrics.md)
  - Human-readable representation of times, time ranges, and data sizes.
- [airframe-surface](airframe-surface.md)
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](airframe-spec.md)
  - A simple base trait for using ScalaTest.
- [airframe-sql](airframe-sql.md)
  - SQL parser


We also have developed sbt plugins for packaging and publishing your projects:

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating a distributable package or [docker image](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack)
  of your program.

- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.


