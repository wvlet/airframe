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
package wvlet.airframe.http

import wvlet.airspec.AirSpec

class RPCExceptionTest extends AirSpec {

  private def newTestException = RPCStatus.INVALID_REQUEST_U1.newException(
    "invalid RPC request",
    new IllegalArgumentException("syntax error"),
    appErrorCode = 10,
    metadata = Map("line" -> 100, "pos" -> 10)
  )

  test("Create a new RPCException") {
    RPCStatus.USER_ERROR_U0.newException(s"user error test")
  }

  test("toMap error contents") {
    val e1 = newTestException
    val m  = e1.toMessage
    m.code shouldBe e1.status.code
    m.codeName shouldBe e1.status.name
    m.message shouldBe e1.message
    m.appErrorCode shouldBe e1.appErrorCode
    m.metadata shouldBe e1.metadata
    m.cause shouldNotBe empty
  }

  test("hide stack trace") {
    val e1 = newTestException
    e1.noStackTrace
    val m = e1.toMessage
    m.cause shouldBe empty
  }

  test("toJson error contents") {
    val e1 = newTestException
    // sanity test
    val json = e1.toJson
  }

}
