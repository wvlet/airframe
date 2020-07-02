---
id: airframe-http
title: airframe-http: Creating REST Service
---

airframe-http is a library for creating REST HTTP web servers at ease. airframe-http-finagle is an extension of airframe-http to use Finagle as a backend HTTP server.

- Blog article: [Airframe HTTP: Building Low-Friction Web Services Over Finagle](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-finagle_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-finagle_2.12/)
[![Scaladoc](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-http_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.wvlet.airframe/airframe-http_2.12)
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-finagle" % (version)
```

## Defining HTTP Endpoints

**MyApi.scala**
```scala
import com.twitter.finagle.http.{Request,Response}
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

  // It is also possible to receive backend server specific Request type
  @Endpoint(method = HttpMethod.GET, path = "/info2")
  def getInfo(request: Request): ServerInfo = {
    ServerInfo("1.0", request.userAgent)
  }

  // Returning Future[X] is also possible.
  // This style is convenient when you need to call another service that returns Future response.
  @Endpoint(method = HttpMethod.GET, path = "/info_f")
  def getInfoFuture(request: HttpMessage.Request): Future[ServerInfo] = {
    Future.value(ServerInfo("1.0", request.userAgent))
  }

  // It is also possible to return custom HTTP responses
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

If an HTTP POST request has `Content-Type: application/x-msgpack` header, airframe-http
will read the content body of the request as [MessagePack](https://msgpack.org) data, and bind it to the method arguments using
[airframe-codec](https://wvlet.org/airframe/docs/airframe-codec.html),
which will manage data type conversions (e.g, from msgpack into Int or objects) automatically.

If an HTTP request has `Accept: application/x-msgpack` header, the response body will be
encoded with MessagePack format. This is useful for reducing the response size and
sending data to the client as is. For example, JSON cannot represent precise double values and binary data
without some transformations. With MessagePack, you can send the data to the client more naturally.

## Starting A Finagle HTTP Server

To start a server, create a finagle server configuration with Finagle.server, and
```scala
import wvlet.airframe._
import wvlet.airframe.http.finagle._
import com.twitter.finagle.http.Request

// Define API routes. This will read all @Endpoint annotations in MyApi
// You can add more routes by using `.add[X]` method.
val router = Router.add[MyApi]

Finagle.server
  .withPort(8080)
  .withRouter(router)
  .start { server =>
    // Finagle http server will start here
    // To keep running the server, run `server.waitServerTermination`:
    server.waitServerTermination
  }
// The server will terminate here
```

## Customizing Finagle

To customize Finagle, use `Finagle.server.withXXX` methods.

For example, you can:
- Customize HTTP filters
- Start multiple Finagle HTTP servers with different configurations
- Add custom Tracer, StatsReceiver, etc.
- Add more advanced server configurations using `.withServerInitializer(...)`

```scala
import wvlet.airframe.http.finagle._

val router = Router.add[MyApi]

val server = Finagle.server
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

server.start { server =>
  // The customized server will start here
  server.waitServerTermination
}
```

See also the examples in [here](https://github.com/wvlet/airframe/blob/master/airframe-http-finagle/src/test/scala/wvlet/airframe/http/finagle/FinagleServerFactoryTest.scala)

## Integration with Airframe DI

By calling `.design`, you can get a design for Airframe DI:

```scala
val design = Finagle.server
 .withName("my-server")
 .withRouter(router)
 .withPort(8080)
 .design

design.build[FinagleServer] { server =>
   // A server will start here
}
// The server will terminate after exiting the session
```


### Running Multiple Finagle Servers

To run multiple HTTP servers, create a FinagleServerFactory and use `newFinagleServer(FinagleServerConfig)`:
```scala
import wvlet.airframe.http.finagle._

// Use a design for not starting the default server:
finagleBaseDesign.build[FinagleServerFactory] { factory =>
  val config1 = Finagle.server.withName("server1").withPort(8080).withRouter(router1)
  val config2 = Finagle.server.withName("server2").withPort(8081).withRouter(router2)

 factory.newFinagleServer(config1)
 factory.newFinagleServer(config2)
 // Two finagle servers will start at port 8081 and 8081
}
// Two servers will be stopped after exiting the session
```

## Shutting Down Finagle Server

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

## Static Content

To return static contents (e.g., html, image files, etc.), use `StaticContent.fromResource(resourceBasePath, relativePath)`. This method finds a file from your class paths (e.g., files in dependency jar files or resource files).

```scala
trait StaticContentServer {
  @Endpoint(path="/content/*path")
  def content(path:String) = StaticContent.fromResource(basePath = "/your/resource/package/path", path)
}
```

You can also add multiple resource paths or local directories to the search paths:

```scala
val sc = StaticContent
  .fromResource("/resource/path1")
  .fromResource("/resource/path2")
  .fromDirecotry("/path/to/directory")

sc(path) // Create an HTTP response
```

## Error Handling

To handle errors that happens during the request processing, return HttpServerException with a custom HttpStatus code.

```scala
import wvlet.airframe.http.Http

// This will return 403 http response to the client
throw Http.serverException(HttpStatus.Forbidden_403)
```

If the endpoint returns Future type, returning just `Future[Throwable]` (will produce 500 response code) or `Future[HttpServerException]` to customize the response code by yourself will also work.


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

Router supports nesting HTTP request filters (e.g., authentication, logging) before processing the final `@EndPoint` method:

```scala
import wvlet.airframe._
import wvlet.airframe.http._
import wvlet.airframe.http.finagle._

// Your endpoint definition
class MyApp {
  @Endpoint(method = HttpMethod.GET, path = "/user/:name")
  def getUser(name: String): User = User(name)
}

// Implement FinagleFilter (or HttpFilter[Req, Resp, F])
// to define a custom filter that will be applied before the endpoint processing.
object LoggingFilter extends FinagleFilter with LogSupport {
  def apply(request: Request, context: Context): Future[Response] = {
    info(s"${request.path} is accessed")
    // Call the child
    context(request)
  }
}

// Use .andThen[X] for nesting filters
Router
 .add(LoggingFilter)
 .andThen[MyApp]
```

### Thread-Local Storage

To pass data between filters and applications, you can use thread-local storage in the context:

```scala
object AuthLogFilter extends FinagleFilter with LogSupport {
  def apply(request: Request, context: FinagleContext): Future[Response] = {
    context(request).map { response =>
      // Read the thread-local parameter set in the context(request)
      context.getThreadLocal[String]("user_id").map { uid =>
        info(s"user_id: ${uid}")
      }
      response
    }
  }
}

object AuthFilter extends FinagleFilter {
  def apply(request: Request, context: FinagleContext): Future[Response] = {
    if(authorize(request)) {
      request.getParam("user_id").map { uid =>
        // Pass a thread-local parameter to the parent response handler
        context.setThreadLocal("user_id", uid)
      }
    }
    context(request)
  }
}


Router
  .add(AuthLogFilter)
  .andThen(AuthFilter)
  .andThen[MyApp]
```

Using local variables inside filters will not work because the request processing will happen when Future[X] is evaluated, so we must use thead-local parmeter holder, which will be prepared for each request call.


## Access Logs

airframe-http stores HTTP access logs at `log/http-access.json` by default in JSON format. When the log file becomes large, it will be compressed with gz and rotated automatically. 

The default logger will record request parameters, request headers (except Authorization headers), response parameters, and response headers.

Example JSON logs:
```json
{"time":1589319681,"event_time":"2020-05-12T14:41:21.567-0700","method":"GET","path":"/user/1","uri":"/user/1","request_size":0,"remote_host":"127.0.0.1","remote_port":52786,"host":"localhost:52785","connection":"Keep-Alive","user_agent":"okhttp/3.12.11","x_request_id":"10","content_length":"0","accept_encoding":"gzip","response_time_ms":714,"status_code":200,"status_code_name":"OK","response_content_type":"application/json;charset=utf-8"}
{"time":1589319681,"event_time":"2020-05-12T14:41:21.573-0700","method":"GET","path":"/user/info","uri":"/user/info?id=2&name=kai","query_string":"id=2&name=kai","request_size":0,"remote_host":"127.0.0.1","remote_port":52786,"host":"localhost:52785","connection":"Keep-Alive","user_agent":"okhttp/3.12.11","x_request_id":"10","content_length":"0","accept_encoding":"gzip","response_time_ms":921,"status_code":200,"status_code_name":"OK","response_content_type":"application/json;charset=utf-8"}
```

For most of the cases, using the default logger is sufficient. If necessary, you can customize the logging by using your own request/response loggers: 

```scala
import wvlet.airframe.http.finagle._

Finagle
  .server
  .withLoggingFilter {
     HttpAccessLogFilter
      .default
      .addRequestLogger(...)
      .addResponseLogger(...)
      // Remove unnecessary HTTP headers from logs
      .addExcludeHeaders(Set("X-XXX-Header"))
  }
```


### Reading Access Logs with Fluentd

The generated HTTP access log files can be processed in Fluentd. For example, if you want to store access logs to Treasure Data, add the following [in_tail](https://docs.fluentd.org/input/tail) fluentd configuration:

```
<source>
  @type tail
  # Your log file location and position file
  path     /var/log/http_access.json
  pos_file /var/log/td-agent/http_access.json.pos
  # [Optional] Append tags to the log (For using td-agent)  
  tag      td.(your database name).http_access
  format   json
  time_key time
</source>
```


# HTTP Clients

airframe-http has several HTTP client implementations (FinagleClient, OkHttp client, URLConnection client, etc.).    

## Simple Http Client (No extra dependency) 

```scala
import wvlet.airframe.http.Http 

// Creating a URLConnection based client 
val client = Http.client.newSyncClient("http://localhost:8080")
val response = client.sendSafe(Http.request("/v1/info")) 
// Use client.send(Request) for throwing HttpClientException upon 4xx, 5xx errors
```

Note: URLConnection-based client cannot send PATCH requests due to [a bug of JDK](https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch)


## Finagle Http Client
```scala
import wvlet.airframe.http.finagle.Finagle

// Asynchronous HTTP client backed by Finagle (Using twitter-util Future) 
Finagle.client.newClient("http://localhost:8080")

// a Finagle-based sync client 
Finagle.client.newSyncClient(host_name)   
```

## OkHttp Client


```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-okhttp" % (version)
```

```scala
// Create an OkHttp-based sync http client
OkHttpClient.newClient(host_name, config...)
```


