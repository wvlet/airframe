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

First, define your RPC service using regular Scala functions and case classes. By adding `@RPC` annotation to your class, all public methods will be your RPC endpoints:

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
  def hello(person:Person): String = s"Hello ${person.name} (id=${person.id})!"
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
client.myService.hello(Person(id=1, name="leo")) // Receives "Hello leo (id=1)!"
```


## Protocol


### HTTP Requests and Responses

Airframe RPC maps function calls to HTTP POST requests. Let's see how RPC calls will be translted into HTTP requests using the following RPC interface example:

```scala
package hello.api.v1
@RPC
trait MyService { 
  def hello(request:HelloRequest): HelloResponse 
}

case class HelloRequest(name:String)
case class HelloResponse(message:String) 
```

- __Method__: POST
- __Path__: `/(package name).(RPC interface name)/(method name)`
  - `POST /hello.api.v1.MyService/hello`
- __Content-Type__: `application/json` (default) or `application/x-msgpack`
- __Request body__: The request data structure for calling a method `def m(p1:T1, p2:T2, ...)` will be `{"p1":(json representation of T1), "p2":(json representation of T2}, ...}`. For example, the request to the above `hello(request:HelloRequest)` method requires the following JSON body:
```json
{"request":{"name":"leo"}}
```
  - If `Content-Type: application/x-msgpack` is used, MessagePack representation of the above json data can be used for efficiency.
- __Accept__: "application/json" (default) or "application/x-msgpack"
- __Response body__: JSON (or MessagePack) representation of the method return type: 
```json
{"message":"..."}
```

### Object Serialization

Airframe RPC uses [schema-on-read codec of airframe-codec](airframe-codec.md) for object-json serialization. Even if the data type is slightly different from the target type, for example, the input data is "100", but the target type is Int, the input data will be automatically converted to the target type. 

Airframe RPC supports almost all commonly used Scala data types:

- Primitive types (Int, Long, String, Double, Float, Boolean, etc), and UUID. 
- DateTime representation: java.time.Instant (recommended because it can be used for Scala.js too)
  - (JVM only) ZonedDataTime, java.util.Date. These types cannot be used in Scala.js.
- Collection types: Seq, IndexedSeq, List, Set, Array, Map, Tuple (up to 21 parameters), Option, Either. 
- Exception, Throwable types will be serialized as GenericException.
- [airframe-metrics](airframe-metrics.md) types: ElapsedTime, DataSize, Count, etc.
- Raw Json, JSONValue, MsgPack values.
- Enum-like case object class, which has `object X { def unapply(s:String): Option[X] }` definition. String representation of enum-like classes will be used. Scala's native Enumeration classes are not supported.  

