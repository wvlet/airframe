# Airframe [![Gitter Chat][gitter-badge]][gitter-link] [![Build Status](https://travis-ci.org/wvlet/airframe.svg?branch=master)](https://travis-ci.org/wvlet/airframe)  [![codecov](https://codecov.io/gh/wvlet/airframe/branch/master/graph/badge.svg)](https://codecov.io/gh/wvlet/airframe) [![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link] [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org) [![Scaladoc](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12)

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/airframe?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=master
[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

Airframe https://wvlet.org/airframe is a collection of lightweight building blocks for Scala.

See [Documentation](https://wvlet.org/airframe/) for further details.

## Resources
- [Airframe Home](https://wvlet.org/airframe/)
- [Documentation](https://wvlet.org/airframe/docs)
- [Release Notes](https://wvlet.org/airframe/docs/release-notes.html)

## Getting Started
[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link]

**build.sbt**

Airframe is a collection of Scala libraries. You can include one or more of them to your dependencies:
```scala
val AIRFRAME_VERSION = "(version)"

# For Scala 2.11, 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"           % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"     % AIRFRAME_VERSION, // MessagePack-based schema-on-read transcoder
  "org.wvlet.airframe" %% "airframe-config"    % AIRFRAME_VERSION, // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-control"   % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %% "airframe-http"      % AIRFRAME_VERSION, // HTTP REST API router
  "org.wvlet.airframe" %% "airframe-jmx"       % AIRFRAME_VERSION, // JMX entry
  "org.wvlet.airframe" %% "airframe-jdbc"      % AIRFRAME_VERSION, // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-json"      % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %% "airframe-log"       % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %% "airframe-metrics"   % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %% "airframe-msgpack"   % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %% "airframe-opts"      % AIRFRAME_VERSION, // Command-line option parser
  "org.wvlet.airframe" %% "airframe-stream"    % AIRFRAME_VERSION, // Stream processing library
  "org.wvlet.airframe" %% "airframe-surface"   % AIRFRAME_VERSION, // Object surface inspector
  "org.wvlet.airframe" %% "airframe-tablet"    % AIRFRAME_VERSION  // Table data reader/writer
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %%% "airframe-control" % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %%% "airframe-json"    % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %%% "airframe-log"     % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %%% "airframe-metrics" % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %%% "airframe-msgpack" % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %%% "airframe-surface" % AIRFRAME_VERSION  // Object surface inspector
)
```

## LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)
