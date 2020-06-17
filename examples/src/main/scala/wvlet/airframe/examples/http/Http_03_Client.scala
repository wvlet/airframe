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
package wvlet.airframe.examples.http

import java.util.concurrent.TimeUnit

import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.util.Duration
import wvlet.airframe.http.finagle.{Finagle, FinagleSyncClient}
import wvlet.airframe.http.{Endpoint, HttpMethod, Router}
import wvlet.log.LogSupport

/**
  */
object Http_03_Client extends App with LogSupport {
  case class User(id: String, name: String)
  trait MyApp extends LogSupport {
    @Endpoint(method = HttpMethod.GET, path = "/user/:id")
    def getUser(id: String): User = {
      User(id, "xxx")
    }
  }

  val router = Router.add[MyApp]

  val serverDesign = Finagle.server
    .withName("myapp")
    .withRouter(router)
    .design

  val clietnDesign =
    Finagle.client
    // Max retry attempts
      .withMaxRetry(3)
      // Use backoff (or jittering)
      .withBackOff(1)
      // Set request timeout
      .withTimeout(Duration(90, TimeUnit.SECONDS))
      // Add Finagle specific configuration
      .withInitializer { client: Http.Client => client.withHttp2 } // optional
      // Create a new http client to access the server.
      .syncClientDesign

  val design = serverDesign + clietnDesign

  design.build[FinagleSyncClient] { client =>
    // FinagleServer will be started here
    // Read the JSON response as an object
    val user = client.get[User]("/user/1")
    debug(user)

    // Read the response as is
    val request = client.send(Request("/user/2"))
    val content = request.contentString
    debug(content)
  }
}
