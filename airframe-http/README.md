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
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-finagle" %% (version)
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
  case class ServerInfo(version:String, ua:String)
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
    ServerInfo("1.0", request.userAgent)
  }

  // Returning Future[X] is also possible.
  // This style is convenient when you need to call another service that returns Future response.
  @Endpoint(method = HttpMethod.GET, path = "/info_f")
  def getInfoFuture(request: HttpRequest[Request]): Future[ServerInfo] = {
    Future.value(ServerInfo("1.0", request.userAgent))
  }
  
  // It is also possible to return a custom HTTP responses
  @EndPoint(method = HttpMethod.GET, path = "/custom_response")
  def customResponse: Response = {
    val response = Reponse()
    response.contentString = "hello airframe-http"
    response
  }
}
```

This `MyApi` defines these http end points:
```
GET  /v1/user/:name    returns {"name":"..."}
POST /v1/user          returns {"name":"..."}
GET  /v1/info          returns {"version":"1.0", "ua":"...."}
GET  /v1/info_f        returns {"version":"1.0", "ua":"...."}
```

Mapping between JSON values and Scala objects will be handled automatically.

## Starting A Finagle HTTP Server

To start a server, add airframe bindings based on `finagleDefaultDesign`:
```scala
import wvlet.airframe._
import wvlet.airframe.http.finagle._
import com.twitter.finagle.http.Request

// Define API routes. This will read all @Endpoint annotations in MyApi
// You can add more routes by using `.add[X]` method.
val router = Router.of[MyApi]

val design =
  finagleDefaultDesign
    // Configure port and routes
    .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = 8080, router = router))

design.build[FinagleServer] { server =>
  // Finagle http server will start here

  // To keep runing the server, run `server.waitServerTermination`:
  server.waitServerTermination
}
// The server will terminate here
```


## Customizing Finagle

It's possible to customize Finagle. For example, if you need to:
- Customize Finagle filters, or
- Start multiple Finagle HTTP servers with different configurations

see the examples [here](https://github.com/wvlet/airframe/blob/master/airframe-http-finagle/src/test/scala/wvlet/airframe/http/finagle/FinagleServerFactoryTest.scala)


### Adding Finagle Tracer

To customize Finagle server, extend FinagleServerFactory and define your own 
server factory.

```scala
trait CustomFinagleServerFactory extends FinagleServerFactory {
  override def initServer(server: Http.Server): Http.Server = {
    // Enable tracer for Finagle
    server.withTracer(ConsoleTracer)
  }
}

val design =
  finagleDefaultDesign
    // Configure port and routes
    .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = 8080, router = router))
    // Configure Finagle Server
    .bind[FinagleServerFactory].to[CustomFinagleServerFactory]

design.build[FinagleServer] { server => 
  // The server will start here
}
``` 


## Running Multiple Finagle Servers


```scala
import wvlet.airframe.http.finagle._

finagleDefaultDesign.build[FinagleServerFactory] { factory =>
 factory.newFinagleServer(FinagleServerConfig(port = 8080, router = router1))
 factory.newFinagleServer(FinagleServerConfig(port = 8081, router = router2))
 // Two finagle servers will start at port 8081 and 8081
}
// Two servers will be stopped after exiting the session
```
