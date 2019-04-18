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

import com.twitter.finagle.http.{Request, Response, Status}
import wvlet.airframe.AirframeSpec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http._
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class User(id: Int, name: String)

trait FinagleClientTestApi extends LogSupport {

  @Endpoint(method = HttpMethod.GET, path = "/")
  def info: String = {
    "Ok"
  }

  @Endpoint(method = HttpMethod.GET, path = "/user/:id")
  def get(id: Int): User = {
    User(id, "leo")
  }

  @Endpoint(method = HttpMethod.GET, path = "/user")
  def list: Seq[User] = {
    Seq(User(1, "leo"))
  }

  @Endpoint(method = HttpMethod.POST, path = "/user")
  def create(newUser: User): User = {
    newUser
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/user/:id")
  def delete(id: Int): User = {
    User(id, "xxx")
  }

  @Endpoint(method = HttpMethod.PUT, path = "/user")
  def put(updatedUser: User): User = {
    updatedUser
  }

  @Endpoint(method = HttpMethod.GET, path = "/busy")
  def busy: Response = {
    trace("called busy method")
    Response(Status.InternalServerError)
  }

  @Endpoint(method = HttpMethod.GET, path = "/forbidden")
  def forbidden: Response = {
    Response(Status.Forbidden)
  }
}

/**
  *
  */
class FinagleClientTest extends AirframeSpec {

  val r = Router.add[FinagleClientTestApi]
  val d = finagleDefaultDesign
    .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port = IOUtil.randomPort, router = r))
    .noLifeCycleLogging

  "create client" in {

    d.build[FinagleServer] { server =>
      withResource(FinagleClient.newSyncClient(server.localAddress)) { client =>
        // Sending an implementation specific Request type
        val ret = client.send(Request("/")).contentString
        ret shouldBe "Ok"

        // Using HTTP request wrappers
        client.get[User]("/user/1") shouldBe User(1, "leo")

        client.list[Seq[User]]("/user") shouldBe Seq(User(1, "leo"))

        client.post[User]("/user", User(2, "yui")) shouldBe User(2, "yui")
        client.post[User, User]("/user", User(2, "yui")) shouldBe User(2, "yui")

        client.put[User]("/user", User(10, "aina")) shouldBe User(10, "aina")
        client.put[User, User]("/user", User(10, "aina")) shouldBe User(10, "aina")

        client.delete[User]("/user/1") shouldBe User(1, "xxx")
      }
    }
  }

  "fail request" in {
    d.build[FinagleServer] { server =>
      withResource(
        FinagleClient.newSyncClient(
          server.localAddress,
          config = FinagleClientConfig(
            retry = FinagleClient.defaultRetry.withMaxRetry(3).withBackOff(initialIntervalMillis = 1)))) { client =>
        warn(s"Starting http client failure tests")

        {
          // Test max retry failure
          val ex = intercept[HttpClientMaxRetryException] {
            val resp = client.get[String]("/busy")
          }
          warn(ex.getMessage)
          ex.retryContext.retryCount shouldBe 3
          ex.retryContext.maxRetry shouldBe 3
          val cause = ex.retryContext.lastError.asInstanceOf[HttpClientException]
          cause.status shouldBe HttpStatus.InternalServerError_500
        }

        {
          // Non retryable response
          val cause = intercept[HttpClientException] {
            client.get[String]("/forbidden")
          }
          warn(cause.getMessage)
          cause.status shouldBe HttpStatus.Forbidden_403
        }
      }
    }
  }

  "support https request" in {
    withResource(FinagleClient.newSyncClient("https://wvlet.org")) { client =>
      val page = client.get[String]("/airframe/")
      trace(page)
      page should include("<html")
    }
  }
}
