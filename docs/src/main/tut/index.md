---
layout: home
title:  "Home"
section: "home"
technologies:
 - first: ["Scala", "Airframe is completely written in Scala"]
 - second: ["SBT", "Airframe uses SBT and other sbt plugins to generate microsites easily"]
---
# Airframe

Airframe is a [dependency injection (DI)](https://en.wikipedia.org/wiki/Dependency_injection) library tailored to [Scala](https://www.scala-lang.org/). Dependency injection is a design pattern for simplifying object instantiation. It removes the burden of enumerating necessary classes (dependencies) by hand when calling object constructors. While Google's [Guice](https://github.com/google/guice) is designed for injecting Java objects (e.g., using class constructors or providers), Airframe redesigned it for Scala so that we can enjoy the flexibilities of **Scala traits** and **DI** at the same time.

- [Documentation](docs)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)

## Getting Started

Airframe is available for Scala 2.12, 2.11, and [Scala.js](https://www.scala-js.org/):

**build.sbt** [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)
```
libraryDependencies += "org.wvlet" %% "airframe" % "(version)"
```

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

Airframe builds an instance of `App` based on the binding rules specified in *Design* object. When writing applications, you only need to care about how to use objects (**bind**). To configure applications, you can **design** object bindings at your startup code. Then **build** application instances configured accordingly. 

## Features

- Simple to use. Just `import wvlet.airframe._` and do the above three steps to enjoy DI in Scala.
- Design remembers how to build complex objects on your behalf.
  - For example, you can avoid code duplications in your test and production codes. Compare writing `new App(new X, new Y(...), new Z(...), ...)` every time and just calling `session.build[App]`.
  - When writing application codes, you only need to care about how to use objects, rather than how to provide them. Design already knows how to provide objects to your class.
- You can enjoy the flexibility of Scala traits and dependency injection (DI) at the same time.
    - Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.
- Built-in life cycle management of objects (init, shutdown, etc.) through sessions.
- Scala macro based binding generation.
- Scala 2.11, 2.12, [Scala.js](https://www.scala-js.org/) support.


See [Documentation](docs) for details.
