---
layout: docs
title: airframe-http
---
Airframe HTTP: A light weight web server
===
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-surface_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-http_2.12)

airframe-http is a library for creating HTTP web services at ease.
- **airframe-http-finagle**: Finagle as a backend HTTP server

# airframe-http-finagle

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-finagle_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-finagle_2.12/)
**build.sbt**
```
libraryDependencies += "org.wvlet.airframe" %% "airframe-finagle-http" %% AIRFRAME_VERSION
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
import wvlet.airframe.http.finagle._
import com.twitter.finagle.http.Request

// Define API routes. This will read all @Endpoint annotations in MyApi
// You can add more routes by using `.add[X]` method.
val router = Router.of[MyApi]

val design =
  finagleDefaultDesign
    // Register API impl
    .bind[MyApi].toSingleton
    // Register http routes
    .bind[Router].toInstance(router)
    // Configure port
    .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = 8080))

design.build[FinagleServer] { server =>
  // Finagle http server will start here

  // To keep runing the server, run `server.waitServerTermination`:
  server.waitServerTermination
}
// The server will terminate here
```


## Customizing Finagle

It's possible to customize web servers. For example, if you need to:
- Customize Finagle filters, or
- Start multiple Finagle HTTP servers

see the examples here:
https://github.com/wvlet/airframe/blob/master/airframe-http-finagle/src/test/scala/wvlet/airframe/http/finagle/FinagleServerFactoryTest.scala

