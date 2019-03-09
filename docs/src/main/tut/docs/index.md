---
layout: docs
title: Airframe Modules
---

## Airframe Modules

Airframe is available for Scala 2.13, 2.12, 2.11, and [Scala.js](https://www.scala-js.org/).
For Scala 2.12.6 or later, Java 11 is also supported.

- [airframe](docs/airframe.html)
  - Scala-friendly dependency injection library.
- [ariframe-canvas](docs/airframe-canvas.html)
  - Off-heap memory buffer
- [airframe-codec](docs/airframe-codec.html)
  - [MessagePack](https://msgpack.org) based Schema-on-Read data transcoder, which can be used for object serialization and deserialization. 
- [airframe-config](docs/airframe-config.html)
  - YAML-based configuration reader & provider.
- [airframe-control](docs/airframe-control.html)
  - Utilities for controlling code flows with loan pattern, retry logic, parallelization, etc.
- [airframe-fluentd](docs/airframe-fluentd.html)
  - MetricLogger for sending logs to [fluentd](https://www.fluentd.org) or [Treasure Data](https://www.treasuredata.com)
- [airframe-http](docs/airframe-http.html)
  - A light-weight HTTP server builder, based on Finagle
- [airframe-http-recorder](docs/airframe-http-recorder.html)
  - A handly HTTP recorder and replayer for HTTP server development
- [airframe-jdbc](docs/airframe-jdbc.html)
  - Reusable JDBC connection pool.
- [airframe-jmx](docs/airframe-jmx.html)
  - Enable runtime application monitoring through JMX.
- [airframe-json](docs/airframe-json.html)
  - Pure-Scala JSON parser.
- [airframe-log](docs/airframe-log.html)
  - Light-weight handy logging library.
- [airframe-launcher](docs/airframe-launcher.html)
  - Command line parser and launcher.
- [airframe-metrics](docs/airframe-metrics.html)
  - Human-readable representation of times, time ranges, and data sizes.
- [airframe-msgpack](docs/airframe-msgpack.html)
  - Pure-scala MessagePack reader and writer
- [airframe-surface](docs/airframe-surface.html)
  - Object shape inspector. What parameters are defined in an object? Surface gives you an answer for that. 
- [airframe-spec](docs/airframe-spec.html)
  - A simple base trait for using ScalaTest.
- [airframe-tablet](docs/airframe-tablet.html)
  - Table-structured data (e.g., CSV, TSV, JDBC ResultSet) reader/writer.

## Resources
- [Release Notes](docs/release-notes.html)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)
- [Use Cases](docs/use-cases.html)
   - Configuring applications, managing resources, service mix-in, etc.

## Usage

Airframe is a collection of Scala libraries. You can include one or more of them to your dependencies:

[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link] [![Scaladoc](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12)

**build.sbt**
```scala
val AIRFRAME_VERSION="(version)"

# For Scala 2.11, 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"               % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"         % AIRFRAME_VERSION, // MessagePack-based schema-on-read codec
  "org.wvlet.airframe" %% "airframe-config"        % AIRFRAME_VERSION, // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-control"       % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %% "airframe-http"          % AIRFRAME_VERSION, // HTTP REST API router
  "org.wvlet.airframe" %% "airframe-http-finagle"  % AIRFRAME_VERSION, // HTTP server (Finagle backend)
  "org.wvlet.airframe" %% "airframe-http-recorder" % AIRFRAME_VERSION, // HTTP recorder and replayer
  "org.wvlet.airframe" %% "airframe-jmx"           % AIRFRAME_VERSION, // JMX monitoring
  "org.wvlet.airframe" %% "airframe-jdbc"          % AIRFRAME_VERSION, // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-json"          % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %% "airframe-log"           % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %% "airframe-metrics"       % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %% "airframe-msgpack"       % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %% "airframe-launcher"      % AIRFRAME_VERSION, // Command-line program launcher
  "org.wvlet.airframe" %% "airframe-stream"        % AIRFRAME_VERSION, // Stream processing library
  "org.wvlet.airframe" %% "airframe-surface"       % AIRFRAME_VERSION, // Object surface inspector
  "org.wvlet.airframe" %% "airframe-tablet"        % AIRFRAME_VERSION  // Table data reader/writer
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"    % AIRFRAME_VERSION, // MessagePack-based schema-on-read transcoder
  "org.wvlet.airframe" %%% "airframe-json"    % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %%% "airframe-log"     % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %%% "airframe-metrics" % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %%% "airframe-surface" % AIRFRAME_VERSION  // Object surface inspector
)
```


## What's Next

Read how to use [Airframe DI](docs/airframe.html).

