---
id: airframe-http
title: airframe-http: Creating REST Service
---

airframe-http is a library for creating REST HTTP web servers at ease. airframe-http-netty is an extension of airframe-http to use Netty as a backend HTTP server.

- Blog article: [Airframe HTTP: Building Low-Friction Web Services Over Finagle](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-netty_3/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-netty_3/)


```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-netty" % (version)
```

## Defining HTTP Endpoints

**MyApi.scala**
```scala

import wvlet.airframe.http.*
import wvlet.airframe.http.HttpMessage.{Request, Response}
import scala.concurrent.Future

object MyApi {
  case class User(name: String)
  case class NewUserRequest(name:String)
  case class ServerInfo(version:String, ua:Option[String])
}

// [Optional] Specify a common prefix for all endpoints
@Endpoint(path="/v1")
class MyApi {
  import MyApi._

  // Binding http path parameters (e.g., :name) to method arguments
  @Endpoint(method = HttpMethod.GET, path = "/user/:name")
  def getUser(name: String): User = User(name)

  // Receive a JSON request body {"user":"leo"} to generate NewUserRequest instance
  @Endpoint(method = HttpMethod.POST, path = "/user")
  def createNewUser(request:NewUserRequest): User = User(request.name)

  // To read http request headers, get it from RPCContext
  @Endpoint(method = HttpMethod.GET, path = "/info")
  def getInfo(): ServerInfo = {
    val request = RPCContext.current.httpRequest
    ServerInfo("1.0", request.userAgent)
  }

  // Returning Future[X] is also possible.
  // This style is convenient when you need to call another service that returns Future response.
  @Endpoint(method = HttpMethod.GET, path = "/info_f")
  def getInfoFuture(): Future[ServerInfo] = {
    val request = RPCContext.current.httpRequest
    Future.apply(ServerInfo("1.0", request.userAgent))
  }

  // It is also possible to return custom HTTP responses
  @EndPoint(method = HttpMethod.GET, path = "/custom_response")
  def customResponse: Response = {
    val response = Http.response().withContent("hello airframe-http")
    response
  }

  import com.twitter.io.{Buf,Reader}
  // [finagle-backend only] If you return a Reader, the response will be streamed (i.e., it uses less memory)
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
GET  /v1/info2         returns {"version":"1.0", "ua":"...."}
GET  /v1/info_f        returns {"version":"1.0", "ua":"...."}
...
```

Mapping between JSON values and Scala objects will be handled automatically.

### Path Parameter Types


 pattern | description|  example |   input example | binding |
