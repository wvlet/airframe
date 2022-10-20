---
id: airframe-di
layout: docs
title: airframe-di: Dependency Injection
---
Airframe DI is a dependency-injection library designed for Scala. Dependency injection ([Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection)) is a design pattern for simplifying object instantiation; Instead of manually passing all necessary objects (dependencies) into the constructor argument, DI framework builds the object on behalf of you.

To start using Airframe DI, you only need to know about *Design* and *Session*:

- **Design** specifies mappings between types and its implementation.
- **Session** holds singleton instances of your application, and helps building your application objects. The session will properly manage the lifecycle of objects using user-defined lifecycle hooks (e.g., onStart, onShutdown, or `def close()` method in AutoCloseable interface of Java, etc.).

With Airframe DI, you can solve typical programming patterns, such as:

- Switching the implementation between production and test and debug code.
- Reusing complex object construction patterns (e.g., launching a server application with custom start/shutdown hooks).
- Minimizing the service implementation classes.
- Configuring your applications by injecting config objects.
- Managing resources like database/network connections, threads, etc. in the right order.
- Managing differently configured singletons of the same type.
- ..., etc.

Airframe DI is available for Scala 2.12, 2.13, Scala 3, and [Scala.js](https://www.scala-js.org/). 

In Scala, we have various approaches for dependency injection, such as [cake pattern](http://jonasboner.com/real-world-scala-dependency-injection-di/), [Google Guice](https://github.com/google/guice), [Macwire](https://github.com/adamw/macwire), [reader monad](https://softwaremill.com/reader-monad-constructor-dependency-injection-friend-or-foe/), etc. For more detailed comparison, see also [DI Framework Comparison](https://wvlet.org/airframe/docs/comparison.html), which describes pros and cons of various DI frameworks, including Airframe, Google Guice, Macwire, Dagger2, etc.

## Quick Start

[![maven central](https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22)

To use Airframe DI, add the following dependency to your **build.sbt**:

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe" % "(version)"

// For Scala.js
libraryDependencies += "org.wvlet.airframe" %%% "airframe" % "(version)"
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

First, create a class that has some parameters as dependencies. For example, the following code defines an App class having X, Y, and Z as its dependencies:

```scala
import wvlet.airframe._

class App(x:X, y:Y, z:Z) {
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

Airframe builds an instance of `App` based on the binding rules specified in the *design* object. That means when writing applications, you only need to care about how to use objects, rather than how to build them, because the design already knows how to provide necessary objects to build your classes.

This separation of object binding and their design (assembly) will reduce the duplications between production and test codes. For example, compare writing `new App(new X, new Y(...), new Z(...), ...)` in both of your main and test codes, and just calling `design.build[App]`.


## Constructor Injection

Airframe DI supports only __constructor injection__, which is the most natural form of injection.
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

> ### Why only supports constructor injection?
> 
> In the previous version of Airframe DI, we supported [in-trait injections](airframe-di-legacy.md). This design, however, introduces the complexity of application design because you need to worry about which type of injections (constructor or in-trait injection?) is appropriate. And also, your application needs to depend on Airframe DI. 
>
> If we only use constructor-injection, no Airframe DI dependency is required to your application interface. Only when binding actual implementations to your application, you need to use Airframe DI Design. This achieves a clear separation of application logic and its construction design.
>

## Design

To configure injected instances, you need to define a `Design` object using one of the following syntax:

```scala
import wvlet.airframe._

// If you define multiple bindings to the same type, the last one will be used.
val design: Design =
  newDesign                      // Create an empty design
  .bind[A].to[AImpl]             // Bind a class AImpl to A (Singleton)
  .bind[B].toInstance(new B(1))  // Bind a concrete instance to B (This instance will be a singleton)
  .bind[S].toSingleton           // S will be a singleton within the session
  .bind[ES].toEagerSingleton     // ES will be initialized as a singleton at session start time
  .bind[D1].toInstance(D1(1))    // Bind D1 to a concrete instance D1(1)
  .bind[D2].toInstance(D2(2))    // Bind D2 to a concrete instance D2(2)
  .bind[D3].toInstance(D3(3))    // Bind D3 to a concrete instance D3(3)
  .bind[P].toProvider{ d1:D1 => P(d1) } // Create a singleton P by resolving D1 from the design
  .bind[P].toProvider{ (d1:D1, d2:D2) => P(d1, d2) }  // Resolve D1 and D2
  .bind[P].toProvider{ provider _ }                   // Use the given function as a provider
  .bind[P].toEagerSingletonProvider{ d1:D1 => P(d1) } // Create an eager singleton using the provider function
```

If you define multiple bindings to the same type (e.g., P), the last binding will have the highest precedence.


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

### Injected Instances Are Always Singletons

If you only need singletons (e.g.,`X`) and how to construct `X` is clear from its definition, no need exists to specify `bind[X].toSingleton` in your design:

```scala
import wvlet.airframe._

class X(y:Y)
class Y(z:Z)
case class Z(port:Int)

val design: Design =
  newDesign
    // Binding X and Y toSingleton is unnecessary as singleton binding is the default behavior.
    //.bind[X].toSingleton
    //.bind[Y].toSingleton
    .bind[Z].toInstance(Z(port = 8080))  // Z has no default instance, so we should bind it manually.
```

## Life Cycle

The lifecycle (including calling onInject, onStart, onShutdown hooks) of the injected instances will be managed by the session of Airframe DI. To properly release the resources injected by bindings, define these lifecycle hooks in the design or implement [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) interface. If the injected instance implements AutoCloseable, `def close(): Unit` method of AutoCloseable will be called when the session terminates. To override this behavior, define your own `onShutdown` hooks.

By default, all injections generates singleton objects that are alive until closing the current session. These singleton objects are managed inside the current session object.

Server side application often requires resource management (e.g., network connection, threads, etc.). Airframe DI has a built-in object life cycle manager to implement these hooks:

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

These life cycle hooks except `onInject` will be called only once.

### Eager Initialization of Singletons for Production

In production, initializing all of the singletons when starting the session is preferred. To use production mode, add `Design.withProductionMode` to your design:

```scala
// All singletons defined in the design will be initialized (i.e., onInit/onInject/onStart hooks will be called)
design
  .bind[X].to[XImpl]
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


## Session

`Session` is a placeholder of your singleton instances created from your Design:

```scala
val session = design.newSession
val a = session.build[A] { obj: A =>
  // Do something with obj
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

Session manages the life cycle of your objects and holds instances of singletons. The generated instances can be discarded after `session.shutdown` is called:

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
import wvlet.airframe.di.Session

class MyServer(session: Session) { // Bind the current session

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

You may need to find the current session to manage lifecycles of manually created instances. In this case, you can bind Airframe's Session by injecting `wvlet.airframe.Session`. You can register newly created instances to the session to manages their lifecycle with the current session.

```scala
import wvlet.airframe._

class MyDB(name:String) extends AutoCloseable {
  private val conn = newConnection(name)
  override def close(): Unit = { conn.close() }
}

class MyApp(session: Session) {
  def openDB(name:String): MyDB = {
    val db = new MyDB(name)
     // Adding MyDB instance to the current session so that
     // MyDB connection can be closed when the session terminates.
    session.register(db)
    db
  }
}
```


## Designing Applications with Airframe DI

When writing an application, these concerns below are often unrelated to the core application logic:

- How to build service objects.
- How to configure services.
- How to manage life cycle of service objects.

Airframe allows separating these concerns into `Design`. For example, when writing service A and B in the following figure, you should be able to focus only direct dependencies. In this example DBClient and FluentdLogger are the direct dependencies of A and B.

![image](https://wvlet.org/airframe/img/airframe/build-service-objects.png)

When building objects A and B, we usually need to think about the other indirect dependencies like ConnectionPool, HttpClient, DB, etc. While writing `class A(dbClient:DBClient, fluentdLogger:FluentdLogger)`, you don't need to care about its indirect dependencies.

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

class TaggedBinding(apple:Apple, banana:Banana)

 ```

Alias binding is useful to inject primitive type values:
```scala
import wvlet.airframe._

type Env = String

class MyService(env:Env, session: Session) {
  // Conditional binding
  lazy val threadManager = env match {
     case "test" => new TestingThreadManager(...) // prepare a testing thread manager
     case "production" => new ThreadManager(...)  // prepare a thread manager for production
  }
  session.register(threadManager)
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

If you want to switch a service to be called depending on the user input, you can just use Scala's functionality + Airframe DI. To illustrate this, consider building an web application that receives a request and returns a string message.
`Dispatcher` class receives an URL path and choose an appropriate `Handler` to use:

```scala
import wvlet.airframe._

trait Handler {
  def handle(request:Request): String
}

class DefaultHandler extends Handler {
  def handle(request:Request): String = "hello"
}

class InfoHandler extends Handler {
  def handle(rquest:Request): String = "info"
}

class Dispatcher(infoHandler:InfoHandler, defaultHandler:DefaultHanlder) {
  private val dispatcher: String => Handler = {
    case "info" => infoHandler
    case _ => defaultHandler
  }

  def dispatch(path:String, request:Request): String = {
     dispatcher(path).handle(request)
  }
}
```

In Google Guice, we need to use a special binder like [Multibinder](https://github.com/google/guice/wiki/Multibindings).
In Airframe, we just need to use a constructor of Scala. 


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

To check the runtime behavior of Airframe's dependency injection, set the log level of `wvlet.airframe.di` to `debug` or `trace`:

**src/main/resources/log.properties**
```
wvlet.airframe.di=debug
```

While debugging the code in your test cases, you can also use `log-test.properties` file:
**src/test/resources/log-test.properties**
```
wvlet.airframe.di=debug
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
import wvlet.airframe.di._

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

// bind configurations
class App(config: Config, server: Server) {
  def run = {
    sever.launch(config.host, config.port)
  }
}

// Create a new design and add configuration
val d =
  newDesign
  .bind[Server].to[YourServer]
  .bind[Config].toInstance(new Config("localhost", 8080))

// Start the application
d.build[App] { app =>
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

class DBService(dbConfig: DBConfig, connection: DBConnection) {
  def query(sql:String) = {
    connection.query(sql)
  }
}

// Using DBService. This class has no need to care about closing DB
// connection resources because DBService will take care of it.
class App(dbService: DBService) {
  dbService.query("select * from tbl")
}

// Your application launcher code
val d = newDesign
  .bind[DBService].toSingleton // To share the connection between classes
  .bind[DBConfig].toInstance(DBConfig("jdbc://...", "user name", ...))
  .bind[DbConnection]
  .onInit { c => c.connect(dbConfig.url, dbConfig.user, ... ) }
  .onShutdown {
    // This will be executed when session.shutdown is called
    c => c.close
  }

d.build[App] { app =>
  // db connection will be established here
}
// database connection will be closed automatically

```



## Airframe Internals

This page describes the internals of Airframe DI for developers who are interested in extending Airframe DI.

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

To manage life cycle of A and B, you eventually need to store the object references somewhere like this:

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

If your need to manage hundreds of services, manually writing such object management functions will be cumbersome. Airframe DI helps you to organize construction of service objects.


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
