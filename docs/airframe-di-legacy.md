---
id: airframe
layout: docs
title: airframe-di: Dependency Injection
---
Airframe DI is a new dependency injection library designed for Scala. Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation; Instead of manually passing all necessary objects (dependencies) into the constructor argument, DI framework builds the object on your behalf.

Airframe DI introduces three concepts to your Scala programming:

- **Bind** for injecting necessary objects to your service through constructor arguments or `bind[X]` syntax.
- **Design**: for customizing the actual application implementation to use at runtime.
- **Session**: for managing singleton instances and properly initialize and terminate injected service objects with lifecycle management hooks (e.g., onStart, onShutdown, or `def close()` method in AutoCloseable interface).

One of the advantages of Airframe DI is that it enables isolating application logic and service design. This abstraction addresses the common patterns in writing applications, such as:

- Switching the implementation between production and test/debug code.
- Minimizing the service implementation for the ease of testing.
- Configuring applications using config objects.
- Managing resources like database/network connections, threads, etc. .
- Managing differently configured singletons.
- etc., ...

Airframe is available for Scala 2.12, 2.13, and [Scala.js](https://www.scala-js.org/). Airframe also supports JDK11.

In Scala, we have various approaches for dependency injection, such as [cake pattern](http://jonasboner.com/real-world-scala-dependency-injection-di/), [Google Guice](https://github.com/google/guice), [Macwire](https://github.com/adamw/macwire), [reader monad](https://softwaremill.com/reader-monad-constructor-dependency-injection-friend-or-foe/), etc. For more detailed comparison, see the following article:

- [DI Framework Comparison](https://wvlet.org/airframe/docs/comparison.html): Comparing Airframe with Google Guice, Macwire, Dagger2, etc.

## Quick Start

[![maven central](https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22) [![scala-index](https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange)](https://index.scala-lang.org/wvlet/airframe)

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

This separation of object bindings and their design (assembly) will reduce duplications between production and test codes. For example, compare writing `new App(new X, new Y(...), new Z(...), ...)` in both of your main and test codes, and just calling `design.build[App]`.

Airframe integrates the flexibility of Scala traits and dependency injection (DI). Mixing traits is far easier than calling object constructors. This is because traits can be combined in an arbitrary order. So you no longer need to remember the order of the constructor arguments.

## Bind

In Airframe, you can use two types of dependency injections: __constructor injection__ or
__in-trait injection__:

### Constructor Injection

Constructor injection is the most natural form of injection.
When `design.build[A]` is called, Airframe will find the primary constructor of `A` and its arguments, then creates a new instance of `A` by looking up instances for the constructor arguments defined in the _Design_.

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

To configure bindings described in the above, we need to define a `Design` object using the following syntax:

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

// or use Design.add(Design) 
d1.add(d2)
```

`+` (add) operator is not commutative because of this override behavior, so `d1 + d2` and `d2 + d1` will be different designs if there are some overlaps.

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
      val x = childSession.build[X]
      ...
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

## Life Cycle

__Update since version 19.9.0__: If objects injected by DI implements `def close(): Unit` function of `java.lang.AutoCloseable` interface, airframe will call the close method upon the session shutdown. To override this behavior, define your own `onShutdown` hook or use `@PreDestory` annotation.

Server side application often requires resource management (e.g., network connection, threads, etc.). Airframe has a built-in object life cycle manager to implement these hooks:

```scala
import wvlet.airframe._

object MyServerService {
  val design = newDesign
    .bind[Server]
    .onInit{ x:Server => ... }        // Called when the object is initialized
    .onInject{ x:Server => ... }      // Called when the object is injected
    .onStart{ x:Server => ... }       // Called when session.start is called
    .afterStart{ x:Server => ... }    // Called after onStart lifecycle is finished.
                                      // Use this only when you need to add an extra startup process for testing.
    .beforeShutdown{ x:Server => ...} // Called right before all shutdown hook is called
                                      // Useful for adding pre-shutdown step
    .onShutdown{ x:Server => ... }    // Called when session.shutdown is called
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
  def init = {
    // Called when the object is initialized. The same behavior with onInit
  }

  @PreDestroy
  def stop = {
    // Called when session.shutdown is called. The same with onShutdown.
  }
}
```

These annotations are not supported in Scala.js, because Scala.js has no run-time reflection to read annotations in a class. For maximum compatibility, we recommend using onStart/onShutdown hooks or implementing AutoCloseable interface.


## Designing Applications with Airframe

When writing an application, these concerns below are often unrelated to the core application logic:

- How to build service objects.
- How to configure services.
- How to manage life cycle of service objects.

Airframe allows separating these concerns into `Design`. For example, when writing service A and B in the following figure, you should be able to focus only direct dependencies. In this example DBClient and FluentdLogger are the direct dependencies of A and B.

![image](https://wvlet.org/airframe/img/airframe/build-service-objects.png)

When building objects A and B, we usually need to think about the other indirect dependencies like ConnectionPool, HttpClient, DB, etc. By injecting dependencies using `bind[X]` syntax (left), we can effectively forget about there indirect dependencies (right):

![image](https://wvlet.org/airframe/img/airframe/code-example.png)


## Advanced Binding Types

### Generic Type Binding

Airframe can bind objects to generics types. Traditional DI libraries for Java (e.g., [Guice](https://github.com/google/guice), etc.) cannot
distinguish generic classes that have different type parameters (e.g., `Seq[Int]`, `Seq[String]`) because Java compiler applies [type erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html), and converts them to the same `Seq[Object]` type. In Airframe, generic types with different type parameters will be treated differently. For example, all of the following bindings can be assigned to different objects:

```scala
bind[Seq[_]]
bind[Seq[Int]]
bind[Seq[String]]

bind[Map[Int,String]]
bind[Map[_,_]]
```

Behind the scene, Airframe uses [Surface](https://github.com/wvlet/airframe/surface/) as identifier of types so that we can extract these types identifiers at compile time.

### Type Alias Binding

If you need to bind different objects to the same data type, use type aliases of Scala. For example,
```scala
case class Fruit(name: String)

type Apple = Fruit
type Banana = Fruit

trait TaggedBinding {
  val apple  = bind[Apple]
  val banana = bind[Banana]
}
 ```

Alias binding is useful to inject primitive type values:
```scala
import wvlet.airframe._

type Env = String

trait MyService {
  // Conditional binding
  lazy val threadManager = bind[Env] match {
     case "test" => bind[TestingThreadManager] // prepare a testing thread manager
     case "production" => bind[ThreadManager] // prepare a thread manager for production
  }
}

val coreDesign = newDesign

val testingDesign =
  coreDesign.
    bind[Env].toInstance("test")

val productionDesign =
  coreDesign
    .bind[Env].toInstance("production")
```

### Multi-Binding

If you want to switch a service to be called depending on the user input, you can just use Scala's functionality + Airframe binding.

To illustrate this, consider building an web application that receives a request and returns a string message.
`Dispatcher` class receives an URL path and choose an appropriate `Handler` to use:

```scala
import wvlet.airframe._

trait Handler {
  def handle(request:Request): String
}

trait DefaultHandler extends Handler {
  def handle(request:Request): String = "hello"
}

trait InfoHandler extends Handler {
  def handle(rquest:Request): String = "info"
}

trait Dispatcher {
  private val dispatcher: String => Handler = {
    case "info" => bind[InfoHandler]
    case _ => bind[DefaultHandler]
  }

  def dispatch(path:String, request:Request): String = {
     dispatcher(path).handle(request)
  }
}
```

In Google Guice, we need to use a special binder like [Multibinder](https://github.com/google/guice/wiki/Multibindings).
In Airframe, we just need to write a Scala code that uses `bind[X]`.

### Tagged Type Binding

Tagged binding `@@` is also useful to annotate type names:

```scala
// This import statement is necessary to use tagged type (@@)
import wvlet.airframe.surface.tag._

trait Name
trait Id

trait A {
  val name = bind[String @@ Name]
  val id = bind[Int @@ Id]
}
```

## Known Issues

### Running `design.build[X]` inside Future causes ClassNotFoundException in sbt 1.3.x

This is caused by [LayeredClassLoader of sbt 1.3.x](https://github.com/sbt/sbt/issues/5410), which
initialize Scala's global ExecutionContext with a class loader isolated from the application classloader.

To avoid this issue, we need to explicitly prepare an executor for the Future inside the application,
instead of using `scala.concurrent.ExecutionContext.Implicits.global`

```scala
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import wvlet.airframe._

// Do not import scala.concurrent.ExecutionContext.Implicits.global
val threadPool              = Executors.newCachedThreadPool()
implicit val futureExecutor = ExecutionContext.fromExecutor(threadPool)

case class MyConfig(port: Int = 8080)

Future {
  newDesign.build[MyConfig] { config => println(config) }
}
```

Another workaround is setting `fork in run := true` or `fork in test := test` to your `build.sbt`, or using `Flat` classloader layering strategy:

```scala
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
```

## Debugging DI

To check the runtime behavior of Airframe's dependency injection, set the log level of `wvlet.airframe` to `debug` or `trace`:

**src/main/resources/log.properties**
```
wvlet.airframe=debug
```

While debugging the code in your test cases, you can also use `log-test.properties` file:
**src/test/resources/log-test.properties**
```
wvlet.airframe=debug
```
See [airframe-log configuration](https://github.com/wvlet/airframe/blob/master/log/README.md#configuring-log-levels) for the details of log level configurations.


Then you will see the log messages that show the object bindings and injection activities:
```
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(PlaneType,List(),wvlet.airframe.LazyF0@442b0f),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(Metric,List(),wvlet.airframe.LazyF0@1595a8db),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ClassBinding(Engine,GasolineEngine)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(PlaneType,List(),wvlet.airframe.LazyF0@b24c12d8),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ClassBinding(Engine,SolarHybridEngine)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [SessionBuilder] Creating a new session: session:7bf38868  - (SessionBuilder.scala:48)
2016-12-29 22:23:17-0800 debug [SessionImpl] [session:7bf38868] Initializing  - (SessionImpl.scala:48)
2016-12-29 22:23:17-0800 debug [SessionImpl] [session:7bf38868] Completed the initialization  - (SessionImpl.scala:55)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get or update dependency [AirPlane]  - (SessionImpl.scala:80)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [wvlet.obj.tag.@@[example.Example.Wing,example.Example.Left]]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [wvlet.obj.tag.@@[example.Example.Wing,example.Example.Right]]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.Engine]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get or update dependency [Fuel]  - (SessionImpl.scala:80)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.PlaneType]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.Metric]  - (SessionImpl.scala:60)
```

### Tracing DI with Google Chrome Browser

To visualize the lifecycle of injected objects, enabling `ChromeTracer` is useful:

```scala
import wvlet.airframe._

val d = newDesign
  .withTracer(ChromeTracer.newTracer("target/trace.json"))

// DI tracing report will be stored in target/trace.json
// You can open this file with Google Chrome. Open chrome://tracing, and load the json file.
d.build[MyApp] { app =>
  //
}
```

After running a session, open `target/trace.json` file using Google Chrome. Open `chome://tracing`, and load the json file. It will
display the lifecycle of AirframeSession and the injected objects: 

![image](https://wvlet.org/airframe/img/airframe/chrome_tracing.png)


## Use Cases

This page illustrates typical use cases of Airframe.

- [Configuring Applications](#configuring-applications)
- [Managing Resources](#managing-resources)
- [Factory Binding](#factory-binding)
- [Service Mix-In](#service-mix-in)
- [Override Bindings](#override-bindings)

### Configuring Applications

Configuring applications is cumbersome because you need to think about how to pass configurations to your classes. With Airframe this process becomes much simpler; Just binding configuration objects to your class:

```scala
import wvlet.airframe._

case class Config(host:String, port:Int)

trait App {
  // bind configurations
  private val config = bind[Config]
  private val server = bind[Server]

  def run {
    sever.launch(config.host, config.port)
  }
}

// Create a new design and add configuration
val d =
  newDesign
  .bind[Server].to[YourServer]
  .bind[Config].toInstance(new Config("localhost", 8080))

// Start the application
d.withSession { session =>
  val app = session.build[App]
  app.run
}
```

To change the configuration, you only need to add another binding because bindings to the same type object can be overwritten:
```scala
// You can override Config in your test code
val testDesign =
  d.bind[Config].toInstance(new Config("localhost", randomPort))
```

Airframe has a submodule called [airframe-config](airframe-config.md), which is useful for
configuring your applications with YAML files.

### Managing Resources

Airframe makes easier managing resources (e.g., network or database connections, thread managers, etc.) For example, if you are writing an application that requires an access to a database service. You need to establish a connection, and also need to properly close the connection after the application terminates. Airframe support such resource management using [Life Cycle](#life-cycle) triggers (onInit, onStart, onShutdown):

```scala
import wvlet.airframe._

trait DBService {
  private val dbConfig = bind[DBConfig]
  private val connection = bind[DBConnection]
    .onInit { c => c.connect(dbConfig.url, dbConfig.user, ... ) }
    .onShutdown {
      // This will be executed when session.shutdown is called
      c => c.close
    }

  def query(sql:String) = {
    connection.query(sql)
  }
}

// Using DBService. This class has no need to care about closing DB
// connection resources because DBService will take care of it.
trait App {
  val dbService = bind[DBService]

  dbService.query("select * from tbl")
}

// Your application launcher code
val d = newDesign
  .bind[DBService].toSingleton // To share the connection between classes
  .bind[DBConfig].toInstance(DBConfig("jdbc://...", "user name", ...))

d.withSession { session =>
  // db connection will be established here
  val app = session.build[App]
}
// database connection will be closed automatically

```

### Factory Binding

If you need to configure a service (e.g., port number of an web client), but you need to provide other dependencies from Airframe,
`bindFactory[I => A]` can be used.

```scala
trait MyClient {
  private val port = bind[Int] // This will be overwritten by the factory
  private val httpClientConfig = bind[HttpClientConfig] // Use the shared instance
  private val httpClient = new HttpClient(port, httpClientConfig)

  @PreDestroy
  def stop: Unit = {
    httpClient.close()
  }
}

trait MyService {
  // Create a factory Int => MyClient, which will override Int binding using a given parameter.
  val clientFactory = bindFactory[Int => MyClient]
}


newDesign
  .bind[HttpClientConfig].toInstance(HttpClientConfig(useSSL=true, timeoutSec=60))
  .build[MyService] { s =>
    val client1 = s.clientFactory(8080)
    val client2 = s.clientFactory(8081)
  }
// clients will be closed here
```
In this example, port number (Int) can be provided later when instantiating MyClient.
HttpClientConfig instance can be shared between generated clients.
You can also define lifecycle hooks to MyClient, which will be added for each generated instance of MyClient.


### Service Mix-In

A traditional way of building applications is passing necessary services to a main class:
```scala
class YourService(threadPool:ThreadPool, s1:Service1, s2:Service2, ...) {
  ...
}

val t = new ThreadPool
val s1 = new Service1(...)
val s2 = new Service2(...)
...
val service = new YourService(t, s1, s2, ...)
```

However, this approach is not scalable if you need to use more services in your class or if you need to implement applications that require different subsets of services.

If you write such services as [traits](http://docs.scala-lang.org/tutorials/tour/traits.html) in Scala, it will be quite easy to compose applications that depends on many services. Here is an example of defining services using Airframe and Scala traits:
```scala
import wvlet.airframe._

// Define a thread pool functionality
trait ThreadPool {
  private val executorService = bind[ExecutorService]
    .onShutdown{ _.shutdown }

  def submit[U](body: => U) {
    executorService.submit(new Runnable {
      def run = { body }
    }
  }
}

// Instead of using constructor arguments,
// create a service trait that binds necessary service objects
trait ThreadPoolService {
  val threadPool = bindSingleton[ThreadPool]
}

// Another service
trait MonitorService {
  val monitor = bind[Monitor]
}

// Mix-in services
trait App1 extends ThreadPoolService with MonitorService {
  monitor.log("starting app")
  threadPool.submit( ... )
}

// Reuse singleton ThreadPool in another application
trait App2 extends ThreadPoolService {
  threadPool.submit( ... )
}
```

In general, you can create your application with Service mix-ins as below:

```scala
trait YourApp
 extends AService
    with BService
    with CDService
    ...
    with ZService
{
  // use a, b, c, d, .., z here
}

trait AService {
  val a = bind[A]
}

trait BService {
  val b = bind[B]
}

trait CDService {
  val c = bind[C]
  val d = bind[D]
}
...

```

### Override Bindings

It is also possible to manually inject an instance implementation. This is useful for changing the behavior of objects for testing:
```scala
trait CustomApp extends App1 {
  // Manually inject an instance
  override val monitor = new MockMonitor { ... }
}
```

If you are using [ScalaMock](http://scalamock.org/) or [Mockito](http://site.mockito.org/), you may overwrite a service with a mock (empty) implementation:

```scala
trait MockApp extends App1 {
  override val monitor = mock[Monitor]
}
```

Or you can use mock instance binding by extending the design:

```scala
val coreDesign =
  newDesign
  .bind[Monitor].to[MonitorImpl]

val testDesign =
  coreDesign
  .bind[Monitor].toInstance(mock[Monitor])
```

## Airframe Internals

This page describes the internals of Airframe for developers who are interested in extending Airframe.

### Session

A Session in Airframe is a holder of instances and binding rules. Airframe is designed to simplify the instantiation of complex objects like:
```scala
new App(a = new A(b = new B), ...)
```

into this form:
```scala
session.build[App]
```

In this code Airframe DI will take care of the object instantiation by automatically finding how to build `App`, and its dependencies `A`, `B`, etc.

### Example

To explain the role of Session, let's start with a simple code that uses Airframe bindings:

```scala
import wvlet.airframe._

trait App {
  val a = bind[A]
}

trait A {
  val b = bind[B]
}

val session =
  newDesign
  .bind[B].toInstance(new B(...))
  .newSesion // Creates a session thats holds the above instance of B

val app = session.build[App]
```
This code builds an instance of `App` using a concrete instance of `B` stored in the session.

### Injecting Session

To create instances of `A` and `B` inside `App`, we need to pass the concrete instance of B though the session instance. But trait definitions of `App` and `A` don't know anything about the session, so we need a way to resolve the instance of `B`.

To do so, Airframe will pass a reference to the Session while building `App`, `A`, and `B`. A trick is inside the implementation of `build` and `bind`. Let's look at how `session.build[App]` will work when creating an instance of `App`.

Here is the code for building an App:

```scala
val app = session.build[App]
```

Airframe expands this code into this form at compile-time:

```scala
val app: App =
{ ss: Session =>
  // Extends DISupport to pass Session object
  new App extends DISupport {
    // Inject a reference to the current session
    def session = ss

    // val a = bind[A] (original code inside App)
    // If type A is instantiatable trait (non abstract type)
    val a: A = {
      // Trying to find a session (using DISupport.session).
      // If no session is found, MISSING_SESSION exception will be thrown
      val ss1 = wvlet.airframe.Session.findSession(this)
      val binder: Session => A = (ss2: Session =>
        // Register a code for instantiating A
        ss2.getOrElseUpdate(Surface.of[A],
	  (new A with DISupport { def session = ss1 }).asInstanceOf[A]
        )
      )
      // Create an instance of A by injecting the current session
      binder(ss1)
    }
  }
}.apply(session)
```

To generate the above code, Airframe is using [Scala Macros](http://docs.scala-lang.org/overviews/macros/overview.html). You can find the actual macro definitions in [AirframeMacros.scala](https://github.com/wvlet/airframe/blob/master/airframe-macros/shared/src/main/scala/wvlet/airframe/AirframeMacros.scala)

When `bind[X]` is called, the active session must be found. So if you try to instantiate A without using `session.build[A]`, `MISSING_SESSION` runtime-error will be thrown:

```scala
val a1 = new A // MISSING_SESSION error will be thrown at run-time

val a2 = session.build[A] // This is OK
```

In the above code, `A` will be instantiated with DISupport trait, which has `session` definition. `bind[B]` inside trait `A` will be expanded liks this similarly:

```scala
new A extends DISupport {
  // (original code) val b = bind[B]
  val b: B = { ss: Session =>
    val ss = findSession(this)
    // If the session already has an instance of B, return it. Otherwise, craete a new instance of B
    ss.getOrElse(Surface.of[B], (session:Session => new B with DISupport { ... } ))
  }
  // Inject the current session to build B
  .apply(session)
}
```

### Comparison with a naive approach

The above macro-generated code looks quite scarly at first glance.
However, if you write similar code by yourself, you will end up doing almost the same thing with Session.

For example, consider building `App` trait using a custom `B` instance:

```scala
{
  val myB = new B {}
  val myA = new A(b = myB) {}
  new App(a = myA)
}
// How can we find myA and myB after exiting the scope?
// What if a and b hold resources (e.g., network connection, database connection, etc.), that need to be released later?
```

To manage life cycle of A and B, you eventually needs to store the object references somewhere like this:

```scala
// Assume storing objects in a Map-backed session
val session = Map[Class[_], AnyRef]()

session += classOf[B] -> new B {}
session += classOf[A] -> new A(b=session.get(classOf[B])) {}

val app = new App(a = session.get(classOf[A])) {}
session += classOf[App] -> app

// At shutdown phase
session.objects.foreach { x=>
  x match {
    case a:A => // release A
    case b:B => // release B ...
    case _ => ...
  }
}

```
As we have seen in the example of [Service Mix-in](#service-mix-in), if we need to manage hundreds of services,
manually writing such object management functions will be cumbersome. Airframe helps you to oraganize building service objects.


### Instantiation Methods

When `bind[X]` is used, according to the type of `X` different code can be generated:

- If `X` is a non-abstract trait, the generated code will be like the above.
- If `X` is a non-abstract class that has a primary constructor, Airframe inject dependencies to the constructor arguments:

```scala
// case class X(a:A, b:B, ..)

val surface = Surface.of[X]
// build instances of a, b, ...
val args = surface.params.map(p -> session.getInstance(p.surface))
surface.objectFactory.newInstance(p)
```

- If `X` is an abstract class or trait, `X` needs to be found in X because `X` cannot be instantiated automatically:

```scala
session.get(Surface.of[X])
```


### Surface

Airframe uses `Surface.of[X]` as identifiers of object types. [Surface](https://github.com/wvlet/airframe/tree/master/surface) is an object type inspection library.

Here are some examples of Surface:
```scala
import wvlet.surface

Surface.of[A] // A
Surface.of[Seq[Int]] // Seq[Int]
Surface.of[Seq[_]] // Seq[_]
// Seq[Int] and Seq[_] are different types as Surface

// Type alias
type MyInt = Int
Surface.of[MyInt] // MyInt:=Int
```

Surface treats type aliases (e.g., MyInt) and Int as different types. This provides flexibilities in binding different objects to the same type. For example, you can define MyInt1, MyInt2, ... Google Guice doesn's support this kind of bindings to the same types.

Scala is a JVM language, so at the byte-code level, all of generics type parameters will be removed because of type erasure.
That means, we cannot distinguish between `Seq[Int]` and `Seq[_]` within the byte code; These types are the same type `Seq[AnyRef]` in the byte code:
```
Seq[Int] => Seq[AnyRef]
Seq[_] => Seq[AnyRef]
```
Surface knows the detailed type parameters like `Seq[Int]` and `Seq[_]`, so it can distinguish these two `Seq` types.


To provide detailed type information only available at compile-time, Surface uses runtime-reflecation, which can pass compile-type type information such as
 function argument names, generic types, etc., to the runtime environment. Surface extensively uses `scala.reflect.runtime.universe.Type`
information so that bindings using type names can be convenient for the users.

For compatibility with [Scala.js](https://www.scala-js.org/), which doesn't support any runtime reflection,
Surface uses Scala macros to embed compile-time type information into the runtime objects.

### Surface Parameters

Surface also holds object parameters, so that we can find objects necessary for building `A`:
```scala
case class A(b:B, c:C)

// B and C will be necessary to build A
Surface.of[A] => Surface("A", params:Seq("b" -> Surface.of[B], "c" -> Surface.of[C]))
```
