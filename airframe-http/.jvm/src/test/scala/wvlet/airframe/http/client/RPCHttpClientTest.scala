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
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

object RPCHttpClientTest extends AirSpec {

  // Use a public REST test server - skip tests if unavailable
  private val PUBLIC_REST_SERVICE = "https://httpbin.org/"

  private def isServiceAvailable: Boolean = {
    try {
      val client = Http.client
        .withConnectTimeout(Duration(5, TimeUnit.SECONDS))
        .withReadTimeout(Duration(5, TimeUnit.SECONDS))
        .newSyncClient(PUBLIC_REST_SERVICE)
      val resp = client.sendSafe(Http.GET("/get"))
      resp.status.isSuccessful
    } catch {
      case _: Exception => false
    }
  }

  case class TestRequest(id: Int, name: String)
  case class TestResponse(url: String, headers: Map[String, Any])

  test("Create an RPCSyncClient") {
    if (!isServiceAvailable) {
      pending(
        s"External service ${PUBLIC_REST_SERVICE} is not available. Use integration tests with local Netty server instead."
      )
    }

    val rpcClient = Http.client.newSyncClient(PUBLIC_REST_SERVICE)
    val m         = RPCMethod("/post", "example.Api", "test", Surface.of[TestRequest], Surface.of[TestResponse])
    val response  = rpcClient.rpc[TestRequest, TestResponse](m, TestRequest(1, "test"))

    // Test message
    debug(response)
    response.headers.get("Content-Type") shouldBe Some(MediaType.ApplicationJson)
  }
}
