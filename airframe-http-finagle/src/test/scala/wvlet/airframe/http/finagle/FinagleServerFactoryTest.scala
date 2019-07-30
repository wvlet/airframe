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
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.tracing.ConsoleTracer
import com.twitter.util.{Await, Future}
import wvlet.airframe.AirframeSpec
import wvlet.airframe.http.Router
import wvlet.airframe.http.finagle.FinagleServer.FinagleService
import wvlet.log.io.IOUtil

/**
  *
  */
class FinagleServerFactoryTest extends AirframeSpec {
  val p1 = IOUtil.unusedPort
  val p2 = IOUtil.unusedPort

  val router1 = Router.add[MyApi]
  val router2 = Router.add[MyApi]

  "FinagleServerFactory" should {
    "start multiple FinagleServers" in {
      val serverConfig1 = FinagleServerConfig(name = "server1", port = p1, router = router1)
      val serverConfig2 = FinagleServerConfig(name = "server2", port = p2, router = router2)

      finagleDefaultDesign.build[FinagleServerFactory] { factory =>
        val server1 = factory.newFinagleServer(serverConfig1)
        val server2 = factory.newFinagleServer(serverConfig2)

        val client1 = Http.client.newService(s"localhost:${p1}")
        val client2 = Http.client.newService(s"localhost:${p2}")

        Await.result(client1(Request("/v1/info")).map(_.contentString)) shouldBe "hello MyApi"
        Await.result(client2(Request("/v1/info")).map(_.contentString)) shouldBe "hello MyApi"
      }
    }

    "allow customize services" in {
      val d2 =
        finagleDefaultDesign
          .bind[FinagleServerFactory].to[CustomFinagleServerFactory]

      d2.build[FinagleServer] { server =>
        val client = Http.client.newService(s"localhost:${server.port}")
        Await.result(client(Request("/v1")).map(_.contentString)) shouldBe "hello custom server"
      }
    }

    "allow customize Finagle Http Server" in {
      val d =
        finagleDefaultDesign
          .bind[FinagleServerFactory].to[CustomFinagleServerFactoryWithTracer]

      d.build[FinagleServer] { server =>
        val client = Http.client.newService(server.localAddress)
        Await.result(client(Request("/v1")).map(_.contentString)) shouldBe "hello custom server with tracer"
      }
    }
  }
}

trait CustomFinagleServerFactory extends FinagleServerFactory {
  override protected def newService(finagleRouter: FinagleRouter): FinagleService = {
    // You can customize service filter as you with
    finagleRouter andThen new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val r = Response(Status.Ok)
        r.contentString = "hello custom server"
        Future.value(r)
      }
    }
  }
}

trait CustomFinagleServerFactoryWithTracer extends FinagleServerFactory {
  override protected def newService(finagleRouter: FinagleRouter): FinagleService = {
    // You can customize service filter as you with
    finagleRouter andThen new Service[Request, Response] {
      override def apply(request: Request): Future[Response] = {
        val r = Response(Status.Ok)
        r.contentString = "hello custom server with tracer"
        Future.value(r)
      }
    }
  }
  override def initServer(server: Http.Server): Http.Server = {
    server.withTracer(ConsoleTracer)
  }
}
