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

import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{
  Endpoint,
  Http,
  HttpStatus,
  RxHttpEndpoint,
  RxHttpFilter,
  RxRouter,
  WebSocketContext,
  WebSocketHandler
}
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.{HttpClient, WebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletionStage, CountDownLatch, LinkedBlockingQueue, TimeUnit}

class WebSocketTestApi {
  @Endpoint(path = "/v1/hello")
  def hello: String = "hello"
}

/**
  * A JDK11 WebSocket listener that collects received text/binary messages into blocking queues and exposes latches for
  * lifecycle assertions.
  */
private class CollectingListener extends WebSocket.Listener {
  private val textBuffer   = new StringBuilder
  private val binaryBuffer = new ByteArrayOutputStream()
  val textMessages         = new LinkedBlockingQueue[String]()
  val binaryMessages       = new LinkedBlockingQueue[Array[Byte]]()
  val openLatch            = new CountDownLatch(1)
  val closeLatch           = new CountDownLatch(1)

  override def onOpen(ws: WebSocket): Unit = {
    openLatch.countDown()
    ws.request(Long.MaxValue)
  }

  override def onText(ws: WebSocket, data: CharSequence, last: Boolean): CompletionStage[?] = {
    textBuffer.append(data)
    if (last) {
      textMessages.put(textBuffer.toString)
      textBuffer.setLength(0)
    }
    null
  }

  override def onBinary(ws: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage[?] = {
    val bytes = new Array[Byte](data.remaining())
    data.get(bytes)
    binaryBuffer.write(bytes)
    if (last) {
      binaryMessages.put(binaryBuffer.toByteArray)
      binaryBuffer.reset()
    }
    null
  }

  override def onClose(ws: WebSocket, statusCode: Int, reason: String): CompletionStage[?] = {
    closeLatch.countDown()
    null
  }

  def nextText: String        = textMessages.poll(10, TimeUnit.SECONDS)
  def nextBinary: Array[Byte] = binaryMessages.poll(10, TimeUnit.SECONDS)
}

class WebSocketTest extends AirSpec {

  private def connect(server: NettyServer, path: String, listener: WebSocket.Listener): WebSocket = {
    val client = HttpClient.newHttpClient()
    val uri    = URI.create(s"ws://${server.localAddress}${path}")
    client.newWebSocketBuilder().buildAsync(uri, listener).get(10, TimeUnit.SECONDS)
  }

  test("echo text messages") {
    Netty.server
      .withWebSocketRoute("/ws/echo") { _ =>
        new WebSocketHandler {
          override def onTextMessage(ctx: WebSocketContext, message: String): Unit = {
            ctx.send(s"echo:${message}")
          }
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/echo", listener)
        try {
          ws.sendText("hello", true)
          listener.nextText shouldBe "echo:hello"
          ws.sendText("world", true)
          listener.nextText shouldBe "echo:world"
        } finally {
          ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        }
      }
  }

  test("echo binary messages") {
    Netty.server
      .withWebSocketRoute("/ws/echo") { _ =>
        new WebSocketHandler {
          override def onBinaryMessage(ctx: WebSocketContext, message: Array[Byte]): Unit = {
            ctx.send(message)
          }
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/echo", listener)
        val payload  = Array[Byte](1, 2, 3, 4, 5)
        try {
          ws.sendBinary(ByteBuffer.wrap(payload), true)
          listener.nextBinary shouldBe payload
        } finally {
          ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        }
      }
  }

  test("fragmented text messages are aggregated") {
    Netty.server
      .withWebSocketRoute("/ws/echo") { _ =>
        new WebSocketHandler {
          override def onTextMessage(ctx: WebSocketContext, message: String): Unit = ctx.send(s"echo:${message}")
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/echo", listener)
        try {
          // Send the message in two fragments (last=false, then last=true); the server should see one whole message
          ws.sendText("foo", false).get(10, TimeUnit.SECONDS)
          ws.sendText("bar", true).get(10, TimeUnit.SECONDS)
          listener.nextText shouldBe "echo:foobar"
        } finally {
          ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        }
      }
  }

  test("server can push messages on open") {
    Netty.server
      .withWebSocketRoute("/ws/push") { _ =>
        new WebSocketHandler {
          override def onOpen(ctx: WebSocketContext): Unit = {
            ctx.send("welcome")
          }
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/push", listener)
        try {
          listener.nextText shouldBe "welcome"
        } finally {
          ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        }
      }
  }

  test("onOpen and onClose lifecycle callbacks fire") {
    val openLatch  = new CountDownLatch(1)
    val closeLatch = new CountDownLatch(1)
    Netty.server
      .withWebSocketRoute("/ws/lifecycle") { _ =>
        new WebSocketHandler {
          override def onOpen(ctx: WebSocketContext): Unit  = openLatch.countDown()
          override def onClose(ctx: WebSocketContext): Unit = closeLatch.countDown()
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/lifecycle", listener)
        openLatch.await(10, TimeUnit.SECONDS) shouldBe true
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        closeLatch.await(10, TimeUnit.SECONDS) shouldBe true
      }
  }

  test("filter can reject the upgrade") {
    val denyFilter = new RxHttpFilter {
      override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = {
        Rx.single(Http.response(HttpStatus.Forbidden_403))
      }
    }
    Netty.server
      .withWebSocketRoute("/ws/secure", denyFilter) { _ =>
        new WebSocketHandler {}
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        // The handshake should fail because the filter returns a non-2xx response
        intercept[Exception] {
          connect(server, "/ws/secure", new CollectingListener)
        }
      }
  }

  test("filter returning an empty response rejects the upgrade") {
    val emptyFilter = new RxHttpFilter {
      override def apply(request: Request, next: RxHttpEndpoint): Rx[Response] = Rx.empty
    }
    Netty.server
      .withWebSocketRoute("/ws/empty", emptyFilter) { _ =>
        new WebSocketHandler {}
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        intercept[Exception] {
          connect(server, "/ws/empty", new CollectingListener)
        }
      }
  }

  test("WebSocket works with a handler executor thread pool") {
    Netty.server
      .withHandlerExecutorThreads(4)
      .withWebSocketRoute("/ws/echo") { _ =>
        new WebSocketHandler {
          override def onTextMessage(ctx: WebSocketContext, message: String): Unit = ctx.send(s"echo:${message}")
        }
      }
      .noLogging
      .design
      .build[NettyServer] { server =>
        val listener = new CollectingListener
        val ws       = connect(server, "/ws/echo", listener)
        try {
          ws.sendText("hello", true)
          listener.nextText shouldBe "echo:hello"
        } finally {
          ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye")
        }
      }
  }

  test("normal HTTP endpoints still work alongside WebSocket routes") {
    Netty.server
      .withRouter(RxRouter.of[WebSocketTestApi])
      .withWebSocketRoute("/ws/echo") { _ =>
        new WebSocketHandler {
          override def onTextMessage(ctx: WebSocketContext, message: String): Unit = ctx.send(message)
        }
      }
      .noLogging
      .designWithSyncClient
      .build[wvlet.airframe.http.client.SyncClient] { client =>
        val resp = client.send(Http.GET("/v1/hello"))
        resp.status shouldBe HttpStatus.Ok_200
        resp.contentString shouldBe "hello"
      }
  }
}
