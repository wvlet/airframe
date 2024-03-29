---
id: airframe-http-recorder
title: airframe-http-recorder: Web Request/Response Recorder
---

airframe-http-recorder is an HTTP server for recording and replaying HTTP responses.
This is useful for testing HTTP server interactions in an environment with limited resources (e.g., CI servers) 

With HttpRecorder, you can:
- Create a proxy server to the actual server and record the responses automatically.
  - This reduces the burden of writing mock servers and dummy responses.
- Record custom HTTP request/response pairs. 
  - This is useful for simulating server failures (e.g., returning 5xx responses)
- Replay recorded responses, stored in SQLite databases.
  - This is useful for running HTTP server/client integration tests on CI, without actually accessing the real destination servers.

## Usage

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-recorder_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-recorder_2.12/)
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-recorder" %% (version)
```

### Record & Replay

```scala
import wvlet.airframe.control.Control._
import wvlet.airframe.http.{Http,HttpStatus}
import wvlet.airframe.http.recorder.{HttpRecorder,HttpRecorderConfig}
import wvlet.airframe.http.finagle.FinagleClient

// Create a proxy server that will record responses for matching requests,
// and make actual requests the destination for non-recorded requests.
val recorderConfig = HttpRecorderConfig(destUri = "https://www.google.com")
withResource(HttpRecorder.createRecorderProxy(recorderConfig)) { server =>

  // Create an HTTP client
  val client = Http.client.newSyncClient(server.localAddress)  // "localhost:(port number)"

  // Requests to the local server will be recorded
  val response = client.send(Http.GET("/"))

  // You can record your own dummy responses
  server.record(Http.GET("/dummy"), Http.response(HttpStatus.NotFound_404))
}


// Create a proxy server only for recording server responses
withResource(HttpRecorder.createRecordOnlyServer(recorderConfig)) { server =>
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will be recorded

  // If necessary, add custom request-response pairs
  server.recordIfNotExists(Http.GET("/dummy"), Http.response(HttpStatus.Ok_200))
}

// Create a replay server that returns recorded responses for matching requests 
withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the recorded responses
}

```

### Programmable HttpRecorderServer

```scala
import wvlet.airframe.http.Http
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._

val recorderConfig = HttpRecorderConfig(sessionName="my-recording")
val response = withResource(HttpRecorder.createProgrammableServer(recorderConfig)) { server =>
  // Add custom server responses
  val request = Http.GET("/index.html")
  val response = Http.response().withContent("Hello World!")
  server.record(request, response)

  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the programmed responses
}
```

If you don't need to persist your recoded responsoe (e.g., in unit tests), use
`HttpRecorder.createInMemoryProgrammableServer`. The recorded responses wiil be
discarded after closing the server.


### Customize Request Matcher

By default, http-recorder finds records that have the same method type (e.g., GET, POST, etc.), path, query string, request body, and HTTP headers. Some unstable HTTP headers like Date, User-Agent, etc. (defined in `defaultExcludeHeaderPrefixes`) will not be used for matching.

If this matching is too strict, we can use `PathOnlyMatcher` (checks only method types and uri) or your own implementation to compute hash values for matching requests:
```scala
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._

val config = HttpRecorderConfig(requestMatcher = HttpRequestMatcher.PathOnlyMatcher)
withResource(HttpRecorder.createInMemoryServer(config)) { server =>
  // 
}
```

## Related Projects

- [VCR](https://github.com/vcr/vcr) (for Ruby)
- [Betamax](https://github.com/betamaxteam/betamax) (no longer maintained)
