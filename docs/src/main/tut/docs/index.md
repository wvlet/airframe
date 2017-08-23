---
layout: docs
title: Quick Start
---

# Quick Start

[![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)

This page describes all you need to know to start using Airframe.

## sbt

To use Airframe, add the following to your **build.sbt**:
```scala
libraryDependencies += "org.wvlet" %% "airframe" % "(version)"
```

And import `wvlet.airframe._` in your Scala code:
```scala
import wvlet.airframe._
```

## Bind

This example shows all binding types available in Airframe:

```
import wvlet.airframe._

trait BindingExample {
  val a = bind[A]  // Inject A
  val b = bind[B]  // Inject B

  // Inject S as a singleton  
  val s = bindSingleton[S]

  import BindingExample._

  // Constructor binding
  val pc: P = bind[P] // Inject P using constructor (Inject D1, D2 and D3)

  // Provider bindings
  val p0: P = bind { P() } // Inject P using the provider function (closure)
  val p1: P = bind { d1:D1 => P(d1) } // Inject D1 to create P
  val p2: P = bind { (d1:D1, d2:D2) => P(d1, d2) } // Inject D1 and D2 to create P
  val p3: P = bind { (d1:D1, d2:D2, d3:D3) => P(d1, d2, d3) } // Inject D1, D2 and D3

  val pd: P = bind { provider _ } // Inject D1, D2 and D3 to call a provider function
  val ps: P = bindSingleton { provider _ } // Create a singleton using a provider
}

object BindingExample {
  case class P(d1:D1 = D1(), d2:D2 = D2(), d3:D3 = D3())
  def provider(d1:D1, d2:D2, d3:D3) : P = P(d1, d2, d3)
}
```

## Design

To configure actual bindings, define object bindings using design:

```scala
import wvlet.airframe._

// If you define multiple bindings to the same type, the last one will be used.
val design: Design =
  newDesign                      // Create an empty design
  .bind[A].to[AImpl]             // Bind a concrete class AImpl to A
  .bind[B].toInstance(new B(1))  // Bind a concrete instance to B (This instance will be a singleton)
  .bind[S].toSingleton           // S will be a singleton within the session
  .bind[ES].toEagerSingleton     // ES will be initialized as a singleton at session start time
  .bind[D1].toInstance(D1(1))    // Bind D1 to a concrete instance D1(1)
  .bind[D2].toInstance(D2(2))    // Bind D2 to a concrete instance D2(2)
  .bind[D3].toInstance(D3(3))    // Bind D3 to a cocreete instance D3(3)
  .bind[P].toProvider{ d1:D1 => P(d1) } // Create P by resolveing D1 from the design to create P
  .bind[P].toProvider{ (d1:D1, d2:D2) => P(d1, d2) } // Resolve D1 and D2
  .bind[P].toProvider{ provider _ }  // Use a function as a provider. D1, D2 and D3 will be resolved from the design
  .bind[P].toSingletonProvider{ d1:D1 => P(d1) } // Create a singleton using the provider function
  .bind[P].toEagerSingletonProvider{ d1:D1 => P(d1) } // Create an eager singleton using the provider function
```

If you define multiple bindings to the same type (e.g., P), the last binding will be used. 

Design objects are immutable, so you can safely override bindings without modifying the original design:
```
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

You can also use `Design.withSession` to start and shutdown a session automatically:
```scala
design.withSesssion { session =>
   // session.start will be called here
   val p = session.build[P]
}
// session.shutdown will be called here
```

## Life Cycle

Server side application often requires resource managemeng (e.g., network connection, threads, etc.). Airframe has a built-in object life cycle manager to implement these hooks:

```scala
trait MyServerService {
  val service = bind[Server]
    .onInit { _.init }      // Called when the object is initialized
    .onInject { _.inject }  // Called when the object is injected 
    .onStart = { _.start }  // Called when session.start is called
    .beforeShutdown = { _.notify } // Called right before all shutdown hook is callsed
                                   // Useful for adding pre-shutdown step 
    .onShutdown = { _.stop } // Called when session.shutdown is called
  )
}
```
These life cycle hooks except `onInject` will be called only once when the binding type is singleton.

### Annotation-based life cycle hooks

Airframe also supports [JSR-250](https://en.wikipedia.org/wiki/JSR_250) style shutdown hooks `@PostConstruct` and `@PreDestroy`:

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
