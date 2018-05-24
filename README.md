# Airframe  [![Gitter Chat][gitter-badge]][gitter-link] [![Build Status](https://travis-ci.org/wvlet/airframe.svg?branch=master)](https://travis-ci.org/wvlet/airframe) [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12) [![codecov](https://codecov.io/gh/wvlet/airframe/branch/master/graph/badge.svg)](https://codecov.io/gh/wvlet/airframe) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org) [![Scaladoc](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-scaladoc_2.12)

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/airframe?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=master

Airframe http://wvlet.org/airframe is a collection of lightweight libraries useful for building full-fledged applications in Scala.
As its core Airframe provides a dependency injection (DI) library tailored to Scala. While Google's [Guice](https://github.com/google/guice) is designed for injecting Java objects (e.g., using constructors or providers), Airframe redesigned it for Scala traits so that we can mix-in traits that have several object dependencies.

## Resources
- [Airframe Home](http://wvlet.org/airframe/)
- [Documentation](http://wvlet.org/airframe/docs)
- [Use Cases](http://wvlet.org/airframe/docs/use-cases.html)
   - Configuring applications, managing resources, service mix-in, etc.
- [DI Framework Comparison](http://wvlet.org/airframe/docs/comparison.html)
   - Comparing Airframe with Google Guice, Macwire, Dagger2, etc. 
- [Airframe Utilities](http://wvlet.org/airframe/docs/utils.html)
   - A collection of useful Scala libraries that can be used with Airframe.
- [Release Notes](http://wvlet.org/airframe/docs/release-notes.html)

## Getting Started
 [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12)

**build.sbt**

Airframe is a collection of Scala libraries. You can include one or more of them to your dependencies:
```scala
val AIRFRAME_VERSION = "(version)"

# For Scala 2.11, 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"           % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-bootstrap" % AIRFRAME_VERSION, // Application bootstrap helper
  "org.wvlet.airframe" %% "airframe-codec"     % AIRFRAME_VERSION, // MessagePack-based schema-on-read transcoder
  "org.wvlet.airframe" %% "airframe-config"    % AIRFRAME_VERSION, // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-jmx"       % AIRFRAME_VERSION, // JMX entry
  "org.wvlet.airframe" %% "airframe-jdbc"      % AIRFRAME_VERSION, // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-log"       % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %% "airframe-metrics"   % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %% "airframe-msgpack"   % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %% "airframe-opts"      % AIRFRAME_VERSION, // Command-line option parser
  "org.wvlet.airframe" %% "airframe-surface"   % AIRFRAME_VERSION, // Object surface inspector
  "org.wvlet.airframe" %% "airframe-tablet"    % AIRFRAME_VERSION  // Table data reader/writer
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %%% "airframe-log"     % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %%% "airframe-metrics" % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %%% "airframe-msgpack" % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %%% "airframe-surface" % AIRFRAME_VERSION  // Object surface inspector
)
```

See [Documentation](http://wvlet.org/airframe/docs/) for further details.

## LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)
