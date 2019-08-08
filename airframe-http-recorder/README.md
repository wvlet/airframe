airframe-http-recorder
===

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
import wvlet.airframe.http.recorder.{HttpRecorder,HttpRecorderConfig}
import wvlet.airframe.http.finagle.FinagleClient
import com.twitter.finagle.http.Status
import com.twitter.finagle.http.{Request,Response}

// Create a proxy server that will record responses for matching requests,
// and make actual requests the destination for non-recorded requests.
val recorderConfig = HttpRecorderConfig(destUri = "https://www.google.com")
withResource(HttpRecorder.createRecorderProxy(recorderConfig)) { server =>

  // Create an HTTP client. This example uses Finagle's http client implementation
  val client = FinagleClient.newSyncClient(server.localAddress)  // "localhost:(port number)"

  // Requests to the local server will be recorded
  val response = client.send(Request("/"))

  // You can record your own dummy responses
  server.record(Request("/dummy"), Response(Status.NotFound))
}


// Create a proxy server only for recording server responses
withResource(HttpRecorder.createRecordOnlyServer(recorderConfig)) { server =>
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will be recorded

  // If necessary, add custom request-response pairs
  server.recordIfNotExists(Request("/dummy"), Response(Status.Ok))
}

// Create a replay server that returns recorded responses for matching requests 
withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the recorded responses
}

```

### Programmable HttpRecorderServer

```scala
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._
import com.twitter.finagle.http.{Request,Response}

val recorderConfig = HttpRecorderConfig(sessionName="my-recording")
val response = withResource(HttpRecorder.createProgrammableServer(recorderConfig)) { server =>
  // Add custom server responses
  val request = Request("/index.html")
  val response = Response()
  response.setContentString("Hello World!")
  server.record(request, response)

  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the programmed responses
}
```

If you don't need to persist your recoded responsoe (e.g., in unit tests), use
`HttpRecorder.createInMemoryProgrammableServer`. The recorded responses wiil be
discarded after closing the server.


## Related Projects
- [VCR](https://github.com/vcr/vcr) (for Ruby)
- [Betamax](https://github.com/betamaxteam/betamax) (no longer maintained)
