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

import wvlet.airframe.http.{Endpoint, Http, HttpServer, HttpStatus, RxRouter}
import wvlet.airframe.http.client.SyncClient
import wvlet.airspec.AirSpec

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

class NettyHandlerExecutorTest extends AirSpec {

  test("configure handler executor threads") {
    val config = Netty.server.withHandlerExecutorThreads(16)
    config.handlerExecutorThreads shouldBe Some(16)
  }

  test("default has no handler executor") {
    val config = Netty.server
    config.handlerExecutorThreads shouldBe None
  }

  test("reject non-positive thread count") {
    intercept[IllegalArgumentException] {
      Netty.server.withHandlerExecutorThreads(0)
    }
    intercept[IllegalArgumentException] {
      Netty.server.withHandlerExecutorThreads(-1)
    }
  }

  test("server with handler executor starts and handles requests") {
    Netty.server
      .withRouter(RxRouter.of[HandlerExecutorTestApi])
      .withHandlerExecutorThreads(4)
      .noLogging
      .designWithSyncClient
      .build[SyncClient] { client =>
        val resp = client.send(Http.GET("/v1/hello"))
        resp.status shouldBe HttpStatus.Ok_200
        resp.contentString shouldBe "hello"
      }
  }
}

class HandlerExecutorTestApi {
  @Endpoint(path = "/v1/hello")
  def hello: String = "hello"
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
    val config = Netty.server.withShutdownHook
      .withGracefulShutdown(quietPeriodSeconds = 1, timeoutSeconds = 5)
      .noLogging

    config.design.build[NettyServer] { server =>
      server.localAddress.contains("localhost") shouldBe true
      // Explicitly stop to test shutdown hook unregistration
      server.stop()
    }
  }
}
