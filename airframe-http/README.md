airframe-http
====

airframe-http is a library for creating HTTP web servers at ease.
**airframe-http-finagle** is an extention of airframe-http to use Finagle as a backend HTTP server.

- Blog article: [Airframe HTTP: Building Low-Friction Web Services Over Finagle](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)

# airframe-http-finagle

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-finagle_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-finagle_2.12/)
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-http_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-http_2.12)
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-finagle" % (version)
```

## Defining HTTP Endpoints

**MyApi.scala**
```scala
import com.twitter.finagle.http.Request
import com.twitter.util.Future
import wvlet.airframe.http.{Endpoint, HttpMethod, HttpRequest}


object MyApi {
  case class User(name: String)
  case class NewUserRequest(name:String)
  case class ServerInfo(version:String, ua:Option[String])
}

// [Optional] Specify a common prefix for all endpoints
@Endpoint(path="/v1")
trait MyApi {
  import MyApi._

  // Binding http path parameters (e.g., :name) to method arguments
  @Endpoint(method = HttpMethod.GET, path = "/user/:name")
  def getUser(name: String): User = User(name)

  // Receive a JSON request body {"user":"leo"} to generate NewUserRequest instance
  @Endpoint(method = HttpMethod.POST, path = "/user")
  def createNewUser(request:NewUserRequest): User = User(request.name)

  // To read http request headers, add a method argument of HttpRequest[Request] type
  @Endpoint(method = HttpMethod.GET, path = "/info")
  def getInfo(request: HttpRequest[Request]): ServerInfo = {
    ServerInfo("1.0", request.toRaw.userAgent)
  }

  // Returning Future[X] is also possible.
  // This style is convenient when you need to call another service that returns Future response.
  @Endpoint(method = HttpMethod.GET, path = "/info_f")
  def getInfoFuture(request: HttpRequest[Request]): Future[ServerInfo] = {
    Future.value(ServerInfo("1.0", request.toRaw.userAgent))
  }
  
  // It is also possible to return a custom HTTP responses 
  @EndPoint(method = HttpMethod.GET, path = "/custom_response")
  def customResponse: Response = {
    val response = Reponse()
    response.contentString = "hello airframe-http"
    response
  }

  import com.twitter.io.{Buf,Reader}
  // If you return a Reader, the response will be streamed (i.e., it uses less memory)
  @EndPoint(method = HttpMethod.GET, path = "/stream_response")
  def streamingResponse: Reader[User] = {
     Reader.fromSeq(Seq(User("leo"), User("yui")))
  }
}
```

This `MyApi` defines these http end points:
```
GET  /v1/user/:name    returns {"name":"..."}
POST /v1/user          returns {"name":"..."}
GET  /v1/info          returns {"version":"1.0", "ua":"...."}
GET  /v1/info_f        returns {"version":"1.0", "ua":"...."}
...
```

Mapping between JSON values and Scala objects will be handled automatically.

### Path Parameter Types


| pattern | description|  example |   input example | binding | 
|---------|------------|----------|-------------|-------|
| :arg  | single match | /v1/user/:id  |  /v1/user/1 | id = 1 |
| *arg  | tail match | /v1/entry/*key  | /v1/entry/config/version | key = config/version |

`*arg` can be used only at the end of the path.

### MessagePack Support

If an HTTP POST request has `Content-Type: application/x-msgpack` header, airframe-http
will read the content body of the request as [MessagePack](https://msgpack.org) data, and bind it to the method arguments using
[airframe-codec](https://wvlet.org/airframe/docs/airframe-codec.html),
which will manage data type conversions (e.g, from msgpack into Int or objects) automatically.

If an HTTP request has `Accept: application/x-msgpack` header, the response body will be
encoded with MessagePack format. This is useful for reducing the response size and
sending data to the client as is. For example, JSON cannot represent precise double values and binary data
without some transformations. With MessagePack, you can send the data to the client more naturally.

## Starting A Finagle HTTP Server

To start a server, create a finagle server design with `newFinagleServerDesign(router, port)`:
```scala
import wvlet.airframe._
import wvlet.airframe.http.finagle._
import com.twitter.finagle.http.Request

// Define API routes. This will read all @Endpoint annotations in MyApi
// You can add more routes by using `.add[X]` method.
val router = Router.add[MyApi]

val design = newFinagleServerDesign(port = 8080, router = router)

design.build[FinagleServer] { server =>
  // Finagle http server will start here

  // To keep running the server, run `server.waitServerTermination`:
  server.waitServerTermination
}
// The server will terminate here
```

## Customizing Finagle

To customize Finagle, use `Finagle.server.withXXX` methods. 

For example, you can:
- Add custom Tracer, StatsReceiver, etc.
- Add more advanced configurations using `.withServerInitializer(...)`
- Customize HTTP filters
- Start multiple Finagle HTTP servers with different configurations

See also the examples in [here](https://github.com/wvlet/airframe/blob/master/airframe-http-finagle/src/test/scala/wvlet/airframe/http/finagle/FinagleServerFactoryTest.scala)

To customize Finagle server, extend FinagleServerFactory and define your own 
server factory:

```scala
import wvlet.airframe.http.finagle._

val router = Router.add[MyApi]

val serverConfig = Finagle.server
  .withName("my server")
  .withRouter(router)
  .withPort(8080)
  // Enable tracer for Finagle
  .withTracer(ConsoleTracer)
  // Add your own Finagle specific customization here
  .withServerInitializer{ x: Server => x }
  .withStatsReceiver(...)
  // Add a custom MessageCodec mapping
  .withCustomCodec(Map(Surface.of[X] -> ...))

newFinagleServerDesign(serverConfig)
  .build[FinagleServer] { server:FinagleServer =>
  // The customized server will start here  
}
```


### Running Multiple Finagle Servers

Create a FinagleServerFactory, and call `newFinagleServer(FinagleServerConfig)`:
```scala
import wvlet.airframe.http.finagle._

// Use a design for not starting the default server:
finagleBaseDesign.build[FinagleServerFactory] { factory =>
 factory.newFinagleServer(FinagleServerConfig(name = "server1", port = 8080, router = router1))
 factory.newFinagleServer(FinagleServerConfig(name = "server2", port = 8081, router = router2))
 // Two finagle servers will start at port 8081 and 8081
}
// Two servers will be stopped after exiting the session
```

### Shutting Down Finagle Server

Closing the current Airframe session will terminate the finagle server as well:

```scala
import wvlet.airframe._
import wvlet.airframe.http._

trait YourApi {
   // Bind the current session
   private val session = bind[Session]

   @Endpoint(path="/v1/shutdown", method=HttpMethod.POST)
   def shutdown {
     // Closing the current session will terminate the FinagleServer too.
     session.shutdown
   }
}
```


