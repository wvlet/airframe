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

import wvlet.airframe.Design
import wvlet.airframe.http.{RPCException, RPCStatus}
import wvlet.airframe.http.grpc.example.DemoApiV2
import wvlet.airspec.AirSpec

class GrpcClientTest extends AirSpec {

  override def design: Design = DemoApiV2.design

  test("GrpcClient") { (client: DemoApiV2.SyncClient) =>
    test("hello") {
      client.hello("v2") shouldBe "Hello v2!"
    }

    test("RPCException") {
      warn("Testing RPCException handling")
      val ex = intercept[RPCException] {
        client.errorTest("xxx")
      }
      ex.status shouldBe RPCStatus.INVALID_ARGUMENT_U2
      ex.message shouldBe "Hello error: xxx"
    }
  }

}
