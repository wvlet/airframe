---
layout: docs
title: Quick Start
---

# Quick Start

This page describes all you need to know to start using dependency injection with Airframe.

## sbt
[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link]

To use Airframe, add the following to your **build.sbt**:
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe" % "(version)"
```

And import `wvlet.airframe._` in your Scala code:
```scala
import wvlet.airframe._
```

### .scalafmt.conf
If you are using [scalafmt](https://scalameta.org/scalafmt/) for code formatting, add the following option to your `.scalafmt.conf`:
```
optIn.breaksInsideChains = true
```

This option enables writing each binding in a single line:
```scala
val d = newDesign
  .bind[X].toInstance(...)
  .bind[Y].to[YImpl]
```


## Bind

In Airframe, you can use two types of dependency injections: __constructor injection__ or
__in-trait injection__.
 
### Constructor Injection
Constructor injection is the most natural form of injection.
When `session.build[A]` is called, Airframe will find the primary constructor of `A` and 
its arguments, then creates a new instance of `A` by finding dependencies from a _Design_.

```scala
import wvlet.airframe._

class MyApp(val config:AppConfig)
case class AppConfig(appName:String)

// Define a design
val d = newDesign
  .bind[AppConfig].toInstance(AppConfig("Hello Airframe!"))

// Create MyApp. AppConfig instance defined in the design will be used.
// d.build[MyApp] will call new MyApp(AppConfig("Hello Airframe!")) to build a MyApp instance
d.build[MyApp]{ app: MyApp => 
  // Do something with app
  ...
}
// Session will be closed here
```

### In-Trait Injection
In-trait injection with `bind[X]` is useful to create reusable moduels. Note that this only works inside Scala traits:
```scala
import wvlet.airframe._

class Database(name:String, conn:Connection)
trait DatabaseService {
  val db = bind[Database]
}

val d = newDesign
  .bind[Connection].to[ConnectionImpl]

  // Creates a new DatabaseService with ConnectionImpl
d.build[DatabaseService] { db: DatabaseService =>
   ...
}

// [DON'T DO THIS] You can't use bind[X] inside classes:
class A {
  val a = bind[B] // [Error] Because class A can't find the current session
}
```
The following examples show basic binding types available in Airframe:
```scala
val a = bind[A]          // Inject A as a singleton
val b = bindInstance[B]  // Inject an instance of B

import BindingExample._

// Constructor binding
val pc: P = bind[P] // Inject a sigleton of P
                    // (Inject D1, D2 and D3)

// Provider bindings
val p0: P = bind { P() } // Inject P using the provider function (closure)
val p1: P = bind { d1:D1 => P(d1) } // Inject D1 to create P
val p2: P = bind { (d1:D1, d2:D2) => P(d1, d2) } // Inject D1 and D2 to create P
val p3: P = bind { (d1:D1, d2:D2, d3:D3) => P(d1, d2, d3) } // Inject D1, D2 and D3
val pd: P = bind { provider _ } // Inject D1, D2 and D3 to call a provider function
val pd: P = bindInstance { provider _ } // Create a new P using the provider

// Factory bindings can be used to override a part of the dependencies
val f1: D1 => P = bindFactory[D1 => P] // A factory to use a given D1 to generate P
val f2: (D1, D2) => P = bindFactory2[(D1, D2) => P] // A factory to use given D1 and D2
...

object BindingExample {
  case class P(d1:D1 = D1(), d2:D2 = D2(), d3:D3 = D3())
  def provider(d1:D1, d2:D2, d3:D3) : P = P(d1, d2, d3)
}
```

By default all injections generates singleton objects,
which are available until the session closes.

If you need to create a new instance for each binding, use `bindInstance[X]` or `bindFactory[I => X]`.

## Design

To configure actual bindings, define object bindings using design:

```scala
import wvlet.airframe._

