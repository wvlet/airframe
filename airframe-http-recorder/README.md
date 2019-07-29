airframe-http-recorder
===

airframe-http-recorder is an HTTP server for recording and replaying HTTP responses.
This is useful for testing HTTP server interactions in an environment with limited resources (e.g., CI servers) 

## Usage

**build.sbt**

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-http-recorder_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-http-recorder_2.12/)
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-http-recorder" %% (version)
```

### Record & Replay

```scala
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._

val recorderConfig = 
  HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe")

// Create a proxy server that will record responses for matching requests,
// and make actual requests the destination for non-recorded requests.
withResource(HttpRecorder.createRecorderProxy(recorderConfig)) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will be recorded
}


// Create a proxy server only for recording server responses
withResource(HttpRecorder.createRecordOnlyServer(recorderConfig)) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will be recorded
}

// Create a replay server that returns recorded responses for matching requests 
withResource(HttpRecorder.createReplayOnlyServer(recorderConfig)) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the recorded responses
}
```

### Programmable

```scala
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._
import com.twitter.finagle.http.{Request,Response}

val response = withResource(HttpRecorder.createProgrammableServer { recorder =>
  // Program server responses instead of recodring
  val request = Request("/index.html")
  val response = Response()
  response.setContentString("Hello World!")
  
  recorder.record(request, response)
  
}) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the programmed responses
}
```


## Related Projects
- [VCR](https://github.com/vcr/vcr) (for Ruby)
- [Betamax](https://github.com/betamaxteam/betamax) (no longer maintained)
