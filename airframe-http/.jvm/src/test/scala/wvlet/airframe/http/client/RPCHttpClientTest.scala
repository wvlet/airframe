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
package wvlet.airframe.http.client

import wvlet.airframe.http.{Http, RPCMethod}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec
import wvlet.airframe.http.HttpHeader.MediaType

object RPCHttpClientTest extends AirSpec {

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  case class TestRequest(id: Int, name: String)
  case class TestResponse(url: String, headers: Map[String, Any])

  test("Create an RPCSyncClient") {
    val rpcClient = Http.client.newSyncClient(PUBLIC_REST_SERVICE)
    val m         = RPCMethod("/post", "example.Api", "test", Surface.of[TestRequest], Surface.of[TestResponse])
    val response  = rpcClient.rpc[TestRequest, TestResponse](m, TestRequest(1, "test"))

    // Test message
    debug(response)
    response.headers.get("Content-Type") shouldBe Some(MediaType.ApplicationJson)
  }

}
