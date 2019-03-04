airframe-http-recorder
===

airframe-http-recorder is an HTTP server for recording and replaying HTTP responses.
This is useful for testing HTTP server interactions in an environment with limited resources (e.g., CI servers) 

## Usage

```scala
import wvlet.airframe.http.recorder._
import wvlet.airframe.control.Control._

val recorderConfig = 
  HttpRecorderConfig(destUri = "https://wvlet.org", sessionName = "airframe")

// Create a proxy server for recording server responses
withResource(HttpRecorder.createRecordingServer(recorderConfig)) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will be recorded 
}

// Create a replay server that returns recorded responses for matching requests 
withResource(HttpRecorder.createReplayServer(recorderConfig)) { server =>
  server.start
  val addr = server.localAddress // "localhost:(port number)"
  // Requests to the local server will return the recorded responses 
}
```


## Related Projects
- https://github.com/vcr/vcr (for Ruby)
- https://github.com/betamaxteam/betamax (no longer maintained)
