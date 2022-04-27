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
import wvlet.airframe.http.{RPCException, Router}
import wvlet.airframe.http.grpc.GrpcErrorLogTest.DemoApiDebug
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airframe.http.grpc.internal.GrpcException
import wvlet.airspec.AirSpec

object GrpcErrorHandlingTest extends AirSpec {

  protected override def design = {
    gRPC.server
      .withName("error-handling-test-api")
      .withRouter(Router.add[DemoApiDebug])
      .designWithChannel
  }

  test("exception test") { (client: DemoApiClient) =>
    warn("Starting a gRPC error handling test")

    test("handle error") {
      val ex = intercept[StatusRuntimeException] {
        client.error409Test
      }
      ex.getMessage.contains("409") shouldBe true
      ex.getStatus.isOk shouldBe false
      ex.getStatus.getCode shouldBe Code.ABORTED
      val trailers = Status.trailersFromThrowable(ex)
      val rpcError = trailers.get[String](GrpcException.rpcErrorKey)
      info(s"error trailer: ${rpcError}")
    }

    test("rpc exception test") {
      val ex = intercept[StatusRuntimeException] {
        client.rpcExceptionTest
      }
      val trailers     = Status.trailersFromThrowable(ex)
      val rpcErrorJson = trailers.get[String](GrpcException.rpcErrorKey)
      info(rpcErrorJson)
      val e = RPCException.fromJson(rpcErrorJson)
      info(e)
      info(e.getCause)
    }
  }
}
