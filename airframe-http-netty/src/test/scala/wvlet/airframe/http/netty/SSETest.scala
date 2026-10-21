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

import wvlet.airframe.http.{Endpoint, Http, HttpMethod, RxRouter, ServerSentEvent, ServerSentEventHandler}
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.rx.{OnNext, Rx, RxBlockingQueue, RxRunner}
import wvlet.airspec.AirSpec

import java.io.ByteArrayOutputStream
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.{CountDownLatch, TimeUnit}

class SSEApi {
  @Endpoint(path = "/v1/health")
  def health(): String = "ok"

  @Endpoint(method = HttpMethod.POST, path = "/v1/slow-sse-stream")
  def slowSseStream(): Rx[ServerSentEvent] = {
    val queue = new RxBlockingQueue[ServerSentEvent]()
    new Thread(new Runnable {
      override def run(): Unit = {
        queue.put(ServerSentEvent(data = "start"))
        // Simulate a slow stream that takes a while to complete
        Thread.sleep(3000)
        queue.put(ServerSentEvent(data = "end"))
        queue.stop()
      }
    }).start()
    queue
  }

  @Endpoint(path = "/v1/sse")
  def sse(): Response = {
    Http
      .response()
      .withContentType("text/event-stream")
      .withContent(s"""data: hello stream
           |
           |data: another stream message
           |data: with two lines
           |
           |event: custom-event
           |data: hello custom event
           |
           |: this is a comment
           |
           |id: 123
           |data: hello again
           |
           |id: 1234
           |event: custom-event
           |data: hello again 2
           |
           |retry: 1000
           |data: need to retry
           |""".stripMargin)
  }

  @Endpoint(method = HttpMethod.POST, path = "/v1/sse-stream")
  def sseStream(): Rx[ServerSentEvent] = {
    val queue = new RxBlockingQueue[ServerSentEvent]()
    new Thread(new Runnable {
      override def run(): Unit = {
        queue.put(ServerSentEvent(data = "hello stream"))
        // Thread.sleep(100)
        queue.put(ServerSentEvent(data = "another stream message\nwith two lines"))
        // Thread.sleep(50)
        queue.put(ServerSentEvent(event = Some("custom-event"), data = "hello custom event"))
        Thread.sleep(20)
        queue.put(ServerSentEvent(id = Some("123"), data = "hello again"))
        Thread.sleep(10)
        queue.put(ServerSentEvent(id = Some("1234"), event = Some("custom-event"), data = "hello again 2"))
        Thread.sleep(30)
        queue.put(ServerSentEvent(retry = Some(1000), data = "need to retry"))
        queue.stop()
      }
    }).start()
    queue
  }
}

