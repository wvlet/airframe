---
layout: docs
title: Use Cases
---

# Use Cases

This page illustrates typical use cases of Airframe.

- [Configuring Applications](#configuring-applications)
- [Managing Resources](#managing-resources)
- [Composing Services](#composing-services)

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


## Composing Services (Service Mix-in)

A traditional way of building applications is passing necessary servies to a main class:
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
trait CustomApp extends App {
  // Manually inject an instance
  override val userAuth = new MockUserAuth { ... }
}
```



