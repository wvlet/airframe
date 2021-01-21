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
 * limitations under the Lice nse.
 */
package wvlet.airframe.http.grpc

import wvlet.airframe.http.{HttpAccessLogWriter, Router}
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airframe.rx.{Rx, RxStream}
import wvlet.airspec.AirSpec

import scala.util.{Failure, Try}

/**
  */
class GrpcRequestLoggerTest extends AirSpec {

  private val inMemoryLogWriter = HttpAccessLogWriter.inMemoryLogWriter

  protected override def design = {
    gRPC.server
      .withRouter(DemoApi.router)
      .withRequestLogger(GrpcRequestLogger(inMemoryLogWriter))
      .designWithChannel
  }

  private def captureAll(body: => Unit): Seq[Map[String, Any]] = {
    inMemoryLogWriter.clear()
    body
    inMemoryLogWriter.getLogs
  }

  test("request logger test") { client: DemoApiClient =>
    test("unary method log") {
      val logs = captureAll {
        client.hello("gRPC")
      }
      logs.size shouldBe 1
      val log = logs(0)
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

    test("client-streaming method log") {
      val logs = captureAll {
        client.helloClientStreaming(Rx.sequence("Apple", "Banana"))
      }
      logs.size shouldBe 1

      val log = logs(0)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/helloClientStreaming"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_method") shouldBe "helloClientStreaming"
      // Do not record rpc_args for client-streaming
      log.get("rpc_args") shouldBe empty

      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "CLIENT_STREAMING"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true
    }

    test("server-streaming method log") {
      val logs = captureAll {
        client.helloStreaming("gRPC")
      }
      logs.size shouldBe 1

      val log = logs(0)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/helloStreaming"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_method") shouldBe "helloStreaming"
      // Do not record rpc_args for client-streaming
      log("rpc_args") shouldBe Map("name" -> "gRPC")

      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "SERVER_STREAMING"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true
    }

    test("bidi-streaming method log") {
      val logs = captureAll {
        client.helloBidiStreaming(Rx.sequence("A", "B")).toSeq
      }
      logs.size shouldBe 1

      val log = logs(0)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/helloBidiStreaming"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_method") shouldBe "helloBidiStreaming"
      // Do not record rpc_args for client-streaming
      log.get("rpc_args") shouldBe empty

      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "BIDI_STREAMING"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true
    }

  }
}

object GrpcErrorLogTest extends AirSpec {

  private val inMemoryLogWriter = HttpAccessLogWriter.inMemoryLogWriter

  class DemoApiDebug extends DemoApi {
    override def hello(name: String): String = {
      throw new IllegalArgumentException(s"invalid message: ${name}")
    }

    override def helloClientStreaming(input: RxStream[String]): String = {
      throw new UnsupportedOperationException(s"N/A")
    }
  }

  protected override def design = {
    gRPC.server
      .withRouter(Router.add[DemoApiDebug])
      .withRequestLogger(GrpcRequestLogger(inMemoryLogWriter))
      .designWithChannel
  }

  private def captureAll(body: => Unit): Seq[Map[String, Any]] = {
    inMemoryLogWriter.clear()
    Try(body) match {
      case Failure(exception) =>
        warn(exception.getMessage)
      case _ =>
    }
    inMemoryLogWriter.getLogs
  }

  test("request logger test") { client: DemoApiClient =>
    test("unary method error log") {
      val logs = captureAll {
        client.hello("gRPC")
      }
      logs.size shouldBe 1

      val log = logs(0)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/hello"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.GrpcErrorLogTest$DemoApiDebug"
      log("rpc_method") shouldBe "hello"
      log("rpc_args") shouldBe Map("name" -> "gRPC")
      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "UNARY"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true

      log("exception").getClass shouldBe classOf[IllegalArgumentException]
      log("exception_message") shouldBe "invalid message: gRPC"
    }

    test("client-streaming method error log") {
      val logs = captureAll {
        client.helloClientStreaming(Rx.sequence("A", "B"))
      }
      logs.size shouldBe 1

      val log = logs(0)
      info(log)
      log("path") shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/helloClientStreaming"
      log("content_type") shouldBe "application/grpc"
      log("rpc_interface") shouldBe "wvlet.airframe.http.grpc.example.DemoApi"
      log("rpc_class") shouldBe "wvlet.airframe.http.grpc.GrpcErrorLogTest$DemoApiDebug"
      log("rpc_method") shouldBe "helloClientStreaming"
      log.get("rpc_args") shouldBe empty
      log.contains("time") shouldBe true
      log.contains("event_time") shouldBe true
      log("grpc_method_type") shouldBe "CLIENT_STREAMING"

      log.contains("local_addr") shouldBe true
      log.contains("remote_addr") shouldBe true

      log("exception").getClass shouldBe classOf[UnsupportedOperationException]
      log("exception_message") shouldBe "N/A"
    }

  }

}