class SSETest extends AirSpec {
  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[SSEApi])
        .designWithAsyncClient
    )
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client.newSyncClient(server.localAddress)
      }
  }

  test("read sse-events") { (client: AsyncClient) =>
    val queue = new RxBlockingQueue[ServerSentEvent]()
    val rx    = client.send(
      Http
        .GET("/v1/sse")
        .withEventHandler(new ServerSentEventHandler {
          override def onError(e: Throwable): Unit = {
            queue.stop()
          }
          override def onCompletion(): Unit = {
            queue.stop()
          }
          override def onEvent(e: ServerSentEvent): Unit = {
            queue.put(e)
          }
        })
    )

    rx.map { resp =>
      resp.statusCode shouldBe 200

      val events   = queue.toSeq.toList
      val expected = List(
        ServerSentEvent(data = "hello stream"),
        ServerSentEvent(data = "another stream message\nwith two lines"),
        ServerSentEvent(event = Some("custom-event"), data = "hello custom event"),
        ServerSentEvent(id = Some("123"), data = "hello again"),
        ServerSentEvent(id = Some("1234"), event = Some("custom-event"), data = "hello again 2"),
        ServerSentEvent(retry = Some(1000), data = "need to retry")
      )

      trace(events.mkString("\n"))
      events shouldBe expected
    }
  }

  test("read sse-stream") { (client: AsyncClient) =>
    val queue = new RxBlockingQueue[ServerSentEvent]()
    val rx    = client.send(
      Http
        .POST("/v1/sse-stream")
        .withEventHandler(new ServerSentEventHandler {
          override def onError(e: Throwable): Unit = {
            queue.stop()
          }
          override def onCompletion(): Unit = {
            queue.stop()
          }
          override def onEvent(e: ServerSentEvent): Unit = {
            debug(e)
            queue.put(e)
          }
        })
    )

    rx.map { resp =>
      resp.statusCode shouldBe 200

      val events   = queue.toSeq.toList
      val expected = List(
        ServerSentEvent(data = "hello stream"),
        ServerSentEvent(data = "another stream message\nwith two lines"),
        ServerSentEvent(event = Some("custom-event"), data = "hello custom event"),
        ServerSentEvent(id = Some("123"), data = "hello again"),
        ServerSentEvent(id = Some("1234"), event = Some("custom-event"), data = "hello again 2"),
        ServerSentEvent(retry = Some(1000), data = "need to retry")
      )

      trace(events.mkString("\n"))
      // trace(expected.mkString("\n"))
      events shouldBe expected
    }
  }

  test("SSE streaming should not block concurrent requests") { (asyncClient: AsyncClient, syncClient: SyncClient) =>
    // Start a slow SSE stream in the background
    val streamStarted = new CountDownLatch(1)
    val streamThread  = new Thread(new Runnable {
      override def run(): Unit = {
        val rx = asyncClient.send(
          Http
            .POST("/v1/slow-sse-stream")
            .withEventHandler(new ServerSentEventHandler {
              override def onError(e: Throwable): Unit       = {}
              override def onCompletion(): Unit              = {}
              override def onEvent(e: ServerSentEvent): Unit = {
                // Signal that SSE stream has started
                streamStarted.countDown()
              }
            })
        )
        // Subscribe to actually trigger the request
        RxRunner.run(rx) { case _ => }
      }
    })
    streamThread.setDaemon(true)
    streamThread.start()

    // Wait for the SSE stream to start
    streamStarted.await(5, TimeUnit.SECONDS) shouldBe true

    // While the slow SSE stream is active, send a regular GET request.
    // Before the fix, this would hang because the Netty worker thread was blocked.
    val resp = syncClient.send(Http.GET("/v1/health"))
    resp.statusCode shouldBe 200
    resp.contentString shouldBe "ok"
  }

  /**
    * Send a raw HTTP request to the SSE stream endpoint and read the whole response until the server closes the socket
    */
  private def readRawSseResponse(server: NettyServer, httpVersion: String): String = {
    val socket = new Socket("localhost", server.localAddress.split(":").last.toInt)
    try {
      // Fail the test instead of hanging if the server keeps the connection open
      socket.setSoTimeout(10000)
      val out = socket.getOutputStream
      out.write(
        s"POST /v1/sse-stream ${httpVersion}\r\nHost: localhost\r\nContent-Length: 0\r\n\r\n"
          .getBytes(StandardCharsets.UTF_8)
      )
      out.flush()
      val in   = socket.getInputStream
      val buf  = new Array[Byte](8192)
      val body = new ByteArrayOutputStream()
      var n    = in.read(buf)
      // read() returns -1 (EOF) only after the server closes the connection
      while (n != -1) {
        body.write(buf, 0, n)
        n = in.read(buf)
      }
      new String(body.toByteArray, StandardCharsets.UTF_8)
    } finally {
      socket.close()
    }
  }

  private def checkSseConnectionClose(response: String): Unit = {
    val (header, body) = response.splitAt(response.indexOf("\r\n\r\n"))
    val headerLines    = header.split("\r\n").map(_.toLowerCase).toSeq
    headerLines.find(_.startsWith("connection:")) shouldBe Some("connection: close")
    body shouldContain "data: hello stream"
    body shouldContain "data: need to retry"
  }

  test("advertise Connection: close for SSE streams, which close the channel at the end") { (server: NettyServer) =>
    checkSseConnectionClose(readRawSseResponse(server, "HTTP/1.1"))
  }

  test("advertise Connection: close for SSE streams with HTTP/1.0 requests") { (server: NettyServer) =>
    checkSseConnectionClose(readRawSseResponse(server, "HTTP/1.0"))
  }

}
