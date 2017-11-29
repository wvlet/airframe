# Airframe  [![Gitter Chat][gitter-badge]][gitter-link] [![Build Status](https://travis-ci.org/wvlet/airframe.svg?branch=master)](https://travis-ci.org/wvlet/airframe) [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe_2.12) [![codecov](https://codecov.io/gh/wvlet/airframe/branch/master/graph/badge.svg)](https://codecov.io/gh/wvlet/airframe) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org)

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
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
```
# For Scala 2.11, 2.12, and 2.13
libraryDependencies += "org.wvlet.airframe" %% "airframe" % "(version)"

# For Scala.js (supported since airframe 0.12)
libraryDependencies += "org.wvlet.airframe" %%% "airframe" % "(version)"
```

See [Documentation](http://wvlet.org/airframe/docs/) for further details.

## Airframe Library Source Codes
- [airframe](https://github.com/wvlet/airframe/tree/master/airframe)
  - Dependency injection library
- [airframe-log](https://github.com/wvlet/airframe/tree/master/log)
  - Light-weight handy logging library
- [airframe-config](https://github.com/wvlet/airframe/tree/master/config) 
  - Configuration reader & provider
- [airframe-opts](https://github.com/wvlet/airframe/tree/master/opts) 
  - Command-line parser & launcher
- [airframe-metrics](https://github.com/wvlet/airframe/tree/master/metrics)
  - Human-readable representation of time, time range, and data size.
- [airframe-jmx](https://github.com/wvlet/airframe/tree/master/jmx)
  - Enable application monitoring through JMX
- [airframe-surface](https://github.com/wvlet/airframe/tree/master/surface)
  - Object shape inspector. What parameters are defined in an object? 
- [airframe-spec](https://github.com/wvlet/airframe/blob/master/spec/shared/src/main/scala/wvlet/airframe/AirframeSpec.scala) 
  - A simple base trait for using ScalaTest.

## LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)
