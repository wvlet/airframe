/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.http.netty

import wvlet.airframe.http.HttpLogger.InMemoryHttpLogger
import wvlet.airframe.http.{
  Endpoint,
  Http,
  HttpLogger,
  HttpMethod,
  HttpServer,
  RxRouter,
  ServerSentEvent,
  ServerSentEventHandler
}
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.rx.{OnError, Rx, RxBlockingQueue, RxRunner}
import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit

object SSEStreamLoggingTest extends AirSpec {

  private def waitForLogs(logger: InMemoryHttpLogger, timeout: Long = 5000): Unit = {
    val deadline = System.currentTimeMillis() + timeout
    while (logger.getLogs.isEmpty && System.currentTimeMillis() < deadline) {
      Thread.sleep(10)
    }
  }

  class StreamLoggingApi {
    @Endpoint(method = HttpMethod.POST, path = "/v1/sse-stream")
    def sseStream(): Rx[ServerSentEvent] = {
      val queue = new RxBlockingQueue[ServerSentEvent]()
      new Thread(new Runnable {
        override def run(): Unit = {
          queue.put(ServerSentEvent(data = "event1"))
          queue.put(ServerSentEvent(data = "event2"))
          queue.put(ServerSentEvent(data = "event3"))
          queue.stop()
        }
      }).start()
      queue
    }

    @Endpoint(method = HttpMethod.POST, path = "/v1/sse-error-stream")
    def sseErrorStream(): Rx[ServerSentEvent] = {
      val queue = new RxBlockingQueue[ServerSentEvent]()
      new Thread(new Runnable {
        override def run(): Unit = {
          queue.put(ServerSentEvent(data = "event1"))
          Thread.sleep(20)
          queue.add(OnError(new RuntimeException("stream processing failed")))
        }
      }).start()
      queue
    }
  }

  private var streamLogger: InMemoryHttpLogger = null

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[StreamLoggingApi])
        .withName("stream-log-test")
        .withHttpLogger(HttpLogger.emptyLogger(_))
        .withHttpStreamLogger { config =>
          streamLogger = new InMemoryHttpLogger(config)
          streamLogger
        }
        .design
        .bind[AsyncClient].toProvider { (server: HttpServer) =>
          Http.client.newAsyncClient(server.localAddress)
        }
    )
  }

  test("log SSE stream completion") { (client: AsyncClient) =>
    streamLogger.clear()
    val eventQueue = new RxBlockingQueue[ServerSentEvent]()
    val rx = client.send(
      Http
        .POST("/v1/sse-stream")
        .withEventHandler(new ServerSentEventHandler {
          override def onError(e: Throwable): Unit       = { eventQueue.stop() }
          override def onCompletion(): Unit              = { eventQueue.stop() }
          override def onEvent(e: ServerSentEvent): Unit = { eventQueue.put(e) }
        })
    )

    rx.map { resp =>
      resp.statusCode shouldBe 200
      // Wait for all events to be received
      val events = eventQueue.toSeq.toList
      events.size shouldBe 3

      // Wait for the stream log to be written on the server side
      waitForLogs(streamLogger)

      val logs = streamLogger.getLogs
      logs.size shouldBe 1
      val log = logs.head
      debug(log)

      log("method") shouldBe "POST"
      log("path") shouldBe "/v1/sse-stream"
      log("status_code") shouldBe 200
      log("status_code_name") shouldBe "OK"
      log("event_count") shouldBe 3
      log.contains("start_time_ms") shouldBe true
      log.contains("duration_ms") shouldBe true
      log.contains("server_name") shouldBe true
      log.contains("error_message") shouldBe false
    }
  }

  test("log SSE stream error") { (client: AsyncClient) =>
    streamLogger.clear()
    val eventQueue = new RxBlockingQueue[ServerSentEvent]()
    val rx = client.send(
      Http
        .POST("/v1/sse-error-stream")
        .withEventHandler(new ServerSentEventHandler {
          override def onError(e: Throwable): Unit       = { eventQueue.stop() }
          override def onCompletion(): Unit              = { eventQueue.stop() }
          override def onEvent(e: ServerSentEvent): Unit = { eventQueue.put(e) }
        })
    )

    rx.map { resp =>
      // Wait for events (stream will error after 1 event)
      val events = eventQueue.toSeq.toList
      events.size shouldBe 1

      // Wait for the stream log to be written on the server side
      waitForLogs(streamLogger)

      val logs = streamLogger.getLogs
      logs.size shouldBe 1
      val log = logs.head
      debug(log)

      log("method") shouldBe "POST"
      log("path") shouldBe "/v1/sse-error-stream"
      log("status_code") shouldBe 500
      log("error_message") shouldBe "stream processing failed"
      log("event_count") shouldBe 1
      log.contains("exception") shouldBe true
    }
  }
}
