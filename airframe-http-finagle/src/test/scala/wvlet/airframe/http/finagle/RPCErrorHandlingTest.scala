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
package wvlet.airframe.http.finagle

import com.twitter.finagle.http.{Method, Request}
import wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi
import wvlet.airframe.http.{HttpHeader, RPC, RPCException, RPCStatus, Router}
import wvlet.airspec.AirSpec

import java.io.{PrintWriter, StringWriter}

object RPCErrorHandlingTest {

  @RPC
  trait DemoApi {
    def userError: String = {
      throw RPCStatus.INVALID_REQUEST_U1.newException("invalid request", metadata = Map("retry_count" -> 3))
    }
    def authCheck: String = {
      throw RPCStatus.UNAUTHENTICATED_U13.newException("not authenticated")
    }

    def permissionCheck: String = {
      throw RPCStatus.PERMISSION_DENIED_U14.newException("permission denied", metadata = Map("retry_count" -> 3))
    }
    def userErrorNoStackTrace: String = {
      throw RPCStatus.INVALID_ARGUMENT_U2.newException("invalid argument").noStackTrace
    }
  }

}

class RPCErrorHandlingTest extends AirSpec {

  private val router = Router.of[DemoApi]

  protected override def design =
    Finagle.server.withRouter(router).designWithSyncClient

  test("rpc error test") { (client: FinagleSyncClient) =>
    warn("Running RPC exception test. Some warning messages will be shown")

    test("with stack trace in msgpack") {
      val req = Request(Method.Post, "/wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi/userError")

      val resp = client.sendSafe(req)

      val errorMsgPack = resp.contentBytes
      val ex           = RPCException.fromMsgPack(errorMsgPack)

      resp.statusCode shouldBe RPCStatus.INVALID_REQUEST_U1.httpStatus.code
      ex.status shouldBe RPCStatus.INVALID_REQUEST_U1
      ex.message shouldBe "invalid request"
      ex.metadata shouldBe Map("retry_count" -> 3)

      val s = new StringWriter
      val p = new PrintWriter(s)
      ex.printStackTrace(p)
      p.flush()
      val stackTrace = s.toString
      stackTrace.contains("DemoApi.userError") shouldBe true
    }

    test("with stack trace") {
      val req = Request(Method.Post, "/wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi/userError")
      req.accept = HttpHeader.MediaType.ApplicationJson
      val resp = client.sendSafe(req)

      val errorJson = resp.getContentString()
      val ex        = RPCException.fromJson(errorJson)

      resp.statusCode shouldBe RPCStatus.INVALID_REQUEST_U1.httpStatus.code
      ex.status shouldBe RPCStatus.INVALID_REQUEST_U1
      ex.message shouldBe "invalid request"
      ex.metadata shouldBe Map("retry_count" -> 3)

      val s = new StringWriter
      val p = new PrintWriter(s)
      ex.printStackTrace(p)
      p.flush()
      val stackTrace = s.toString
      stackTrace.contains("DemoApi.userError") shouldBe true
    }

    test("no stack trace for UNAUTHENTICATED_U13") {
      val req = Request(Method.Post, "/wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi/authCheck")
      req.accept = HttpHeader.MediaType.ApplicationJson
      val resp = client.sendSafe(req)

      val errorJson = resp.getContentString()
      val ex        = RPCException.fromJson(errorJson)

      resp.statusCode shouldBe RPCStatus.UNAUTHENTICATED_U13.httpStatus.code
      ex.status shouldBe RPCStatus.UNAUTHENTICATED_U13
      ex.message shouldBe "not authenticated"

      val s = new StringWriter
      val p = new PrintWriter(s)
      ex.printStackTrace(p)
      p.flush()
      val stackTrace = s.toString

      //
      stackTrace.contains("DemoApi.authCheck") shouldBe false
    }

    test("no stack trace for PERMISSION_DENIED_U14") {
      val req = Request(Method.Post, "/wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi/permissionCheck")
      req.accept = HttpHeader.MediaType.ApplicationJson
      val resp = client.sendSafe(req)

      val errorJson = resp.getContentString()
      val ex        = RPCException.fromJson(errorJson)

      resp.statusCode shouldBe RPCStatus.PERMISSION_DENIED_U14.httpStatus.code
      ex.status shouldBe RPCStatus.PERMISSION_DENIED_U14
      ex.message shouldBe "permission denied"
      ex.metadata shouldBe Map("retry_count" -> 3)

      val s = new StringWriter
      val p = new PrintWriter(s)
      ex.printStackTrace(p)
      p.flush()
      val stackTrace = s.toString

      stackTrace.contains("DemoApi.permissionCheck") shouldBe false
    }

    test("exclude stack trace") {
      val req = Request(Method.Post, "/wvlet.airframe.http.finagle.RPCErrorHandlingTest.DemoApi/userErrorNoStackTrace")
      req.accept = HttpHeader.MediaType.ApplicationJson
      val resp = client.sendSafe(req)

      val errorJson = resp.getContentString()
      val ex        = RPCException.fromJson(errorJson)

      resp.statusCode shouldBe RPCStatus.INVALID_ARGUMENT_U2.httpStatus.code
      ex.status shouldBe RPCStatus.INVALID_ARGUMENT_U2
      ex.message shouldBe "invalid argument"

      val s = new StringWriter
      val p = new PrintWriter(s)
      ex.printStackTrace(p)
      p.flush()
      val stackTrace = s.toString

      // No stack trace
      stackTrace.contains("DemoApi.userErrorNoStackTrace") shouldBe false
    }
  }

}
