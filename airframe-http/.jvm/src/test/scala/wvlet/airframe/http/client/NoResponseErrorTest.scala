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

import wvlet.airframe.http.{Http, HttpClientException, HttpStatus, RPCException, RPCMethod, RPCStatus}
import wvlet.airframe.surface.Surface
import wvlet.airspec.AirSpec

import java.io.IOException
import java.net.ServerSocket

/**
  * Errors without any response from the server should not be reported as an empty 500 response
  */
class NoResponseErrorTest extends AirSpec {

  case class TestRequest(id: Int)
  case class TestResponse(id: Int)

  private val rpcMethod =
    RPCMethod("/rpc", "example.Api", "test", Surface.of[TestRequest], Surface.of[TestResponse])

  // A localhost address with no listener, so that every connection is refused
  private val closedAddress: String = {
    val socket = new ServerSocket(0)
    val port   = socket.getLocalPort
    socket.close()
    s"localhost:${port}"
  }

  private def newClientConfig = Http.client.noRetry.noCircuitBreaker

  private def checkConnectionFailure(e: HttpClientException): Unit = {
    e.getCause shouldNotBe null
    e.getCause.isInstanceOf[HttpClientException] shouldBe false
  }

  test("sync client sendSafe throws when no response is received") {
    val client = newClientConfig.newSyncClient(closedAddress)
    try {
      val e = intercept[HttpClientException] {
        client.sendSafe(Http.GET("/"))
      }
      checkConnectionFailure(e)
    } finally {
      client.close()
    }
  }

  test("sync client rpc reports UNAVAILABLE when no response is received") {
    val client = newClientConfig.newSyncClient(closedAddress)
    try {
      val e = intercept[RPCException] {
        client.rpc[TestRequest, TestResponse](rpcMethod, TestRequest(1))
      }
      e.status shouldBe RPCStatus.UNAVAILABLE_I2
      e.getCause shouldNotBe null
    } finally {
      client.close()
    }
  }

  // AsyncClient uses the same error handlers. They are tested directly, because a connection failure reported before
  // subscribing the async response can be lost in JavaHttpClientChannel.sendAsync
  private def noResponseError: HttpClientException = {
    intercept[HttpClientException] {
      HttpClients.defaultHttpClientErrorHandler(() => None)(new IOException("connection closed"))
    }
  }

  test("sendSafe error handler rethrows errors without any response") {
    val e = noResponseError
    HttpClients.sendSafeErrorHandler.isDefinedAt(e) shouldBe false
    e.getCause shouldMatch { case _: IOException => }
  }

  test("sendSafe error handler returns the last response") {
    val resp = Http.response(HttpStatus.ServiceUnavailable_503)
    val e    = intercept[HttpClientException] {
      HttpClients.defaultHttpClientErrorHandler(() => Some(resp))(new IOException("connection closed"))
    }
    HttpClients.sendSafeErrorHandler(e) shouldBe resp
  }

  test("rpc error handler reports errors without any response as UNAVAILABLE") {
    val e = intercept[RPCException] {
      HttpClients.rpcNoResponseErrorHandler(noResponseError)
    }
    e.status shouldBe RPCStatus.UNAVAILABLE_I2
    e.getCause shouldMatch { case _: IOException => }
  }
}
