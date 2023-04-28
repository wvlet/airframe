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

import wvlet.airframe.http.{Http, HttpClientConfig, RPCMethod}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

object JSRPCClientTest extends AirSpec {
  case class Person(id: Int, name: String)
  private val p = Person(1, "leo")

  // Use a public REST test server
  private val PUBLIC_REST_SERVICE = "https://jsonplaceholder.typicode.com"
  case class TestRequest(userId: Int, name: String)
  case class TestResponse(userId: Int, name: String)

  test("Create an Async RPCClient") {
    val client = Http.client.newAsyncClient(PUBLIC_REST_SERVICE)

    test("rpc") {
      flaky {
        val m = RPCMethod("/posts", "example.Api", "test", Surface.of[TestRequest], Surface.of[TestResponse])
        client
          .rpc[TestRequest, TestResponse](m, TestRequest(1, "test"))
          .toRxStream
          .map { response =>
            debug(response)
            response shouldBe TestResponse(1, "test")
          }
      }
    }

    test("call") {
      flaky {
        client
          .call[Person, Map[String, Any]](Http.POST("/posts"), p)
          .toRxStream
          .map { m =>
            debug(m)
            m shouldBe Map("id" -> 101, "name" -> "leo")
          }
      }
    }
  }

  test("create RPC client") {
    val config = HttpClientConfig()

    // Sanity test for client creation
    val client = config.newJSClient
  }

}
