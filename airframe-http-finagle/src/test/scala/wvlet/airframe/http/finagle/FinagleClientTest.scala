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

import com.twitter.finagle.http.Request
import com.twitter.util.Await
import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.log.io.IOUtil

trait FinagleClientTestApi {
  @Endpoint(path = "/hello")
  def hello: String = {
    "hello"
  }
}

/**
  *
  */
class FinagleClientTest extends AirframeSpec {

  "create client" in {
    val port = IOUtil.randomPort

    val r = Router.add[FinagleClientTestApi]
    val d = finagleDefaultDesign
      .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = port, router = r))

    d.build[FinagleServer] { server =>
      withResource(FinagleClient.newClient(s"localhost:${port}")) { client =>
        val r = client.request(Request("/hello")).map { resp =>
          resp.contentString
        }
        info(Await.result(r))
      }
    }

  }

}
