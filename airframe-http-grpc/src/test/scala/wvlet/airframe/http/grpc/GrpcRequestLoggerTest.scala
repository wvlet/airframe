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
package wvlet.airframe.http.grpc

import wvlet.airframe.http.HttpAccessLogWriter
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airspec.AirSpec

/**
  */
class GrpcRequestLoggerTest extends AirSpec {

  private val inMemoryLogWriter = HttpAccessLogWriter.inMemoryLogWriter

  protected override def design = {
    gRPC.server
      .withRouter(DemoApi.router)
      .withRPCLogger(GrpcRequestLogger(inMemoryLogWriter))
      .designWithChannel
  }

  private def capture(body: => Unit): Map[String, Any] = {
    inMemoryLogWriter.clear()
    body
    inMemoryLogWriter.getLogs.head
  }

  test("request logger test") { client: DemoApiClient =>
    test("unary method log") {
      val log = capture {
        client.hello("gRPC")
      }
      debug(log)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/hello"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_method") shouldBe "hello"
      log("rpc_args") shouldBe Map("name" -> "gRPC")
      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "UNARY"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true
    }
  }
}
