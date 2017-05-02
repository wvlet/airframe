---
layout: docs
title: Basic Usage
---

# Basic Usage

**build.sbt** [![Latest version](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-la
ng.org/wvlet/airframe)

To use Airframe, add the following to your **build.sbt**:
```scala
libraryDependencies += "org.wvlet" %% "airframe" % "(version)"
```

And import `wvlet.airframe._` in your Scala code:
```scala
import `wvlet.airframe._
```

## Binding Examples

This example shows all binding types available in Airframe:

```
import wvlet.airframe._
import BindingExample._

trait BindingExample {
  val a = bind[A]  // Inject A
  val b = bind[B]  // Inject B

  val s = bindSingleton[S] // Inject S as a singleton

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

## Design Examples

To configure actual bindings, define object bindings using design:

```scala
import wvlet.airframe._
// If you define multiple bindings to the same type, the last one will be used.
val design : Design =
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

## Starting a Session

Session holds instances of singletons. These instances will be discarded after `session.shutdown` is called:

```scala
// Start a session
val session = design.newSession
try {
  session.start
  val p = session.build[P]
}
finally {
   session.shutdown
}
```

You can also use `Design.withSession` to start and shutdown a session automatically:
```scala
design.withSesssion { session =>
   valp = session.build[P]
}
```
