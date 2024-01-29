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
import wvlet.airframe.http.{Http, HttpMethod, HttpServer, HttpStatus}
import wvlet.airspec.AirSpec

class NettyServerTest extends AirSpec {

  override def design = {
    Netty.server.design
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client.withRetryContext(_.noRetry).newSyncClient(server.localAddress)
      }
  }

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

  test("Handle various http methods") { (client: SyncClient) =>
    test("valid methods") {
      for (
        m <- Seq(
          HttpMethod.GET,
          HttpMethod.POST,
          HttpMethod.PUT,
          HttpMethod.DELETE,
          HttpMethod.PATCH,
          HttpMethod.TRACE,
          HttpMethod.OPTIONS,
          HttpMethod.HEAD
        )
      ) {
        test(s"${m}") {
          val resp = client.sendSafe(Http.request(m, "/get"))
          resp.status shouldBe HttpStatus.NotFound_404
        }
        test(s"${m.toLowerCase} (lower case)") {
          val resp = client.sendSafe(Http.request(m, "/get"))
          resp.status shouldBe HttpStatus.NotFound_404
        }
      }
    }

    test("reject unsupported methods") {
      for (m <- Seq("UNKNOWN_METHOD", HttpMethod.CONNECT)) {
        test(m) {
          if (m == HttpMethod.CONNECT) {
            pending("Not sure how to support CONNECT in Netty")
          }
          val resp = client.sendSafe(Http.request(m, "/get"))
          resp.status shouldBe HttpStatus.BadRequest_400
        }
      }
    }
  }

}
