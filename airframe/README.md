# Airframe DI

Airframe DI is a new dependency injection library designed for Scala. Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation; Instead of manually passing all necessary objects (dependencies) into the constructor argument, DI framework builds the object on your behalf.

Airframe DI has three major features:

- **Bind**: Inject necessary objects to your service without hand wiring.
- **Design**: Allow switching the application implementation at runtime.
- **Session**: Initialize and terminate injected services with lifecycle management hooks (e.g., onStart, onShutdown).

Airframe DI enables isolating the the application logic and service design. This abstraction addresses the common patterns in writing applications, such as:

- Switching the implementation between production and test/debug code.
- Minimizing the service implementation for the ease of testing.
- Configuring applications using config objects.
- Managing resources like database/network connections, threads, etc. .
- Managing differently configured singletons.
- etc., ...

Airframe is available for Scala 2.12, 2.13, and [Scala.js](https://www.scala-js.org/). Airframe also supports JDK11.

In Scala we have various approaches for dependency injection, such as [cake pattern](http://jonasboner.com/real-world-scala-dependency-injection-di/), [Google Guice](https://github.com/google/guice), [Macwire](https://github.com/adamw/macwire), [reader monad](https://softwaremill.com/reader-monad-constructor-dependency-injection-friend-or-foe/), etc. For more detailed comparison, see the following article:

- [DI Framework Comparison](https://wvlet.org/airframe/docs/comparison.html): Comparing Airframe with Google Guice, Macwire, Dagger2, etc.

## Quick Start

[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

[![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link]

To use Airframe DI, add the following dependency to your **build.sbt**:

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe" % "(version)"
```

And import `wvlet.airframe._` in your Scala code:

```scala
import wvlet.airframe._
```

### .scalafmt.conf

If you are using [scalafmt](https://scalameta.org/scalafmt/) for code formatting, add the following option to your `.scalafmt.conf`:

```scala
optIn.breaksInsideChains = true
```

This option allows writing each binding in a single line:

```scala
val d = newDesign
  .bind[X].toInstance(...)
  .bind[Y].to[YImpl]
```

## Basic Usage

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

This separation of object bindings and their design (assembly) will reduce duplications between production and test codes. For example, compare writing `new App(new X, new Y(...), new Z(...), ...)` in both of your main and test codes, and just calling `design.build[App]`.

Airframe integrates the flexibility of Scala traits and dependency injection (DI). Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.

## Bind

In Airframe, you can use two types of dependency injections: __constructor injection__ or
__in-trait injection__:
![image](https://wvlet.org/airframe/img/airframe/injection-types.png)

### Constructor Injection

Constructor injection is the most natural form of injection.
When `design.build[A]` is called, Airframe will find the primary constructor of `A` and its arguments, then creates a new instance of `A` by looking up instances for these constructor arguments defined in the _Design_.

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

If you need to bind dependencies within Scala traits, use in-trait injection with `bind[X]` syntax:

```scala
import wvlet.airframe._

case class AppConfig(appName:String)

trait MyApp {
  // In-trait injection
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

Note that `bind[X]` syntax works only inside Scala traits or classes that implement `wvlet.airframe.DISupport` trait:

```scala
import wvlet.airframe._

// [DON'T DO THIS] You can't use bind[X] inside classes:
class A {
  val a = bind[B] // [Error] class A can't find the current session
}

// To use bind[X] inside classes, extends wvlet.airframe.DISupport 
class A(val session:Session) extends DISupport {
  val a = bind[B] // OK
}

```

If you used the `bind[X]` syntax inside a class, MISSING_SESSION error will be thrown.

### Binding Types

Airframe DI supports three types of in-trait bindings: `bind[X]`, `bindLocal{...}`, and `bindFactory`.

- `bind[X]` will inject a singleton instance of X by default. `Design` object will determine how to prepare an instance of `X`.
- `bindLocal{ new X(...) }` will inject a new instance of X using the given code block. Use `bindLocal` only when you need to locally define object initializtiaon code. This is a good practice for ensuring [RIIA (Resource Initialization Is Acquisition)](https://en.wikipedia.org/wiki/Resource_acquisition_is_initialization) by writing the resource initialization code to the closest place where the resource will be used.
  - bindLocal will override bindings for `X` defiened in `Design` in favor of the local initialization code block.
  - If you need to build an instance of `X` based on the other dependencies, use a provider fucntion like `bindLocal{ (d1:D1, d2:D2, ...) => new X(d1, d2, ...)}`. Dependenies of D1, D2, ... will be injected from the design.
- `bindFactory[D1=>X]` will create a factory method to generate X from a given instance of D1. This is used for partially overriding the design (e.g., `D1`) for building `X`.

The lifecycle (including calling onInject, onStart, onShutdown hooks) of the injected instances will be managed by the session of Airframe. To properly release the resources injected by bindings, define these lifecycle hooks in the design or implement [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) interface. If the injected instance implements AutoCloseable, `def close(): Unit` method of AutoCloseable will be called when the session terminates. See also [Design](#design) and [Life Cycle](#life-cycle) sections for more details.

Here are several examples of in-trait binding types:

```scala
import BindingExample._

// Basic binding
val a = bind[A]          // Inject A as a singleton

// Default constructor binding
val pc: P = bind[P] // Inject a singleton of P(D1, D2, D3)
                    // This will also inject D1, D2 and D3 to P.

// Local binding for creating a new instance using the given code block
val l1: P = bindLocal{ new P() }
val l2: P = bindLocal{ d1:D1 => new P(d1) }

// Factory bindings for partially overriding dependencies
val f1: D1 => P = bindFactory[D1 => P] // A factory to use a given D1 to generate P
val f2: (D1, D2) => P = bindFactory2[(D1, D2) => P] // A factory to use given D1 and D2
...

object BindingExample {
  case class P(d1:D1 = D1(), d2:D2 = D2(), d3:D3 = D3())
  def provider(d1:D1, d2:D2, d3:D3) : P = P(d1, d2, d3)
}
```

By default all injections generates singleton objects that are alive until closing the current session. These singleton objects are managed inside the current session object.

## Design

To configure bindings described in the above, we need to define a `Design` object using the following syntaxes:

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

### Singleton Bindings

If you only need singletons (e.g.,`X`) and how to construct `X` is clear from its definition, no need exists to specify `bind[X].toSingleton` in your design:

```scala
import wvlet.airframe._

trait X {
  val y = bind[Y]
}
trait Y {
  val z = bind[Z]
}
case class Z(port:Int)

val design: Design =
  newDesign
    // Binding X and Y toSingleton is unnecessary as singleton binding is the default behavior.
    //.bind[X].toSingleton
    //.bind[Y].toSingleton
    .bind[Z].toInstance(port = 8080)  // Z has no default instance, so we should bind it manually.
```

### Design is Immutable

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

Design supports `+` (add) operator to combine multiple designs at ease:

```scala
val newDesign = d1 + d2 // d2 will override the bindings in d1
```

`+` operator is not commutative because of this override, so `d1 + d2` and `d2 + d1` will be different designs if there are some overlaps.

## Session

To create instances, you need to create a `Session` from your Design:

```scala
val session = design.newSession
val a = session.build[A] {
  // Do something with a
}
```

If you need a typed-return value, you can use `design.run[A, B](f: A=>B)`:

```scala
val ret: Int = design.run { a: A =>
  // Do something with a and return a value
  1
}
```

This will build an instance of A from the design, and return the result.

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

__Update since version 19.9.0__: If objects injected by DI implements `def close(): Unit` function of `java.lang.AutoCloseable` interface, airframe will call the close method upon the session shutdown. To override this behavior, define your own `onShutdown` hook or use `@PreDestory` annotation.

Server side application often requires resource management (e.g., network connection, threads, etc.). Airframe has a built-in object life cycle manager to implement these hooks:

```scala
import wvlet.airframe._

object MyServerService {
  val design = newDesign
    .bind[Server]
    .onInit{x:Server => ... }    // Called when the object is initialized
    .onInject{x:Server => ... }  // Called when the object is injected
    .onStart{x:Server => ... }   // Called when session.start is called
    .beforeShutdown{x:Server => ...}  // Called right before all shutdown hook is called
                                      // Useful for adding pre-shutdown step
    .onShutdown{x:Server => ... } // Called when session.shutdown is called
  )
}
```

These life cycle hooks except `onInject` will be called only once when the binding type is singleton.

### Eager Initialization of Singletons for Production

In production, initializing singletons (by calling onStart) is preferred. To use production mode, use `Design.withProductionMode`:

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

### Finding The Current Session

You may need to find the current session to manage lifecycles of manually created instances.
In this case, you can bind Airframe's Session with `bind[Session]` and register newly created instances to the session:

```scala
import wvlet.airframe._

class MyDB(name:String) {
  private val conn = newConnection(name)
    .onShutdown{ x => x.close() }
}

trait MyApp {
  private val session = bind[Session]

  def openDB(name:String): MyDB = {
    val db = new MyDB(name)
     // Adding MyDB instance to the current session so that
     // MyDB connection can be closed when the session terminates.
    session.register(db)
    db
  }
}
```

### Child Sessions

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

## Designing Applications with Airframe

When writing an application, these concerns below are often unrelated to the core applcation logic:

- How to build service objects.
- How to configure services.
- How to manage life cycle of service objects.

Airframe allows separating these concerns into `Design`. For example, when writing service A and B in the following figure, you should be able to focus only direct dependencies. In this example DBClient and FluentdLogger are the direct dependencies of A and B.

![image](https://wvlet.org/airframe/img/airframe/build-service-objects.png)

When building objects A and B, we usually need to think about the other indirect dependencies like ConnectionPool, HttpClient, DB, etc. By injecting dependencies using `bind[X]` syntax (left), we can effectively forget about there indirect dependencies (right):

![image](https://wvlet.org/airframe/img/airframe/code-example.png)

## What's Next

- See typical [Use Cases](https://wvlet.org/airframe/docs/use-cases.html) of Airframe
