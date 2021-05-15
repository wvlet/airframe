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

import java.io.IOException
import java.net.URL

import wvlet.airframe.control.Control
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  */
class FinagleDesignTest extends AirSpec {
  trait MyTestServer {
    @Endpoint(path = "/hello")
    def hello: String = {
      "hello"
    }
  }

  def newConfig = FinagleServerConfig(router = Router.of[MyTestServer])

  test("start server") {
    finagleDefaultDesign
      .bind[FinagleServerConfig].toInstance(newConfig)
      .bind[FinagleSyncClient].toProvider { server: FinagleServer => Finagle.newSyncClient(server.localAddress) }
      .noLifeCycleLogging
      .build[FinagleSyncClient] { client =>
        // The server will start here
        val msg = client.get[String]("/hello")
        msg shouldBe "hello"
      }
  }

  test("no-server design") {
    val config = newConfig
    finagleBaseDesign
      .bind[FinagleServerConfig].toInstance(config)
      .noLifeCycleLogging
      .build[FinagleServerFactory] { factory =>
        // No server should start here
        intercept[IOException] {
          Control.withResource(new URL(s"http://localhost:${config.port}").openStream()) { in =>
            IOUtil.readAsString(in)
          }
        }
      }
  }

  test("build a server from factory") {
    finagleBaseDesign.noLifeCycleLogging.build[FinagleServerFactory] { factory =>
      val s1 = factory.newFinagleServer(newConfig)
      Control.withResource(FinagleClient.newSyncClient(s1.localAddress)) { client =>
        client.get[String]("/hello") shouldBe "hello"
      }
    }
  }
}
