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
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airspec.AirSpec

object GrpcContextTest extends AirSpec {

  override protected def design: Design = DemoApi.design

  test("thread local context") { (client: DemoApiClient) =>
    test("get context") {
      val ret = client.getContext
      info(ret)
    }

    test("get context from RPCContext") {
      val ret = client.getRPCContext
      ret shouldBe Some(DemoApi.demoClientId)
    }

    test("get http request from RPCContext") {
      val request = client.getRequest.get
      request.path shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/getRequest"
      val headerMap = request.header
      headerMap.get("x-airframe-client-version") shouldBe defined
      headerMap.get("content-type") shouldBe defined
      headerMap.get("user-agent") shouldBe defined
    }
  }
}
