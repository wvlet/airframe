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

import io.grpc.{CallCredentials, Metadata}
import wvlet.airframe.Design
import wvlet.airframe.http.grpc.example.DemoApi
import wvlet.airframe.http.grpc.example.DemoApi.DemoApiClient
import wvlet.airspec.AirSpec

import java.util.concurrent.Executor
import scala.collection.parallel.ParSeq

object GrpcContextTest extends AirSpec {

  override protected def design: Design = DemoApi.design

  test("thread local context") { (client: DemoApiClient) =>
    test("get context") {
      val ret = client.getContext
      debug(ret)
    }

    test("get context from RPCContext") {
      for (i <- ParSeq(1 to 10)) {
        val ret = client.getRPCContext
        ret shouldBe Some(DemoApi.demoClientId)
      }
    }

    test("get http request from RPCContext") {
      // Set authorization header
      val cred = new CallCredentials {
        override def applyRequestMetadata(
            requestInfo: CallCredentials.RequestInfo,
            appExecutor: Executor,
            applier: CallCredentials.MetadataApplier
        ): Unit = {
          val m = new Metadata()
          m.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer xxxx-yyyy")
          applier.apply(m)
        }
        override def thisUsesUnstableApi(): Unit = {}
      }
      val request = client.withCallCredentials(cred).getRequest
      request.path shouldBe "/wvlet.airframe.http.grpc.example.DemoApi/getRequest"

      val headerMap = request.header
      headerMap.get("x-airframe-client-version") shouldBe defined
      headerMap.get("content-type") shouldBe defined
      headerMap.get("user-agent") shouldBe defined
      headerMap.get("authorization") shouldBe Some("Bearer xxxx-yyyy")
    }
  }
}