// If you define multiple bindings to the same type, the last one will be used.
val design: Design =
  newDesign                      // Create an empty design
  .bind[A].to[AImpl]             // Bind a class AImpl to A (Singleton)
  .bind[A].toInstanceOf[AImpl]   // Bind a class AImpl to A (Create a new instance each time)
  .bind[B].toInstance(new B(1))  // Bind a concrete instance to B (This instance will be a singleton)
  .bind[S].toSingleton           // S will be a singleton within the session
  .bind[ES].toEagerSingleton     // ES will be initialized as a singleton at session start time
  .bind[D1].toInstance(D1(1))    // Bind D1 to a concrete instance D1(1)
  .bind[D2].toInstance(D2(2))    // Bind D2 to a concrete instance D2(2)
  .bind[D3].toInstance(D3(3))    // Bind D3 to a cocreete instance D3(3)
  .bind[P].toProvider{ d1:D1 => P(d1) } // Create a singleton P by resolveing D1 from the design
  .bind[P].toProvider{ (d1:D1, d2:D2) => P(d1, d2) }  // Resolve D1 and D2
  .bind[P].toProvider{ provider _ }                   // Use the given function as a provider
  .bind[P].toInstanceProvider{ d1:D1 => P(d1) }       // Create a new instance using the provider function
  .bind[P].toEagerSingletonProvider{ d1:D1 => P(d1) } // Create an eager singleton using the provider function
```

If you define multiple bindings to the same type (e.g., P), the last binding will be used. 

Design objects are immutable, so you can safely override bindings without modifying the original design:
```scala
val design: Design =
  newDesign.bind[A].to[B] // bind A to B

val newDesign: Design =
  design.bind[A].to[C] // Override binding for A

// design.newSession.build[A] -> produces B
// newDesign.newSession.build[A] -> produes C
```

## Session

To create instances, you need to create a `Session` from you Design:

```scala
val session = design.newSession
val a = session.build[A]
// do something with a
```

Session manages the life cycle of your objects and holds instances of singletons. These instances can be discarded after `session.shutdown` is called:

```scala
// Start a session
val session = design.newSession
try {
  session.start
  val p = session.build[P]
  // do something with P
}
finally {
   session.shutdown
}
```

To simplify this session management, you can use `Design.build[A]` to start and shutdown a session automatically:
```scala
design.build[P]{ p:P => // session.start will be called, and a new instance of P will be created
  // do something with P
}
// session.shutdown will be called here
```
This pattern is useful since you usually need a single entry point for starting an application.

## Life Cycle

Server side application often requires resource managemeng (e.g., network connection, threads, etc.). Airframe has a built-in object life cycle manager to implement these hooks:

```scala
trait MyServerService {
  val service = bind[Server]
    .onInit { _.init }      // Called when the object is initialized
    .onInject { _.inject }  // Called when the object is injected 
    .onStart = { _.start }  // Called when session.start is called
    .beforeShutdown = { _.notify } // Called right before all shutdown hook is called
                                   // Useful for adding pre-shutdown step 
    .onShutdown = { _.stop } // Called when session.shutdown is called
  )
}

trait Server {
  def init = ...
  def inject = ... 
  def start = ...
  def notify = ...
  def stop = ...
}
```
These life cycle hooks except `onInject` will be called only once when the binding type is singleton.

### Eager Initialization of Singletons for Production

In production, initializing singletons (by calling onStart) is preferred. To use production mode, 
use `Design.withProductionMode`:

```scala
// All singletons defined in the design will be initialized (i.e., onInit/onInject/onStart hooks will be called) 
design
  .withProductionMode
  .build[X]{ x =>
    // Do something with X
  }
```

### Suppress Life Cycle Logging

If you don't need to show Session start/terminate logs, use `Design.noLifeCycleLogging`:
```scala
design
  .noLifeCycleLogging
  .build[X]{ x => ... }
```
This will show lifecycle event logs only in debug level logs.

### Annotation-based life cycle hooks

Airframe also supports [JSR-250](https://en.wikipedia.org/wiki/JSR_250) style shutdown hooks via `@PostConstruct` and `@PreDestroy` annotations:

```scala
import javax.annotation.{PostConstruct, PreDestroy}

trait MyService {
  @PostConstruct
  def init {
    // Called when the object is initialized. The same behavior with onInit
  }
  
  @PreDestroy 
  def stop {
    // Called when session.shutdown is called. The same with onShutdown. 
  }
}
```

These annotation are not supported in Scala.js, because it has no run-time reflection to read annotations in a class. 

## What's Next

- See typical [Use Cases](use-cases.html) of Airframe
