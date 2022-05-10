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
package wvlet.airframe.http.js

import wvlet.airframe.http.{Http, HttpClientConfig, RPCHttpClient, RPCHttpSyncClient}
import wvlet.airspec.AirSpec

class JSRPCClientTest extends AirSpec {

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"
  case class TestRequest(id: Int, name: String)
  case class TestResponse(url: String, headers: Map[String, Any])

  test("Create an Async RPCClient") {
    val config = HttpClientConfig()
    val client = Http.client.newAsyncClient(PUBLIC_REST_SERVICE)

    // TODO: This test will be effective after the async test support in AirSpec 22.5.0
    val rpcClient = new RPCHttpClient(config, client)
    rpcClient
      .send[TestRequest, TestResponse]("/post", TestRequest(1, "test"), identity)
      .map { response =>
        debug(response)
        response.headers.get("Content-Type") shouldBe Some("application/msgpack")
      }(config.backend.defaultExecutionContext)
  }

  test("create RPC client") {
    val config = HttpClientConfig()

    // Sanity test for client creation
    val client = config.newRPCClientForScalaJS
  }

}
