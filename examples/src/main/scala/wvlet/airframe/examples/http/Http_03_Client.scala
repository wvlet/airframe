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

import wvlet.airframe.Design

import java.util.concurrent.TimeUnit
import wvlet.airframe.http.{Endpoint, Http, HttpMethod, RxRouter}
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.netty.{Netty, NettyServer}
import wvlet.log.LogSupport

import scala.concurrent.duration.Duration

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

  val router = RxRouter.of[MyApp]

  val serverDesign = Netty.server
    .withName("myapp")
    .withRouter(router)
    .design

  val clientDesign =
    Design.newDesign
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        Http.client
          // Configure the request retry method
          .withRetryContext(
            // Max retry attempts
            _.withMaxRetry(3)
              // Use backoff (or jittering)
              .withBackOff()
          )
          // Set timeout
          .withConnectTimeout(Duration(10, TimeUnit.SECONDS))
          // Create a new http client to access the server.
          .newSyncClient(server.localAddress)
      }

  val design = serverDesign + clientDesign

  design.build[SyncClient] { client =>
    // An HTTP Server will start here
    // Read the JSON response as an object
    val user = client.readAs[User](Http.GET("/user/1"))
    debug(user)

    // Read the response as is
    val request = client.send(Http.GET("/user/2"))
    val content = request.contentString
    debug(content)
  }
}
