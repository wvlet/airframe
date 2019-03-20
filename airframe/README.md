Airframe
===

Airframe is a dependency injection library tailored to Scala, which is useful for isolating various concerns in your programing
in order to focus on the most important application logic. 

## What is Dependency Injection?

Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation;
Instead of enumerating necessary objects (dependencies) within constructor arguments, DI framework builds objects on your behalf.
In Java we can use Google's [Guice](https://github.com/google/guice), but its syntax is not suited to Scala,
 so we redesigned it for Scala so that we can naturally use Scala's syntax (trait and types) with DI.

- [DI Framework Comparison](https://wvlet.org/airframe/docs/comparison.html). Comparing Airframe with Google Guice, Macwire, Dagger2, etc.


## Quick Start

[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link]

To use Airframe, add the following dependency to your **build.sbt**:
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

### Basic Usage

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
    .bind[Z].to[ZImpl]          // Bind type Z to a singleton of ZImpl instance
```

Then **build** an instance and use it:
```scala
design.build[App]{ app =>
  // Do something with App
}
```

Airframe builds an instance of `App` based on the binding rules specified in the *design* object. That means when writing applications, you only need to care about how to use objects (*bind*), rather than how to build them, because design objects already knows how to provide necessary objects to build your classes.

This separation of object bindings and their design (assembly) is also useful for reducing code duplications between production and test codes. For example, compare writing `new App(new X, new Y(...), new Z(...), ...)` in both of your main and test codes, and just calling `session.build[App]`.
Airframe can integrate the flexibility of Scala traits and dependency injection (DI). Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.

## Isolating Service Logic and Design

When writing an application, there are several concerns that are often unrelated to the core applcation logic, such as:
- How to build service objects.
- How to configure services.
- How to manage life cycle of service objects.

Airframe DI allows separating these how-tos for building service objects and managing their lifecycles into `Design` objects 
so that you can focus on logic that uses only direct dependencies. 

For example, when writing service A and B, you should be able to focus only on DBClient and FluentdLogger, even though the entire
code involves other indirect dependencies like ConnectionPool, HttpClient, DB, etc.:

![image](https://wvlet.org/airframe/img/airframe/build-service-objects.png)

By injecting dependencies using `bind[X]` syntax (left), we can isolate the logic for constructing service objects (right):

![image](https://wvlet.org/airframe/img/airframe/code-example.png)

It is also possible to use constructor injection instead of using trait:

```scala
class A(dbClient:DBClient, fluentdClient:FluentdClient) {
   //...
}
```

## Airframe Features

- Simple usage. Only need to include `import wvlet.airframe._` to use Airframe.
- Designs are immutable, so you can create new designs safely based on existing designs.
- Supporting Scala traits for dependency injection, which was not available in other frameworks.
- Built-in life cycle management of objects (onInit, onStart, onShutdown, etc.) through sessions.
- Supporting Scala 2.11, 2.12, 2.13, and [Scala.js](https://www.scala-js.org/).
- Supporting Java 11.


# Airframe Usage

## Bind

In Airframe, you can use two types of dependency injections: __constructor injection__ or
__in-trait injection__:
 
![image](https://wvlet.org/airframe/img/airframe/injection-types.png)
 
### Constructor Injection
Constructor injection is the most natural form of injection.
When `session.build[A]` is called, Airframe will find the primary constructor of `A` and 
its arguments, then creates a new instance of `A` by finding dependencies from a _Design_.

```scala
import wvlet.airframe._

case class AppConfig(appName:String)
class MyApp(val config:AppConfig)

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
In-trait injection with `bind[X]` is useful to create reusable modules. 
```scala
import wvlet.airframe._

case class AppConfig(appName:String)
trait MyApp {
  val config = bind[AppConfig]
}

val d = newDesign
  .bind[AppConfig].toInstance(AppConfig("Hello Airframe!"))

// Creates a new MyApp
d.build[MyApp] { app: MyApp =>
   // Do something with app
}
// Session will be closed here
```

Note that `bind[X]` only works inside Scala traits:
```Scala
// [DON'T DO THIS] You can't use bind[X] inside classes:
class A {
  val a = bind[B] // [Error] class A can't find the current session
}
```

### Binding Types

The following examples show basic binding types available in Airframe:
```scala
val a = bind[A]          // Inject A as a singleton

import BindingExample._

// Constructor binding
val pc: P = bind[P] // Inject a singleton of P
                    // (Inject D1, D2 and D3)

// Provider bindings
val p0: P = bind { P() } // Inject P using the provider function (closure)
val p1: P = bind { d1:D1 => P(d1) } // Inject D1 to create P
val p2: P = bind { (d1:D1, d2:D2) => P(d1, d2) } // Inject D1 and D2 to create P
val p3: P = bind { (d1:D1, d2:D2, d3:D3) => P(d1, d2, d3) } // Inject D1, D2 and D3
val pd: P = bind { provider _ } // Inject D1, D2 and D3 to call a provider function

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

If you need to create a new instance for each binding, use `bindFactory[I => X]`.

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
  .bind[D3].toInstance(D3(3))    // Bind D3 to a concrete instance D3(3)
  .bind[P].toProvider{ d1:D1 => P(d1) } // Create a singleton P by resolving D1 from the design
  .bind[P].toProvider{ (d1:D1, d2:D2) => P(d1, d2) }  // Resolve D1 and D2
  .bind[P].toProvider{ provider _ }                   // Use the given function as a provider
  .bind[P].toInstanceProvider{ d1:D1 => P(d1) }       // Create a new instance using the provider function
  .bind[P].toEagerSingletonProvider{ d1:D1 => P(d1) } // Create an eager singleton using the provider function
```

If you define multiple bindings to the same type (e.g., P), the last binding will be used. 

Design objects are immutable, so you can safely override bindings without modifying the original design:
```scala
import wvlet.airframe._

val design: Design =
  newDesign.bind[A].to[B] // bind A to B

val newDesign: Design =
  design.bind[A].to[C] // Override binding for A

design.build[A] { x => ... } // -> x will be B
newDesign.build[A] { x => ... } // -> x will be C
```

Design supports `+` operator to combine multiple designs at ease:
```scala
val newDesign = d1 + d2 // d2 will override the bindings in d1 
```
`+` operator is not commutative because of the override.

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

Server side application often requires resource management (e.g., network connection, threads, etc.). Airframe has a built-in object life cycle manager to implement these hooks:

```scala
trait MyServerService {
  val service = bind[Server]
    .onInit( _.init )   // Called when the object is initialized
    .onInject(_.inject) // Called when the object is injected 
    .onStart(_.start)   // Called when session.start is called
    .beforeShutdown( _.notify) // Called right before all shutdown hook is called
                               // Useful for adding pre-shutdown step 
    .onShutdown( _.stop ) // Called when session.shutdown is called
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

To initialize `X` eagerly, `X` must be found in the design or used in the other dependencies defined in the design.

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

## Child Sessions

If you need to override a part of the design in a short term, you can use _child sessions_. Child sessions are useful for managing request-scoped sessions (e.g., HTTP requests, database query contexts, etc.). 

___Usage Example___

```scala
import wvlet.airframe._

trait MyServer {
  private val session = bind[Session]   // Bind the current session

  def handleInChildSession = {
    // Define a child session specific design
    val childDesign =
      newDesign
        .bind[X].toSingleton

    // Creates a new child session
    session.withChildSession(childDesign) { childSession =>
      childSession.build[X] { x =>
         ...
      }
    }
  }
}

// Creates a parent session
newDesign.build[MyServer] { server =>
   // Creates a short-lifecycle child session
   server.handleInChildSession
}
```

When building an object `X` in a child session, it will follow these rules:
- If `X` is defined in the child design, the child session will be used for `X`.
- If `X` is not defined in the child design, Airframe tries to find a design for `X` in the parent (or an ancestor) session (owner session).
- If `X` involves internal objects that are defined in a parent (e.g., `P1`) or an ancestor (e.g., `A1`), their owner sessions will be used
for instantiating `P1` and `A1`.
- Lifecycle hooks for `X` will be registered to the owner sessions of the target objects.
For example, if `X` is already started (onStart is called) in the parent session (= owner session), this hook will not be called again in the child session.


## What's Next

- See typical [Use Cases](https://wvlet.org/airframe/docs/use-cases.html) of Airframe
