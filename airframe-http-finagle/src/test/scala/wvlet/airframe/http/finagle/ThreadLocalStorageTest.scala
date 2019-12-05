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
import wvlet.airframe.http.{Endpoint, HttpContext, Router}
import wvlet.airspec.AirSpec

/**
  *
  */
class ThreadLocalStorageTest extends AirSpec {
  class MyApp {
    @Endpoint(path = "/")
    def get(context: FinagleContext): Unit = {
      context.setThreadLocal("mydata", "hello tls")
    }
  }

  class TLSReaderFilter extends FinagleFilter {
    override def apply(request: Request, context: HttpContext[Request, Response, Future]): Future[Response] = {
      context(request).map { x =>
        // Read TLS set by the child MyApp service
        val mydata = context.getThreadLocal("mydata")
        val r      = Response()
        r.contentString = mydata.getOrElse("N/A")
        r
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

  def `store data to thread-local storage`(client: FinagleSyncClient): Unit = {
    val resp = client.get[String]("/")
    resp shouldBe "hello tls"
  }
}
