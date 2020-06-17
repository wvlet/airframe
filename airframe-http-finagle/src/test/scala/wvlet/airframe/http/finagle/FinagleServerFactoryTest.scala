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
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.tracing.ConsoleTracer
import com.twitter.util.Future
import wvlet.airframe.control.Control._
import wvlet.airframe.http.Router
import wvlet.airspec.AirSpec

/**
  */
class FinagleServerFactoryTest extends AirSpec {
  def `start multiple FinagleServers`: Unit = {
    val router1 = Router.add[MyApi]
    val router2 = Router.add[MyApi]

    val serverConfig1 = Finagle.server
      .withName("server1")
      .withRouter(router1)
    val serverConfig2 = Finagle.server
      .withName("server2")
      .withRouter(router2)

    finagleDefaultDesign.build[FinagleServerFactory] { factory =>
      val server1 = factory.newFinagleServer(serverConfig1)
      val server2 = factory.newFinagleServer(serverConfig2)

      withResources(
        Finagle.newSyncClient(s"localhost:${server1.port}"),
        Finagle.newSyncClient(s"localhost:${server2.port}")
      ) { (client1, client2) =>
        client1.send(Request("/v1/info")).contentString shouldBe "hello MyApi"
        client2.send(Request("/v1/info")).contentString shouldBe "hello MyApi"
      }
    }
  }

  def `allow customize services`: Unit = {
    val d = Finagle.server.withFallbackService {
      new Service[Request, Response] {
        override def apply(request: Request): Future[Response] = {
          val r = Response(Status.Ok)
          r.contentString = "hello custom server"
          Future.value(r)
        }
      }
    }.design

    d.build[FinagleServer] { server =>
      withResource(Finagle.newSyncClient(s"localhost:${server.port}")) { client =>
        client.send(Request("/v1")).contentString shouldBe "hello custom server"
      }
    }
  }

  def `allow customize Finagle Http Server`: Unit = {
    Finagle.server
      .withTracer(ConsoleTracer)
      .withFallbackService(
        new Service[Request, Response] {
          override def apply(request: Request): Future[Response] = {
            val r = Response(Status.Ok)
            r.contentString = "hello custom server with tracer"
            Future.value(r)
          }
        }
      )
      .start { server =>
        withResource(Finagle.newSyncClient(server.localAddress)) { client =>
          client.send(Request("/v1")).contentString shouldBe "hello custom server with tracer"
        }
      }
  }
}
