---
id: airframe-rpc
title: Airframe RPC
---

[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22

Airframe RPC is a framework for building RPC services by using Scala as a unified RPC interface between servers and clients.

![overview](../img/airframe-rpc/rpc-overview.png)

## Why Airframe RPC?

Airframe RPC enables calling Scala methods running at remote servers. You don't need to worry about how to encode your data into JSON, nor how to define HTTP REST endpoints. Airframe RPC abstracts away these details, and generates code for serializing your data objects and calls appropriate HTTP endpoints. 

While [gRPC](https://grpc.io/) has been a popular approach for building RPC services, it's built around [Protobuf](https://developers.google.com/protocol-buffers/docs/overview) technology for defining data structures and RPC methods. This means, you and your collaborators need to use Protobuf ecosystem almost for everything to benefit from gRPC. And also, gRPC heavily uses HTTP/2 features, some of them are not supported in web browsers, so if you need to write web applications using gRPC, an additional proxy like [gRPC-Web](https://grpc.io/docs/languages/web/basics/) is required.

In 2020, [Scala.js finally became 1.0.0 after 7 years of development](https://www.scala-js.org/news/2020/02/25/announcing-scalajs-1.0.0/), which can compile Scala code into JavScript. This paved a way for using Scala both for server (JVM) and client (JavaScript) implementations. We explored an approach for using Scala's functional interfaces as RPC endpoint definitions, and successfully created Airframe RPC.

Although Airframe RPC is a relatively new project started at January 2020 inside [Arm Treasure Data](https://www.treasuredata.com/), this project has proved various advantages, for example:

- __Good-bye to REST__. We can just use Scala's functional interface for defining servers. [Google's REST API Design Guide](https://cloud.google.com/apis/design) has been useful resources for defining clear API endpoints, but we've found using programming language's native interface is much easier. 
- __No more web-framework wars__. In Scala, there are many web frameworks, such as [Finatra](https://github.com/twitter/finatra), [Finch](https://github.com/finagle/finch), [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html), and our own [airframe-http](airframe-http.md), etc. Each of them has its own pros and cons, and choosing one of them has been a hard choice for us. Now, we can just start from Airframe RPC because the interface is not different from plain Scala. If necessary, we can use airframe-http for adding custom HTTP endpoints.
- __Seamless integration with Scala.js__. Writing web browser applications in JavaScript that interact with servers is not easy. You may need to learn [React.js](https://https://reactjs.org/), [Vue.js](https://vuejs.org), and a lot of others.    


## Introduction to Airframe RPC

First, define your RPC service interface using regular Scala functions and case classes. By adding `@RPC` annotation to your class, all public methods will be your RPC endpoints:

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
  override def hello(person:Person): String = s"Hello ${person.name} (id=${person.id})!"
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

To access the RPC server, we need to generate an RPC client from the RPC interface definition. 
[sbt-airframe](#sbt-airframe-plugin) will generate `hello.api.v1.ServiceSyncClient` class by reading the RPC interface.

```scala
import hello.api.v1._

// Create an RPC client 
val client = new ServiceSyncClient(Http.client.newSyncClient("localhost:8080"))

// Your first RPC call!
client.myService.hello(Person(id=1, name="leo")) // "Hello leo (id=1)!"
```

## Usage

### sbt-airframe plugin

sbt-airframe plugins supports generating HTTP clients. This supports generating async, sync, or Scala.js HTTP clients for making 
RPC calls.   

[![maven central][central-badge]][central-link]

__plugins.sbt__
```scala
addSbtPlugin("org.wvlet.airframe" % "sbt-airframe" % "(version)")
```

`airframeHttpClients` setting is necessary for specifying which API package to use for generating RPC clients. 
The format is `<RPC package name>:<client type>(:<target package name>)?`: 

__build.sbt__
```scala
airframeHttpClients := Seq("hello.api.v1:sync")
```

Supported client types are:
- __sync__: Create a sync HTTP client (ServiceSyncClient) for Scala (JVM)
- __async__: Create an async HTTP client (ServiceClient) for Scala (JVM) using Future abstraction (`F`). The `F` can be `scala.concurrent.Future` or twitter-util's Future. 
- __scalajs__:  Create an RPC client (ServiceClientJS)

To support other types of clients, see the examples of [HTTP code generators](https://github.com/wvlet/airframe/blob/master/airframe-http/.jvm/src/main/scala/wvlet/airframe/http/codegen/client/ScalaHttpClient.scala). This reads Route definition of RPC interfaces, and generate client code for calling RPC endpoints. Currently, we only supports generating HTTP clients for Scala. In near future, we would like to add Open API spec generator so that many programming languages can be used with Airframe RPC.

The generated client code can be found in `target/scala-2.12/src_managed/(api package)/` folder. 


__sbt-airframe tasks__
```scala
> airframeHttpReload           # Regenerate the generated client code. Use this if RPC interface has changed   
> airframeHttpGenerateClients  # Generating RPC clients manually
> airframeHttpClean            # Clean the generated code
```

### RPC Logging

Airframe RPC stores HTTP access logs to `log/http-access.json` by default. This json logs contains 
HTTP request related parameters and RPC-specific fields:

- __rpc_interface__: RPC interface class name
- __rpc_class__: The atual RPC implementation class name
- __rpc_method__: The RPC method name
- __rpc_args__: The RPC call argument parameters described in JSON

These parameters can be used for debugging your RPC requests.


See also [airframe-http: Access Logs](airframe-http.md#access-logs) for more details.


### RPC Filters

Airframe RPC can chain arbitrary HTTP request filters before processing HTTP requests.
Most typical use cases would be adding an authentication filter for RPC calls:
 

```scala
import wvlet.airframe.http._
import wvlet.ariframe.http.finagle._

object AuthFilter extends FinagleFilter with LogSupport {
  def apply(request: Request, context: Context): Future[Response] = {
    val auth = request.authorization
    if(isValidAuth(auth)) {
      // Call the next filter chain
      context(request)
    }
    else {
      // Reject the request
      Future.value(Response(Version.Http11, Status.Forbidden))
    }
  }
}
```


```scala
// Router for RPC
val rpcRouter = Router.add[MyApp] 

// Add a filter before processing RPC requests
val router = Router
  .add(AuthFilter)
  .andThen(rpcRouterr)
```

### DI Integration

Airframe RPC natively supports [Airframe DI](airframe.md) for dependency injection so that you can inject 
necessary components for running your web service using `bind[X]` syntax or constructor injection. 
This is useful when building web applications that requires many components and if you need to decouple 
component implementation from the service implementation. Airframe DI also supports switching component implementations 
between production and tests. 

```scala
import wvlet.airframe._

trait MyAPIImpl extends MyAPI {
  // Inject your component
  private val myService = bind[MyService]
  
  override def hello(...) = ...
}

val router = Router.add[MyAPIImpl]

// Define the component implementation to use
val design = newDesign
  .bind[MyService].toInstance(new MyServiceImpl(...))
  .add(Finagle.server.withRouter(router).design)

// Launch a Finagle Server
design.build[FinagleServer] { server =>
  server.waitForTermination
}
```


## RPC Internals 

### RPC Protocol

HTTP Requests and Responses

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
  - ex. `POST /hello.api.v1.MyService/hello`
- __Content-Type__: `application/json` (default) or `application/x-msgpack`
- __Request body__: JSON (or MessagePack) representation of the method arguments. Each method parameter names and arguments need to be a key-value pair in the JSON object. 
  - For an RPC method `def m(p1:T1, p2:T2, ...)`, the request body will have the structrure of `{"p1":(json representation of T1), "p2":(json representation of T2}, ...}`. For example, the request to the above `hello(request:HelloRequest)` method will require the following JSON body:
```json
{"request":{"name":"leo"}}
```
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

### Receiving Raw HTTP Responses

If you need to manage HTTP request specific parameters (e.g., HTTP headers), you can add request parameter.

```scala
import wvlet.airframe.http._
import wvlet.airframe.http.HttpMessage.{Request, Respone}

@RPC
trait MyAPI {
  def rpc1(p1:String, p2:Int, request:Request): Response
}
```
