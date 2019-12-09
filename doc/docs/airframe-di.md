---
layout: docs
title: Dependency Injection with Airframe DI
---



# Advanced Binding Types

## Generic Type Binding

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

## Type Alias Binding

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

## Multi-Binding

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
  
  def dispatch(path:String, request:Request): String =  {
     dispatcher(path).handle(request)
  }
}
```

In Google Guice, we need to use a special binder like [Multibinder](https://github.com/google/guice/wiki/Multibindings). 
In Airframe, we just need to write a Scala code that uses `bind[X]`. 

## Tagged Type Binding

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

# Use Cases

This page illustrates typical use cases of Airframe.

- [Configuring Applications](#configuring-applications)
- [Managing Resources](#managing-resources)
- [Factory Binding](#factory-binding)
- [Service Mix-In](#service-mix-in)
- [Override Bindings](#override-bindings)

## Configuring Applications

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

Airframe has a submodule called [airframe-config](airframe-config.html), which is useful for 
configuring your applications with YAML files.

## Managing Resources

Airframe makes easier managing resources (e.g., network or database connections, thread managers, etc.) For example, if you are writing an application that requires an access to a database service. You need to establish a connection, and also need to properly close the connection after the application terminates. Airframe support such resource management using [Life Cycle](lifecicle.html) triggers (onInit, onStart, onShutdown):

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

## Factory Binding

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


## Service Mix-In

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
      def run { body }
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

# Airframe Internals

This page describes the internals of Airframe for developers who are interested in extending Airframe.

## Session 

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
As we have seen in the example of [Service Mix-in](use-cases.html#service-mix-in), if we need to manage hundreds of services,
manually writing such object management functions will be cumbersome. Airframe helps you to oraganize building service objects. 


## Instantiation Methods

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


## Suface

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
