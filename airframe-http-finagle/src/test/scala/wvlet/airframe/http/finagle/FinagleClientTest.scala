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
import com.twitter.util.Await
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http._
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class User(id: Int, name: String, requestId: String) {
  def withRequestId(newRequestId: String): User = User(id, name, newRequestId)
}

case class UserRequest(id: Int, name: String)
case class DeleteRequestBody(force: Boolean)

trait FinagleClientTestApi extends LogSupport {
  @Endpoint(method = HttpMethod.GET, path = "/")
  def info: String = {
    "Ok"
  }

  private def getRequestId(request: Request): String = {
    request.header.getOrElse("X-Request-Id", "N/A")
  }

  @Endpoint(method = HttpMethod.GET, path = "/user/:id")
  def get(id: Int, request: Request): User = {
    User(id, "leo", getRequestId(request))
  }

  @Endpoint(method = HttpMethod.GET, path = "/user/info")
  def getResource(id: Int, name: String, request: Request): User = {
    User(id, name, getRequestId(request))
  }

  @Endpoint(method = HttpMethod.GET, path = "/user/info2")
  def getResource(query: UserRequest, request: Request): User = {
    User(query.id, query.name, getRequestId(request))
  }

  @Endpoint(method = HttpMethod.GET, path = "/user")
  def list(request: Request): Seq[User] = {
    Seq(User(1, "leo", getRequestId(request)))
  }

  @Endpoint(method = HttpMethod.POST, path = "/user")
  def create(newUser: User, request: Request): User = {
    newUser.withRequestId(getRequestId(request))
  }

  @Endpoint(method = HttpMethod.DELETE, path = "/user/:id")
  def delete(id: Int, request: Request): User = {
    User(id, "xxx", getRequestId(request))
  }

  @Endpoint(method = HttpMethod.PUT, path = "/user")
  def put(updatedUser: User, request: Request): User = {
    updatedUser.withRequestId(getRequestId(request))
  }

  @Endpoint(method = HttpMethod.PATCH, path = "/user")
  def patch(updatedUser: User, request: Request): User = {
    updatedUser.withRequestId(getRequestId(request))
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

  @Endpoint(method = HttpMethod.GET, path = "/response")
  def rawResponse: Response = {
    val r = Response(Status.Ok)
    r.setContentString("raw response")
    r
  }
}

/**
  *
  */
class FinagleClientTest extends AirSpec {
  val r = Router.add[FinagleClientTestApi]
  val d = finagleDefaultDesign
    .bind[FinagleServerConfig].toInstance(
      FinagleServerConfig(name = "test-server", router = r)
    )
    .noLifeCycleLogging

  def `create client`: Unit = {
    def addRequestId(request: Request): Request = {
      request.headerMap.put("X-Request-Id", "10")
      request
    }

    d.build[FinagleServer] { server =>
      withResource(FinagleClient.newSyncClient(server.localAddress)) { client =>
        // Sending an implementation specific Request type
        val ret = client.send(Request("/")).contentString
        ret shouldBe "Ok"

        // Using HTTP request wrappers
        client.get[User]("/user/1") shouldBe User(1, "leo", "N/A")
        client.getResource[UserRequest, User]("/user/info", UserRequest(2, "kai")) shouldBe User(2, "kai", "N/A")
        client.getResource[UserRequest, User]("/user/info2", UserRequest(2, "kai")) shouldBe User(2, "kai", "N/A")
        client.list[Seq[User]]("/user") shouldBe Seq(User(1, "leo", "N/A"))

        client.post[User]("/user", User(2, "yui", "N/A")) shouldBe User(2, "yui", "N/A")
        client.postOps[User, User]("/user", User(2, "yui", "N/A")) shouldBe User(2, "yui", "N/A")

        client.put[User]("/user", User(10, "aina", "N/A")) shouldBe User(10, "aina", "N/A")
        client.putOps[User, User]("/user", User(10, "aina", "N/A")) shouldBe User(10, "aina", "N/A")

        client.patch[User]("/user", User(20, "joy", "N/A")) shouldBe User(20, "joy", "N/A")
        client.patchOps[User, User]("/user", User(20, "joy", "N/A")) shouldBe User(20, "joy", "N/A")

        client.delete[User]("/user/1") shouldBe User(1, "xxx", "N/A")
        client.deleteOps[DeleteRequestBody, User]("/user/1", DeleteRequestBody(true)) shouldBe User(1, "xxx", "N/A")

        // Get a response as is
        client.get[Response]("/response").contentString shouldBe "raw response"

        // Using a custom HTTP header
        client.get[User]("/user/1", addRequestId) shouldBe User(1, "leo", "10")
        client.getResource[UserRequest, User]("/user/info", UserRequest(2, "kai"), addRequestId) shouldBe User(
          2,
          "kai",
          "10"
        )
        client.getResource[UserRequest, User]("/user/info2", UserRequest(2, "kai"), addRequestId) shouldBe User(
          2,
          "kai",
          "10"
        )

        client.list[Seq[User]]("/user", addRequestId) shouldBe Seq(User(1, "leo", "10"))

        client.post[User]("/user", User(2, "yui", "N/A"), addRequestId) shouldBe User(2, "yui", "10")
        client.postOps[User, User]("/user", User(2, "yui", "N/A"), addRequestId) shouldBe User(2, "yui", "10")
        client.postRaw[User]("/user", User(2, "yui", "N/A"), addRequestId).contentString shouldBe """{"id":2,"name":"yui","requestId":"10"}"""

        client.put[User]("/user", User(10, "aina", "N/A"), addRequestId) shouldBe User(10, "aina", "10")
        client.putOps[User, User]("/user", User(10, "aina", "N/A"), addRequestId) shouldBe User(10, "aina", "10")
        client.putRaw[User]("/user", User(10, "aina", "N/A"), addRequestId).contentString shouldBe """{"id":10,"name":"aina","requestId":"10"}"""

        client.patch[User]("/user", User(20, "joy", "N/A"), addRequestId) shouldBe User(20, "joy", "10")
        client.patchOps[User, User]("/user", User(20, "joy", "N/A"), addRequestId) shouldBe User(20, "joy", "10")
        client.patchRaw[User]("/user", User(20, "joy", "N/A"), addRequestId).contentString shouldBe """{"id":20,"name":"joy","requestId":"10"}"""

        client.delete[User]("/user/1", addRequestId) shouldBe User(1, "xxx", "10")
        client.deleteOps[DeleteRequestBody, User]("/user/1", DeleteRequestBody(true), addRequestId) shouldBe User(
          1,
          "xxx",
          "10"
        )
        client.deleteRaw("/user/1", addRequestId).contentString shouldBe """{"id":1,"name":"xxx","requestId":"10"}"""
      }
    }
  }

  def `fail request`: Unit = {
    d.build[FinagleServer] { server =>
      withResource(
        Finagle.client
          .withMaxRetry(3)
          .withBackOff(initialIntervalMillis = 1)
          .newSyncClient(server.localAddress)
      ) { client =>
        warn("Starting http client failure tests")

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

  def `support https request`: Unit = {
    withResource(Finagle.newSyncClient("https://wvlet.org")) { client =>
      val page = client.get[String]("/airframe/")
      trace(page)
      page.contains("<html") shouldBe true
    }
  }

  def `support sendRaw`: Unit = {
    withResource(Finagle.newClient("https://wvlet.org")) { client =>
      val r = client.sendRaw(Request("/airframe/")).map { x =>
        x.contentString.contains("<html") shouldBe true
      }
      Await.result(r)
    }
  }
}
