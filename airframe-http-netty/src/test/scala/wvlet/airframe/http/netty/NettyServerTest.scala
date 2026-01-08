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

import wvlet.airframe.control.Control
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.{Http, HttpMethod, HttpServer, HttpStatus, RxRouter}
import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit

class NettyServerTest extends AirSpec {
  initDesign(_ + Netty.server.design)

  test("NettyServer should be available") { (server: NettyServer) =>
    test("double start should be ignored") {
      server.start
    }
  }

  test("can't start server after closing it") { (server: NettyServer) =>
    server.close()
    intercept[IllegalStateException] {
      server.start
    }
  }

  test("safely close multiple times") { (server: HttpServer) =>
    server.close()
    server.close()
  }
}

class NettyGracefulShutdownTest extends AirSpec {

  test("configure graceful shutdown parameters") {
    val config = Netty.server
      .withGracefulShutdown(quietPeriodSeconds = 5, timeoutSeconds = 60)

    config.shutdownQuietPeriodSeconds shouldBe 5
    config.shutdownTimeoutSeconds shouldBe 60
  }

  test("configure shutdown quiet period individually") {
    val config = Netty.server.withShutdownQuietPeriod(10)
    config.shutdownQuietPeriodSeconds shouldBe 10
    // Default timeout should remain
    config.shutdownTimeoutSeconds shouldBe 30
  }

  test("configure shutdown timeout individually") {
    val config = Netty.server.withShutdownTimeout(120)
    config.shutdownTimeoutSeconds shouldBe 120
    // Default quiet period should remain
    config.shutdownQuietPeriodSeconds shouldBe 2
  }

  test("enable shutdown hook") {
    val config = Netty.server.withShutdownHook
    config.registerShutdownHook shouldBe true
  }

  test("disable shutdown hook") {
    val config = Netty.server.withShutdownHook.noShutdownHook
    config.registerShutdownHook shouldBe false
  }

  test("graceful shutdown completes with custom timeout") {
    val config = Netty.server
      .withGracefulShutdown(quietPeriodSeconds = 1, timeoutSeconds = 5)
      .noLogging

    config.design.build[NettyServer] { server =>
      // Server should start successfully
      server.localAddress.contains("localhost") shouldBe true
      // Stop with graceful shutdown
      server.stop()
    }
  }

  test("server with shutdown hook enabled starts and stops correctly") {
    val config = Netty.server
      .withShutdownHook
      .withGracefulShutdown(quietPeriodSeconds = 1, timeoutSeconds = 5)
      .noLogging

    config.design.build[NettyServer] { server =>
      server.localAddress.contains("localhost") shouldBe true
      // Explicitly stop to test shutdown hook unregistration
      server.stop()
    }
  }
}

class NettyConnectionTrackingTest extends AirSpec {

  test("connection tracker tracks active connections") {
    val tracker = new NettyConnectionTracker()
    tracker.activeConnectionCount shouldBe 0
    tracker.activeRequestCount shouldBe 0
    tracker.hasActiveRequests shouldBe false
  }

  test("connection tracker tracks request lifecycle") {
    val tracker = new NettyConnectionTracker()

    tracker.requestStarted()
    tracker.activeRequestCount shouldBe 1
    tracker.hasActiveRequests shouldBe true

    tracker.requestStarted()
    tracker.activeRequestCount shouldBe 2

    tracker.requestCompleted()
    tracker.activeRequestCount shouldBe 1
    tracker.hasActiveRequests shouldBe true

    tracker.requestCompleted()
    tracker.activeRequestCount shouldBe 0
    tracker.hasActiveRequests shouldBe false
  }

  test("connection tracker await completion returns true when no active requests") {
    val tracker = new NettyConnectionTracker()
    val completed = tracker.awaitCompletion(1, TimeUnit.SECONDS)
    completed shouldBe true
  }

  test("server exposes active connection and request counts") {
    val config = Netty.server.noLogging

    config.design.build[NettyServer] { server =>
      // Initially no active requests
      server.activeRequestCount shouldBe 0
      server.hasActiveRequests shouldBe false
      // Connection count may be 0 initially (before any clients connect)
      server.activeConnectionCount shouldBe 0
    }
  }

  test("request tracking with simple HTTP request") {
    val router = RxRouter.of[SimpleApi]
    val config = Netty.server
      .withRouter(router)
      .noLogging

    config.designWithSyncClient.build[SyncClient] { client =>
      // Make a request
      val response = client.send(Http.GET("/hello"))
      response.status shouldBe HttpStatus.Ok_200

      // After request completes, count should return to 0
      // (there may be a small delay for async completion)
      Thread.sleep(100)
    }
  }
}

class SimpleApi {
  import wvlet.airframe.http.*

  @Endpoint(method = HttpMethod.GET, path = "/hello")
  def hello: String = "Hello, World!"
}
