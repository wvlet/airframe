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

import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.{Endpoint, HttpMethod, Router}
import wvlet.log.io.IOUtil

case class User(id: Int, name: String)

trait FinagleClientTestApi {
  @Endpoint(path = "/user/:id")
  def get(id: Int): User = {
    User(id, "leo")
  }

  @Endpoint(method = HttpMethod.POST, path = "/user")
  def create(newUser: User): User = {
    newUser
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/user/:id")
  def delete(id: Int): User = {
    User(id, "leo")
  }

  @Endpoint(method = HttpMethod.PUT, path = "/user")
  def put(updatedUser: User): User = {
    updatedUser
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
      .noLifeCycleLogging

    d.build[FinagleServer] { server =>
      withResource(FinagleClient.newSyncClient(s"localhost:${port}")) { client =>
        client.get[User]("/user/1") shouldBe User(1, "leo")
        client.post[User]("/user", User(2, "yui")) shouldBe User(2, "yui")
        client.delete[User]("/user/1") shouldBe User(1, "leo")
        client.put[User]("/user", User(10, "aina")) shouldBe User(10, "aina")
      }
    }
  }
}
