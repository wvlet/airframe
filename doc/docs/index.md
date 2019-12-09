---
title: Lightweight Building Blocks for Scala
---
# Airframe

Airframe is a collection of [lightweight building blocks](docs/index.html) for kick starting your Scala application development.

- [Documentation](docs)
- [Release Notes](https://wvlet.org/airframe/docs/release-notes.html)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)

- [AirSpec: A Functional Testing Library](https://wvlet.org/airframe/docs/airspec.html)

Airframe has several modules for kick starting your application development in Scala.

<center>
<p><img src="https://github.com/wvlet/airframe/raw/master/logos/airframe-overview.png" alt="logo" width="800px"></p>
</center>

- [airframe](airframe.html)
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


## Usage

Include as many dependencies as you need into your `libraryDependencies` in __build.sbt__ file.

[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link] [![Scaladoc](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12)

- [Release Notes](release-notes.html)

**build.sbt**
```scala
val AIRFRAME_VERSION="(version)"

# For Scala 2.12, and 2.13
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



## Blog Articles
- [AirSpec: AirSpec: Writing Tests As Plain Functions In Scala](https://medium.com/airframe/airspec-bbc8d4369157)
- [Airframe HTTP: Building Low-Friction Web Services Over Finagle](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)
  - airframe-http, airframe-http-finagle
- [Demystifying Dependency Injection with Airframe](https://medium.com/@taroleo/demystifying-dependency-injection-with-airframe-9b637034a78a)
  - airframe dependency injection
- [Airframe Log: A Modern Logging Library for Scala](https://medium.com/@taroleo/airframe-log-a-modern-logging-library-for-scala-56fbc2f950bc)
  - airframe-log
- [3 Tips For Maintaining Your Scala Projects](https://medium.com/@taroleo/3-tips-for-maintaining-your-scala-projects-e54a2feea9c4)
  - Tips on how we are maintaining Airframe.

### Blog Articles In Japanese
- [Airframe Meetup #1: Scala開発に役立つ5つのデザインパターンを紹介](https://medium.com/airframe/airframe-meetup-72d6db13182e)
- [AirframeによるScalaプログラミング：「何ができるか」から「何を効果的に忘れられるか」を考える](https://medium.com/airframe/e9e0f7fc983a)
- [Introdution of Airframe in Japanese](https://medium.com/@taroleo/airframe-c5d044a97ec)

## Presentations
- [Airframe Meetup #3 2019-10-23](https://www.slideshare.net/taroleo/airframe-meetup-3-2019-updates-airspec)
- [Airframe Meetup #2 2019-07-09](https://www.slideshare.net/taroleo/airframe-http-airframe-meetup-2-tokyo-20190709)
- [How to Use Scala At Work - Airframe In Action At Arm Treasure Data](https://www.slideshare.net/taroleo/how-to-use-scala-at-work-airframe-in-action-at-arm-treasure-data). Presentation at Scala Matsuri 2019
- [Airframe Meetup #1. 2018-10-23 @ Arm Treasure Data (Tokyo Office)](https://www.slideshare.net/taroleo/airframe-meetup-1-20181023-arm-treasure-data-tokyo-office)
- [Airframe: Lightweight Building-Blocks for Scala @ TD Tech Talk at Tokyo, 2018](https://www.slideshare.net/taroleo/airframe-lightweight-building-blocks-for-scala-td-tech-talk-20181014)
