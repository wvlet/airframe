---
id: index
layout: docs
title: Overview
---

Airframe is a collection of essential building blocks for writing full-fledged applications in Scala and Scala.js.

- [Release Notes](release-notes.md)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)
- [Articles](articles.md)


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
- [airspec](airspec.html)
  - A functional testing framework for Scala.
- [ariframe-canvas](airframe-canvas.html)
  - Off-heap memory buffer
- [airframe-codec](airframe-codec.html)
  - [MessagePack](https://msgpack.org) based Schema-on-Read data transcoder, which can be used for object serialization and deserialization. 
- [airframe-config](airframe-config.html)
  - YAML-based configuration reader & provider.
- [airframe-control](airframe-control.html)
  - Utilities for controlling code flows with loan pattern, retry logic, parallelization, etc.
- [airframe-fluentd](airframe-fluentd.html)
  - MetricLogger for sending logs to [fluentd](https://www.fluentd.org) or [Treasure Data](https://www.treasuredata.com)
- [airframe-http](airframe-http.html)
  - A light-weight HTTP server builder, based on Finagle
- [airframe-http-recorder](airframe-http-recorder.html)
  - A handly HTTP recorder and replayer for HTTP server development
- [airframe-jdbc](airframe-jdbc.html)
  - Reusable JDBC connection pool.
- [airframe-jmx](airframe-jmx.html)
  - Enable runtime application monitoring through JMX.
- [airframe-json](airframe-json.html)
  - Pure-Scala JSON parser.
- [airframe-log](airframe-log.html)
  - Light-weight handy logging library.
- [airframe-launcher](airframe-launcher.html)
  - Command line parser and launcher.
- [airframe-metrics](airframe-metrics.html)
  - Human-readable representation of times, time ranges, and data sizes.
- [airframe-msgpack](airframe-msgpack.html)
  - Pure-scala MessagePack reader and writer
- [airframe-surface](airframe-surface.html)
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](airframe-spec.html)
  - A simple base trait for using ScalaTest.
- [airframe-sql](airframe-sql.html)
  - SQL parser


We also have developed sbt plugins for packaging and publishing your projects:

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating a distributable package or [docker image](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack)
  of your program.

- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.


