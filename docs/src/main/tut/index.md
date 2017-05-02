---
layout: home
title:  "Home"
section: "home"
technologies:
 - first: ["Scala", "Airframe is completely written in Scala"]
 - second: ["SBT", "Airframe uses SBT and other sbt plugins to generate microsites easily"]
---
# Airframe

Airframe is a [dependency injection (DI)](https://en.wikipedia.org/wiki/Dependency_injection) library tailored to [Scala](https://www.scala-lang.org/). Dependency injection is a design pattern for simplifying object instantiation; Instead of enumerating necessary object instances (dependencies) to constructor arguments, DI framework builds objects on user's behalf. While Google's [Guice](https://github.com/google/guice) is designed for injecting Java objects (e.g., using class constructors or providers), Airframe redesigned it for Scala so that we can enjoy the flexibilities of Scala traits and DI at the same time.

- [Documentation](docs)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)

## Getting Started
 [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)

Airframe is available for Scala 2.12, 2.11, and [Scala.js](https://www.scala-js.org/):


**build.sbt**
```
libraryDependencies += "org.wvlet" %% "airframe" % "(version)"
```

### Bind, Design, then Build!

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

Airframe builds an instance of `App` based on the binding rules specified in the **design** object. That means when writing applications, you only need to care about how to use objects (**bind**), rather than how to build them. **Design** objects already knows how to provide necessary objects to build your classes.

## Features

The major features of Airframe include:

- Simple usage
  - Just `import wvlet.airframe._` and do the above three steps to enjoy DI in Scala.
- Useful for reducing code duplications between production and test codes.        
  - Compare writing `new App(new X, new Y(...), new Z(...), ...)` and just calling `session.build[App]`.
- Airframe can integrate the flexibility of Scala traits and dependency injection (DI) at the same time.
  - Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.
- Built-in life cycle management of objects (init, shutdown, etc.) through sessions.
- Scala macro based binding generation.
- Scala 2.11, 2.12, [Scala.js](https://www.scala-js.org/) support.
  - No reflection is used.

## What's Next?

See [Documentation](docs) for further details.
