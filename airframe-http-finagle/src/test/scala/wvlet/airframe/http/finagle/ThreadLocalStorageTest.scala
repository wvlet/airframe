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

import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.Design
import wvlet.airframe.http.{Endpoint, HttpContext, RPCContext, RPCStatus, Router}
import wvlet.airspec.AirSpec

/**
  */
class ThreadLocalStorageTest extends AirSpec {
  class MyApp {
    @Endpoint(path = "/get")
    def get(context: FinagleContext): Unit = {
      context.setThreadLocal("mydata", "hello tls")
    }

    @Endpoint(path = "/read")
    def read(context: FinagleContext): String = {
      context.getThreadLocal[String]("client_id").getOrElse("unknown")
    }

    @Endpoint(path = "/local")
    def local: String = {
      FinagleBackend.getThreadLocal[String]("client_id").getOrElse("unknown")
    }

    @Endpoint(path = "/rpc-context")
    def rpcContext: String = {
      RPCContext.current.getThreadLocal[String]("client_id").getOrElse("unknown")
    }

    @Endpoint(path = "/rpc-header")
    def rpcHeader: String = {
      RPCContext.current.httpRequest.header.get("Authorization") match {
        case Some(x) if x == "Bearer xxxx" =>
          "Ok"
        case None =>
          throw RPCStatus.PERMISSION_DENIED_U14.newException(s"no auth header")
      }
    }

  }

  class TLSReaderFilter extends FinagleFilter {
    override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
      context.setThreadLocal[String]("client_id", "xxxyyy")

      context(request).map { x =>
        // Read TLS set by the child MyApp service
        val mydata = context.getThreadLocal("mydata")

        if (request.path == "/get") {
          val r = Response()
          r.contentString = mydata.getOrElse("N/A")
          r
        } else {
          x
        }
      }
    }
  }

  override protected def design: Design = {
    val router = Router.add[TLSReaderFilter].andThen[MyApp]
    newFinagleServerDesign(name = "tls-test", router = router)
      .bind[FinagleSyncClient].toProvider { server: FinagleServer =>
        Finagle.client.noRetry.newSyncClient(server.localAddress)
      }
  }

  test("tls test") { client: FinagleSyncClient =>
    test("read thread-local data set at the leaf filter") {
      val resp = client.get[String]("/get")
      resp shouldBe "hello tls"
    }

    test("read thread-local data set by the parent filter") {
      val resp = client.get[String]("/read")
      resp shouldBe "xxxyyy"
    }

    test("Get thread local") {
      val resp = client.get[String]("/local")
      resp shouldBe "xxxyyy"
    }

    test("Get thread local from RPCContext") {
      val resp = client.get[String]("/rpc-context")
      resp shouldBe "xxxyyy"
    }

    test("Get request header from RPCContext") {
      val resp = client.get[String]("/rpc-header", { req: Request => req.authorization = "Bearer xxxx"; req })
      resp shouldBe "Ok"
    }
  }
}