---------|------------|----------|-------------|-------|
 :arg  | single match | /v1/user/:id  |  /v1/user/1 | id = 1 |
 *arg  | tail match | /v1/entry/*key  | /v1/entry/config/version | key = config/version |

`*arg` can be used only at the end of the path.

### MessagePack Support

If an HTTP POST request has `Content-Type: application/msgpack` header, airframe-http
will read the content body of the request as [MessagePack](https://msgpack.org) data, and bind it to the method arguments using
[airframe-codec](https://wvlet.org/airframe/docs/airframe-codec.html),
which will manage data type conversions (e.g, from msgpack into Int or objects) automatically.

If an HTTP request has `Accept: application/msgpack` header, the response body will be
encoded with MessagePack format. This is useful for reducing the response size and
sending data to the client as is. For example, JSON cannot represent precise double values and binary data
without some transformations. With MessagePack, you can send the data to the client more naturally.

## Starting A Netty HTTP Server

To start a server, create a netty server configuration with Netty.server, and
```scala
import wvlet.airframe.http.*
import wvlet.airframe.http.netty.Netty

// Define API routes. This will read all @Endpoint annotations in MyApi
// You can add more routes by using `.add[X]` method.
val router = RxRouter.of[MyApi]

Netty.server
  .withPort(8080)
  .withRouter(router)
  .start { server =>
    // Netty http server will start here
    // Keep running the server 
    server.awaitTermination
  }
```

## Customizing Netty

To customize Netty, use `Netty.server.withXXX` methods. For example, you can customize server name, port, logging, etc.:

```scala
import wvlet.airframe.http.*
improt wvlet.airframe.http.netty.Netty

val router = RxRouter.add[MyApi]

val server = Netty.server
  .withName("my server")
  .withRouter(router)
  .withPort(8080)
  // [optional] Add custom log entries
  .withExtraLogEntries { () => 
    val m = ListMap.newBuilder[String, Any]
    // Add a custom log entry
    m += "application_version" -> "1.0"
    // Add a thread-local parameter to the log
    RPCContext.current.getThreadLocal("user_id").map { uid =>
      m += "user_id" -> uid
    }
    m.result
  }
  // [optional] Disable server-side logging (log/http_server.json)
  .noLogging
  // [optional] Add a custom MessageCodec mapping
  .withCustomCodec{ case s: Surface.of[MyClass] => ... }

server.start { server =>
  // The customized server will start here
  server.waitServerTermination
}
```

## Integration with Airframe DI

By calling `.design`, you can get a design for Airframe DI:

```scala
val design = Netty.server
 .withName("my-server")
 .withRouter(router)
 .withPort(8080)
 .design

design.build[NettyServer] { server =>
   // A server will start here
   
   // Wait until the server is terminated
   server.waitServerTermination
}
// The server will terminate after exiting the session
```

### Running Multiple Netty Servers with Airframe DI

To run multiple HTTP servers with Airframe DI, wrapping NettyServer within different classes is recommended:

```scala
import wvlet.airframe.*
import wvlet.airframe.http.netty.Netty

class MyAppServer(server: NettyServer):
  export server.waitServerTermination

class AdminServer(server: NettyServer):
  export server.waitServerTermination

class MyService(myAppServer: MyAppServer, adminServer: AdminServer):
  def waitServerTermination: Unit = {
    myAppServer.waitServerTermination
    adminServer.waitServerTermination
  }

  // Explicitely stop the servers   
  def stop: Unit = {
    myAppServer.stop
    adminServer.stop
  }

case class ServiceConfig(port:Int, adminPort:Int)

val design = newDesign
  .bind[ServiceConfig].toInstance(ServiceConfig(8080, 8081))  
  .bind[MyAppServer].toProvider { (config: ServiceConfig, session: Session) =>
    Netty.server
     .withName("myapp")
     .withRouter(router1)
     .withPort(config.port) // port 8080
     .newServer(session)
  }
  .bind[AdminServer].toProvider { (config: ServiceConfig, session: Session) =>
    Netty.server
     .withName("admin")
     .withRouter(router2)
     .withPort(config.adminPort) // port 8081
     .newServer(session)
  }
  

design.build[MyService] { service =>
  // Two servers will start here
  
  // Await the MyApp server termination
  service.waitServerTermination
}

// After existing the scope, the servers will be stopped automatically (via AutoCloseable.close() method).

```

## Static Content

To return static contents (e.g., html, image files, etc.), use `StaticContent.fromResource(resourceBasePath, relativePath)`. This method finds a file from your class paths (e.g., files in dependency jar files or resource files).

```scala
import wvlet.airframe.http.*

class StaticContentServer {
  @Endpoint(path="/content/*path")
  def content(path:String) = StaticContent.fromResource(basePath = "/your/resource/package/path", path)
}
```

You can also add multiple resource paths or local directories to the search paths:

```scala
val sc = StaticContent
  .fromResource("/resource/path1")
  .fromResource("/resource/path2")
  .fromDirectory("/path/to/directory")

sc(path) // Create an HTTP response
```


## Reporting Errors

To report server-side errors, you can throw HttpServerException with a custom HttpStatus code.

```scala
import wvlet.airframe.http.Http

// This will return 403 http response to the client
throw Http.serverException(HttpStatus.Forbidden_403)
```

If the endpoint returns Future type, returning just `Future[Throwable]` (will produce 500 response code) or `Future[HttpServerException]` to customize the response code by yourself will also work.

Throwing an RPCStatus as an exception will also work. An appropriate HTTP status code will be set automatically:

```scala
throw RPCStatus.INVALID_REQUEST_U1.newException("Unexpected message")
```

### Returning Custom Error Responses

To return JSON or MsgPack responses, use `Http.serverException(request, status, object, (codec factory)?)`:

```scala
import wvlet.airframe.http.Http

case class ErrorResponse(code:Int, message:String)

// The error response object will be converted into JSON by using airframe-codec:
throw Http.serverException(request, HttpStatus.Forbidden_403, ErrorResponse(100, "forbidden"))

// This will return {"code":100,"message":"forbidden"}
```

If the input request has `Accept: application/x-msgpack` header, the same code will translate the object into MessagePack format.

To fully customize the error response, use `.withXXX` methods:

```scala
val e = Http.serverException(HttpStatus.BadRequest_400)
    .withHeader(...)
    .withJson(...)

throw e
```

## Filters

Router supports nesting HTTP request filters (RxHttpFilter) for authentication, logging, etc. before processing the final `@EndPoint` method:

```scala
import wvlet.airframe.*
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx

// Your endpoint definition
class MyApp {
  @Endpoint(method = HttpMethod.GET, path = "/user/:name")
  def getUser(name: String): User = User(name)
}

// Implement RxHttpFilter to define a custom filter 
// that will be applied before the endpoint processing.

class AuthFilter extends RxHttpFilter {
  def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
    if (isValidAuth(request.authorization)) {
      // Call the next filter chain
      next(request)
    }
    else {
      // Reject the request
      throw RPCStatus.UNAUTHENTICATED_U13.newException("Invalid user")
    }
  }
}

// Add a filter and chain to the endpoint .andThen[X]:
val router = RxRouter
 .filter[AuthFilter]
 .andThen[MyApp]
```

### Thread-Local Storage

To pass data between filters and applications, you can use thread-local storage in the context:

```scala
object AuthFilter extends RxHttpFilter {
  def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
    if(authorize(request)) {
      request.getParam("user_id").map { uid =>
        // Pass a thread-local parameter to the parent response handler
        RPCContext.current.setThreadLocal("user_id", uid)
      }
    }
    next(request)
  }
}

object AuthLogFilter extends RxHttpFilter with LogSupport {
  def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
    next(request).map { response =>
      // Read the thread-local parameter set in the context(request)
      RPCContext.current.getThreadLocal("user_id").map { uid =>
        info(s"user_id: ${uid}")
      }
      response
    }
  }
}


val router = RxRouter
  .filter(AuthLogFilter)
  .andThen(AuthFilter)
  .andThen[MyApp]
```

Using local variables inside filters will not work because the request processing will happen when Future[X] is evaluated, so we must use thead-local parmeter holder, which will be prepared for each request call.


## Access Logs

airframe-http stores HTTP access logs at `log/http-server.json` by default in JSON format. When the log file becomes large, it will be compressed with gz and rotated automatically.

The default logger will record request parameters, request headers (except Authorization headers), response parameters, and response headers.

Example JSON logs:
```json
{"time":1589319681,"event_time":"2020-05-12T14:41:21.567-0700","method":"GET","path":"/user/1","uri":"/user/1","request_size":0,"remote_host":"127.0.0.1","remote_port":52786,"host":"localhost:52785","connection":"Keep-Alive","user_agent":"okhttp/3.12.11","x_request_id":"10","content_length":"0","accept_encoding":"gzip","response_time_ms":714,"status_code":200,"status_code_name":"OK","response_content_type":"application/json;charset=utf-8"}
{"time":1589319681,"event_time":"2020-05-12T14:41:21.573-0700","method":"GET","path":"/user/info","uri":"/user/info?id=2&name=kai","query_string":"id=2&name=kai","request_size":0,"remote_host":"127.0.0.1","remote_port":52786,"host":"localhost:52785","connection":"Keep-Alive","user_agent":"okhttp/3.12.11","x_request_id":"10","content_length":"0","accept_encoding":"gzip","response_time_ms":921,"status_code":200,"status_code_name":"OK","response_content_type":"application/json;charset=utf-8"}
```

For most of the cases, using the default logger is sufficient. If necessary, you can customize the logging by using your own request/response loggers:

```scala
import wvlet.airframe.http.netty.*

Netty
  .server
  .withHttpLoggerConfig {
    _.withLogFilter { (m: Map[String, Any]) =>
      // You can customize the log entries here
      m
    }
  }
```


### Reading Access Logs with Fluentd

The generated HTTP access log files can be processed in Fluentd. For example, if you want to store access logs to Treasure Data, add the following [in_tail](https://docs.fluentd.org/input/tail) fluentd configuration:

```
<source>
  @type tail
  # Your log file location and position file
  path     /var/log/http_server.json
  pos_file /var/log/td-agent/http_server.json.pos
  # [Optional] Append tags to the log (For using td-agent)
  tag      td.(your database name).http_access
  format   json
  time_key time
</source>
```


# HTTP Clients

airframe-http has several HTTP client implementations (Java's http client, OkHttp client, URLConnection client, etc.).
The http client has a built-in retry logic (for 5xx http status code, connection failures) and circuit breakers. 

## Default Http Client 

```scala
import wvlet.airframe.http._
import wvlet.airframe.http.HttpMessage.{Request,Response}

// Creating a default HTTP client
val client = Http.client.newSyncClient("http://localhost:8080")

// Send a new GET request
// Note: Use client.send(Http.xxx) for throwing HttpClientException upon 4xx, 5xx errors
val response = client.sendSafe(Http.GET("/v1/info"))

// Simple a request and receives a raw http response. You can customize headers too:
val r: Response = client.send(Http.GET("/path").withHeader(....))

// Send POST request with JSON body and read the response as an MyObj object
case class MyObj(id: Int, name: String)
val myobj = client.readAs[MyObj](Http.POST("/path2").withJson(...))

// Send an object data as the request body, and receive the response as ResponseType object
// JSON/MessagePack data will be transformed internally
val resp: ResponseType = client.call[RequestType, ResponseType](Http.POST("/path3"), requestDataObj) 

```

## Customizing HTTP clients

You can customize various configuration of the http clients:
```scala
Http.client
  // Add an HTTP header
  .withRequestFilter(_.withAuthorization("Bearer xxx"))
  // Change the number of retry
  .withRetryContext(_.withMaxRetry(10)) 
  // Set a connection timeout
  .withConnectionTimeout(Duration(60, TimeUnit.SECONDS))
  .newSyncClient("http://localhost:8080")
```

## Rx-based async client

An async-http client is a new addition since 23.5.0 and is useful when chaining responses from remote servers or rendering DOM in Scala.js using RPC responses.

```scala
val client = Http.client.newAsyncClient("https://...") 
val rx = client.send(Http.GET("...")) // Returns Rx[HttpMessage.Response]
rx.toRxStream.map(x => x.contentString) // Returns Rx[String]
```

`Rx[A]` value will not trigger any execution until it is evaluated by the other framework (e.g., airframe-http RPC, airframe-rx-http, AirSpec test runner, etc.)

In case you need to explicitly extract a value from the response, you can use `rx.run { event => ...}` 

### URLConnection client for Java 8 

For compatibility with Java 8, you can use URLConnection-based client:
```scala
import wvlet.airframe.http.Http

val client = Http.client
  .withBackend(URLConnectionClientBackend)
  .newSyncClient("http://localhost:8080")
```
Note: URLConnection-based client cannot send PATCH requests due to [a bug of JDK](https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch)

## OkHttp Client

```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-okhttp" % (version)
```

```scala
import wvlet.airframe.http.okhttp.OkHttp
// Create an OkHttp-based sync http client
val client = OkHttp.client.newSyncClient(host_name)
```


## Finagle Http Client

(deprecated. Use Http.client instead)

```scala
import wvlet.airframe.http.finagle.Finagle

// Asynchronous HTTP client backed by Finagle (Using twitter-util Future)
Finagle.client.newClient("http://localhost:8080")

// a Finagle-based sync client
Finagle.client.newSyncClient(host_name)
```

