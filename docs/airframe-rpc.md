---
id: airframe-rpc
title: Airframe RPC
---

[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

## Why Airframe RPC?

Airframe RPC 


## Introduction to Airframe RPC

## Overview

First, define your RPC service using regular Scala functions and case classes. By adding `@RPC` annotation to your trait or class, all public methods will be your RPC endpoints:

```scala
package hello.api.v1;
import wvlet.airframe.http._

// Model classes
case class Person(id:Int, name:String)

// RPC interface definition 
@RPC
trait MyService { 
  def hello(person:Person): String 
}
```

Next, implement the service interface: 

```scala
package hello.api.v1
import wvlet.airframe.http._

class MyServiceImpl extends MyService {
  def hello(person:Person): String = s"Hello ${person.name}!"
}
```

Start an RPC web server at http://localhost:8080. Airfarme RPC provides Finagle-based web server implementation:
```scala
// Create a Router   
val router = Router.add[MyServiceImpl]
  
// Starting a new RPC server.
Finagle
  .server
  .withRouter(router)
  .withPort(8080)                 
  .start { server =>
    server.waitForTermination
  }
```

To access the RPC server, we need to generate an RPC client from the RPC interface definition. sbt-airframe plugin  


[![maven central][central-badge]][central-link]

__plugins.sbt__
```scala
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % "(version)")
```

__build.sbt__
```scala
airframeHttpClients := Seq("hello.api.v1:sync")
```
It will generate `hello.api.v1.ServiceSyncClient` class,   


```scala
import hello.api.v1._

val client = new ServiceSyncClient(Http.client.newSyncClient("localhost:8080"))
client.myService.hello(Person(id=1, name="leo")) // Receives "Hello leo!"
```
