---
layout: home
section: "home"
title: Lightweight Building Blocks for Scala
technologies:
 - first: ["Scala", "Airframe is completely written in Scala"]
 - second: ["SBT", "Airframe uses SBT and other sbt plugins to generate microsites easily"]
---
# Airframe

Airframe is a collection of lightweight libraries useful for building full-fledged applications in Scala.

- [Logging](docs/airframe-log.html)
- [Configuration](docs/airframe-config.html)
- [Dependency Injection (DI)](docs/index.html) tailored to to [Scala](https://www.scala-lang.org/)
  - Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation;
    Instead of enumerating necessary objects (dependencies) within constructor arguments, DI framework builds objects on your behalf.
    While Google's [Guice](https://github.com/google/guice) is designed for injecting Java objects (e.g., using class constructors or providers),
    Airframe redesigned it for Scala to so that we can enjoy the flexibilities of Scala traits and DI at the same time.
  - [DI Framework Comparison](https://wvlet.org/airframe/docs/comparison.html). Comparing Airframe with Google Guice, Macwire, Dagger2, etc.
- [Fluentd Client](docs/airframe-fluentd.html)
- [HTTP Server](docs/airframe-http.html)
- [Object serialization](docs/airframe-codec.html)
- [Option parser](docs/airframe-opts.html)
- [Runtime monitoring via JMX](docs/airframe-jmx.html)
- [Human-readable data and time units](docs/airframe-metrics.html)
- [Object shape inspector (Surface)](docs/airframe-surface.html)
- [Other Utilities](https://wvlet.org/airframe/docs/utils.html)
   - A collection of useful Scala libraries that can be used with Airframe.

## Resources
- [Documentation](docs)
- [Airframe Utilities](docs/utils.html)
   - A collection of useful Scala libraries that can be used with Airframe.
- [Use Cases](docs/use-cases.html)
   - Configuring applications, managing resources, service mix-in, etc.
- [Source Code (GitHub)](https://github.com/wvlet/airframe)

## Blog Articles

- [Airframe HTTP: A Minimalist Approach For Building Web Services in Scala](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)
  - airframe-http, airframe-http-finagle
- [Demystifying Dependency Injection with Airframe](https://medium.com/@taroleo/demystifying-dependency-injection-with-airframe-9b637034a78a)
  - airframe dependency injection
- [Airframe Log: A Modern Logging Library for Scala](https://medium.com/@taroleo/airframe-log-a-modern-logging-library-for-scala-56fbc2f950bc)
  - airframe-log
- [3 Tips For Maintaining Your Scala Projects](https://medium.com/@taroleo/3-tips-for-maintaining-your-scala-projects-e54a2feea9c4)
  - Tips on how we are maintaining Airframe.
- [Introdution of Airframe in Japanese (日本語)](https://medium.com/@taroleo/airframe-c5d044a97ec)

## Getting Started

[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link]

Airframe is available for Scala 2.13, 2.12, 2.11, and [Scala.js](https://www.scala-js.org/).
For Scala 2.12.6 or later, Java 9 and 10 are also supported.

- [Release Notes](docs/release-notes.html)

## Usage

Airframe is a collection of Scala libraries. You can include one or more of them to your dependencies:

**build.sbt**
```scala
val AIRFRAME_VERSION="(version)"

# For Scala 2.11, 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"              % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"        % AIRFRAME_VERSION, // MessagePack-based schema-on-read transcoder
  "org.wvlet.airframe" %% "airframe-config"       % AIRFRAME_VERSION, // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-control"      % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %% "airframe-http"         % AIRFRAME_VERSION, // HTTP REST API router
  "org.wvlet.airframe" %% "airframe-http-finagle" % AIRFRAME_VERSION, // HTTP server (Finagle backend)
  "org.wvlet.airframe" %% "airframe-jmx"          % AIRFRAME_VERSION, // JMX monitoring
  "org.wvlet.airframe" %% "airframe-jdbc"         % AIRFRAME_VERSION, // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-json"         % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %% "airframe-log"          % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %% "airframe-metrics"      % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %% "airframe-msgpack"      % AIRFRAME_VERSION, // Pure-Scala MessagePack
  "org.wvlet.airframe" %% "airframe-opts"         % AIRFRAME_VERSION, // Command-line option parser
  "org.wvlet.airframe" %% "airframe-stream"       % AIRFRAME_VERSION, // Stream processing library
  "org.wvlet.airframe" %% "airframe-surface"      % AIRFRAME_VERSION, // Object surface inspector
  "org.wvlet.airframe" %% "airframe-tablet"       % AIRFRAME_VERSION  // Table data reader/writer
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % AIRFRAME_VERSION, // Dependency injection
  "org.wvlet.airframe" %%% "airframe-control" % AIRFRAME_VERSION, // Library for retryable execution
  "org.wvlet.airframe" %%% "airframe-json"    % AIRFRAME_VERSION, // Pure Scala JSON parser
  "org.wvlet.airframe" %%% "airframe-log"     % AIRFRAME_VERSION, // Logging
  "org.wvlet.airframe" %%% "airframe-metrics" % AIRFRAME_VERSION, // Metrics units
  "org.wvlet.airframe" %%% "airframe-surface" % AIRFRAME_VERSION  // Object surface inspector
)
```

### Dependency Injection with Airframe

With Airframe, your Scala programming can be greatly simplified:

First, **bind** objects to your code with `bind[X]`:
```scala
import wvlet.airframe._

trait App {
  val x = bind[X]
  val y = bind[Y]
  val z = bind[Z]
  // Do something with x, y, and z
}
```

Next, **design** the object bindings:
```scala
val design: Design =
  newDesign
    .bind[X].toInstance(new X)  // Bind type X to a concrete instance
    .bind[Y].toSingleton        // Bind type Y to a singleton object
    .bind[Z].to[ZImpl]          // Bind type Z to a singletone of ZImpl instance
```

Then **build** an instance and use it:
```scala
design.build[App]{ app =>
  // Do something with App
}
```

Airframe builds an instance of `App` based on the binding rules specified in the *design* object. That means when writing applications, you only need to care about how to use objects (*bind*), rather than how to build them, because design objects already knows how to provide necessary objects to build your classes.

This separation of object bindings and their design (assembly) is useful for reducing code duplications between production and test codes. For example, compare writing `new App(new X, new Y(...), new Z(...), ...)` in both of your main and test codes, and just calling `session.build[App]`.
Airframe can integrate the flexibility of Scala traits and dependency injection (DI). Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.

## Airframe DI Features

Major features of Airframe DI are as follows:

- Simple usage
  - Just `import wvlet.airframe._` and do the above three steps to start DI in Scala.
- Airframe can create objects from Scala traits.
- Design objects are immutable. You can create a new design safely based on an existing design.
- Supports all possible binding types: constructor, instance, provider, singleton bindings.
- Built-in life cycle management of objects (init, shutdown, etc.) through sessions.
- Scala macro based binding generation, which helps binding objects to your code.
- Scala 2.11, 2.12, 2.13, and [Scala.js](https://www.scala-js.org/) support.
- Java9/10 support.

## What's Next?

See [Documentation](docs) for further details.
