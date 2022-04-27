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

import io.grpc.Status
import io.grpc.Status.Code
import io.grpc.StatusRuntimeException
import wvlet.airframe.http.{RPCException, RPCStatus, Router}
import wvlet.airframe.http.grpc.GrpcErrorLogTest.DemoApiDebug
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airframe.http.grpc.internal.GrpcException
import wvlet.airspec.AirSpec
import wvlet.log.{LogLevel, Logger}

import java.io.{PrintWriter, StringWriter}

object GrpcErrorHandlingTest extends AirSpec {

  protected override def design = {
    gRPC.server
      .withName("error-handling-test-api")
      .withRouter(Router.add[DemoApiDebug])
      .designWithChannel
  }

  private def suppressLog(loggerName: String)(body: => Unit): Unit = {
    val l                = Logger(loggerName)
    val previousLogLevel = l.getLogLevel
    try {
      // Suppress error logs
      l.setLogLevel(LogLevel.OFF)
      body
    } finally {
      l.setLogLevel(previousLogLevel)
    }
  }
  test("exception test") { (client: DemoApiClient) =>
    warn("Starting a gRPC error handling test")
    suppressLog("wvlet.airframe.http.grpc.internal") {

      test("propagate HttpServerException") {
        val ex = intercept[StatusRuntimeException] {
          client.error409Test
        }
        ex.getMessage.contains("409") shouldBe true
        ex.getStatus.isOk shouldBe false
        ex.getStatus.getCode shouldBe Code.ABORTED
        val trailers = Status.trailersFromThrowable(ex)
        val rpcError = trailers.get[String](GrpcException.rpcErrorBodyKey)
        rpcError.contains("test message") shouldBe true
      }

      test("propagate RPCException") {
        val ex = intercept[StatusRuntimeException] {
          client.rpcExceptionTest
        }
        val trailers     = Status.trailersFromThrowable(ex)
        val rpcErrorJson = trailers.get[String](GrpcException.rpcErrorBodyKey)
        val e            = RPCException.fromJson(rpcErrorJson)

        e.status shouldBe RPCStatus.SYNTAX_ERROR_U3
        e.message shouldBe "test RPC exception"
        e.cause shouldNotBe empty
        e.appErrorCode shouldBe Some(11)
        e.metadata shouldBe Map("retry" -> 0)

        // Extract stack trace
        val s   = new StringWriter()
        val out = new PrintWriter(s)
        e.printStackTrace(out)
        out.flush()

        val stackTrace = s.toString
        // Stack trace should contain two traces from the exception itself and its cause
        stackTrace.contains("wvlet.airframe.http.RPCStatus.newException") shouldBe true
        stackTrace.contains("wvlet.airframe.http.grpc.example.DemoApi.throwEx") shouldBe true
        stackTrace.contains("wvlet.airframe.http.grpc.example.DemoApi.rpcExceptionTest") shouldBe true
      }
    }
  }
}
