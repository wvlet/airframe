---
layout: docs
title: Use Cases
---

# Use Cases

This page illustrates typical use cases of Airframe.

## Binding Configurations

Configuring applications is cumbersome because you need to think about how to pass configurations to your classes. With Airframe this process is simple; Just bind configuration objects to your classes:

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


## Reusing Bindings with Trait Mix-in

To reuse bindings, we can create XXXService traits and mix-in them to build a complex object:

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

trait ThreadPoolService {
  val threadPool = bindSingleton[ThreadPool]
}

trait App1 extends ThreadPoolService {
  threadPool.submit( ... )
}

// Reuse singleton ThreadPool here
trait App2 extends ThreadPoolService {}
  threadPool.submit( ... )
}

```

Using Scala traits is powerful to compose applications that depend on many services:

```
trait App
  extends ThreadPoolService
  with UserAuthenticationService
  with ...
{
   // Use threadPool, userAuthentication, ...

}

```

### Override Bindings

It is also possible to manually inject an instance implementation. This is useful for changing the behavior of objects for testing:
```scala
trait CustomPrinterMixin extends FortunePrinterMixin {
  // Manually inject an instance
  override val printer =
    new Printer {
      def print(s:String) = { Console.err.println(s) }  
    }
}
```
