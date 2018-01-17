---
layout: home
title:  "Home"
section: "home"
technologies:
 - first: ["Scala", "Airframe is completely written in Scala"]
 - second: ["SBT", "Airframe uses SBT and other sbt plugins to generate microsites easily"]
---
# Airframe

Airframe is a collection of [lightweight libraries](docs/util.html) useful for building full-fledged applications in Scala, including:

- [Logging](docs/airframe-log.html)
- [Configuration](docs/airframe-config.html)
- [Object serialization](docs/airframe-codec.html)
- [Option parser](docs/airframe-opts.html)
- [Runtime monitoring via JMX](docs/airframe-jmx.html)
- [Human-readable data units](docs/airframe-metrics.html)
- [Object shape inspector (Surface)](docs/airframe-surface.md)
- etc.

For advanced users we also provide [dependency injection (DI) library](docs) tailored to [Scala](https://www.scala-lang.org/). 
Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation; 
Instead of enumerating necessary objects (dependencies) within constructor arguments, DI framework builds objects on your behalf.
While Google's [Guice](https://github.com/google/guice) is designed for injecting Java objects (e.g., using class constructors or providers),
Airframe redesigned it for Scala to so that we can enjoy the flexibilities of Scala traits and DI at the same time.


## Resources
- [Documentation](docs)
- [Airframe Utilities](docs/utils.html)
   - A collection of useful Scala libraries that can be used with Airframe.
- [Use Cases](docs/use-cases.html)
   - Configuring applications, managing resources, service mix-in, etc.
- [DI Framework Comparison](docs/comparison.html)
   - Comparing Airframe with Google Guice, Macwire, Dagger2, etc. 
- [Source Code (GitHub)](https://github.com/wvlet/airframe)


## Getting Started
 [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)

Airframe is available for Scala 2.13, 2.12, 2.11, and [Scala.js](https://www.scala-js.org/):

- [Release Notes](docs/release-notes.html)

## Usage

**build.sbt**
```scala
# For Scala 2.11, 2.12, and 2.13
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %% "airframe"         % "(version)", // Dependency injection
  "org.wvlet.airframe" %% "airframe-codec"   % "(version)", // MessagePack-based schema-on-read transcoder
  "org.wvlet.airframe" %% "airframe-config"  % "(version)", // YAML-based configuration
  "org.wvlet.airframe" %% "airframe-jmx"     % "(version)", // JMX monitoring
  "org.wvlet.airframe" %% "airframe-jdbc"    % "(version)", // JDBC connection pool
  "org.wvlet.airframe" %% "airframe-log"     % "(version)", // Logging
  "org.wvlet.airframe" %% "airframe-metrics" % "(version)", // Metrics units
  "org.wvlet.airframe" %% "airframe-opts"    % "(version)", // Command-line option parser
  "org.wvlet.airframe" %% "airframe-surface" % "(version)", // Object surface inspector
  "org.wvlet.airframe" %% "airframe-tablet"  % "(version)"  // Table data reader/writer
)

# For Scala.js, the following libraries can be used:
libraryDependencies ++= Seq(
  "org.wvlet.airframe" %%% "airframe"         % "(version)", // Dependency injection
  "org.wvlet.airframe" %%% "airframe-log"     % "(version)", // Logging
  "org.wvlet.airframe" %%% "airframe-metrics" % "(version)", // Metrics units
  "org.wvlet.airframe" %%% "airframe-surface" % "(version)"  // Object surface inspector
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
    .bind[Z].to[ZImpl]          // Bind type Z to ZImpl instance
```

Then **build** an instance and use it:
```scala
val session = design.newSession
val app: App = session.build[App]
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
- Scala 2.11, 2.12, and [Scala.js](https://www.scala-js.org/) support.

## What's Next?

See [Documentation](docs) for further details.
