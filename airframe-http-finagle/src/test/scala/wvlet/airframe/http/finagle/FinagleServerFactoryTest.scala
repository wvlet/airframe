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
import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.util.Await
import wvlet.airframe.AirframeSpec
import wvlet.airframe.http.Router
import wvlet.log.io.IOUtil

/**
  *
  */
class FinagleServerFactoryTest extends AirframeSpec {
  val p1 = IOUtil.unusedPort
  val p2 = IOUtil.unusedPort

  val router1 = Router.of[MyApi]
  val router2 = Router.of[MyApi]

  type MyServer1 = FinagleServer
  type MyServer2 = FinagleServer

  val d =
    finagleDefaultDesign
      .bind[MyApi].toSingleton
      .bind[MyServer1].toProvider { f: FinagleServerFactory =>
        f.newFinagleServer(p1, router1)
      }
      .bind[MyServer2].toProvider { f: FinagleServerFactory =>
        f.newFinagleServer(p2, router2)
      }
      .withProductionMode // Start up both servers

  "FinagleServerFactory" should {
    "start multiple FinagleServers" in {
      d.withSession { session =>
        val client1 = Http.client
          .newService(s"localhost:${p1}")
        val client2 = Http.client
          .newService(s"localhost:${p2}")

        Await.result(client1(Request("/v1/info")).map(_.contentString)) shouldBe "hello MyApi"
        Await.result(client2(Request("/v1/info")).map(_.contentString)) shouldBe "hello MyApi"
      }
    }
  }
}
