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
import wvlet.airframe.http.Router
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

  test("handle error") { (client: DemoApiClient) =>
    warn("Starting a gRPC error handling test")
    val ex = intercept[StatusRuntimeException] {
      client.error409Test
    }
    ex.getMessage.contains("409") shouldBe true
    ex.getStatus.isOk shouldBe false
    ex.getStatus.getCode shouldBe Code.ALREADY_EXISTS
    val trailers = Status.trailersFromThrowable(ex)
    val rpcError = trailers.get[String](GrpcException.rpcErrorKey)
    info(s"error trailer: ${rpcError}")
  }
}
