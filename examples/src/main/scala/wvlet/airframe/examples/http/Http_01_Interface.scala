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

import wvlet.airframe.http.{Endpoint, HttpMethod, HttpServer, RxRouter}
import wvlet.airframe.http.netty.{Netty, NettyServer}
import wvlet.log.LogSupport

/**
  */
object Http_01_Interface extends App {
  case class User(id: String, name: String)

  class MyApp extends LogSupport {
    @Endpoint(method = HttpMethod.GET, path = "/user/:id")
    def getUser(id: String): User = {
      info(s"lookup user: ${id}")
      User(id, "xxx")
    }
  }

  val router = RxRouter.of[MyApp]
  val design = Netty.server.withName("myapp").withPort(18080).withRouter(router).design

  design.build[HttpServer] { server =>
    val serverAddress = server.localAddress

    // Wait server termination
    // server.waitServerTermination
  }
}
