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

import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.*
import wvlet.airframe.http.netty.{Netty}
import wvlet.log.LogSupport

/**
  */
object Http_02_ObjectMapping extends App with LogSupport {
  import wvlet.airframe.*

  case class ListRequest(name: String, page: Int)
  case class ListResponse(name: String, page: Int, nextPageToken: Option[String], data: String)

  case class AppInfo(name: String, version: String = "1.0")

  class MyApp(session: Session) extends LogSupport {
    @Endpoint(method = HttpMethod.GET, path = "/v1/info")
    def appInfo: AppInfo = {
      info(s"showing app info")
      AppInfo("myapp")
    }

    @Endpoint(method = HttpMethod.GET, path = "/v1/list")
    def list(listRequest: ListRequest): ListResponse = {
      ListResponse(
        name = listRequest.name,
        page = listRequest.page,
        nextPageToken = Some("xxxxxx"),
        data = "yyyyyy"
      )
    }

    @Endpoint(method = HttpMethod.GET, path = "/v1/resource/*path")
    def getResource(path: String): HttpMessage.Response = {
      Http.response(HttpStatus.Ok_200, s"resource at ${path}")
    }

    @Endpoint(method = HttpMethod.POST, path = "/admin/shutdown")
    def shutdown: Unit = {
      warn(s"shutting down the server")
      session.shutdown
    }
  }

  val router = RxRouter.of[MyApp]
  val design =
    Netty.server
      .withName("myapp")
      .withRouter(router)
      .design

  design.build[HttpServer] { server =>
    withResource(Http.client.newSyncClient(server.localAddress)) { client =>
      val appInfo = client.readAs[AppInfo](Http.GET("/v1/info"))
      info(appInfo) // AppInfo(myapp,1.0)

      client.readAs[ListResponse](Http.GET("/v1/list"))

      client.send(Http.GET("/v1/resource/resource_path"))
    }
    // Add this code to keep running the server process
    // server.waitServerTermination
  }
}
