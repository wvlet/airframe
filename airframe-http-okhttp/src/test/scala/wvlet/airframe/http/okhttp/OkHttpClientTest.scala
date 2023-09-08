package wvlet.airframe.http.okhttp

import wvlet.airframe.Design
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.client.{AsyncClient, SyncClient}
import wvlet.airframe.http.netty.{Netty, NettyServer}
import wvlet.airframe.http.*
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

case class User(id: Int, name: String, requestId: String) {
  def withRequestId(newRequestId: String): User = User(id, name, newRequestId)
}

case class UserRequest(id: Int, name: String)
case class DeleteRequestBody(force: Boolean)

class NettyTestApi extends LogSupport {
  import wvlet.airframe.http.{Endpoint, HttpMethod}

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
    Http.response(HttpStatus.InternalServerError_500)
  }

  @Endpoint(method = HttpMethod.GET, path = "/forbidden")
  def forbidden: Response = {
    Http.response(HttpStatus.Forbidden_403)
  }

  @Endpoint(method = HttpMethod.GET, path = "/readtimeout")
  def readTimeout: String = {
    Thread.sleep(3000)
    "called readTimeout method"
  }

  @Endpoint(method = HttpMethod.GET, path = "/response")
  def rawResponse: Response = {
    val r = Http.response(HttpStatus.Ok_200)
    r.withContent("raw response")
  }
}

class OkHttpClientTest extends AirSpec {
  private val r = RxRouter.of[NettyTestApi]

  override protected def design = {
    Netty.server
      .withRouter(r).design
      .bind[SyncClient].toProvider { (server: NettyServer) =>
        OkHttp.client.newSyncClient(server.localAddress)
      }
      .bind[AsyncClient].toProvider { (server: NettyServer) =>
        OkHttp.client.newAsyncClient(server.localAddress)
      }
  }

  def addRequestId: HttpMultiMap => HttpMultiMap = { (request: HttpMultiMap) =>
    request.add("X-Request-Id", "10")
  }

  test("create async client") { (client: AsyncClient) =>
    test("readAs") {
      client.readAs[String](Http.GET("/")).toRx.map { v =>
        v shouldBe "Ok"
      }
    }

    test("call") {
      client.call[UserRequest, User](Http.GET("/user/info"), UserRequest(2, "kai")).toRx.map { v =>
        v shouldBe User(2, "kai", "N/A")
      }
    }
  }

  test("create sync client") { (client: SyncClient) =>
    // Sending an implementation specific Request type
    val ret = client.send(Http.GET("/")).contentString
    ret shouldBe "Ok"

    // Using HTTP request wrappers
    client.readAs[User](Http.GET("/user/1")) shouldBe User(1, "leo", "N/A")
    client.call[UserRequest, User](Http.GET("/user/info"), UserRequest(2, "kai")) shouldBe User(2, "kai", "N/A")
    client.call[UserRequest, User](Http.GET("/user/info2"), UserRequest(2, "kai")) shouldBe User(2, "kai", "N/A")
    client.readAs[Seq[User]](Http.GET("/user")) shouldBe Seq(User(1, "leo", "N/A"))

    client.call[User, User](Http.POST("/user"), User(2, "yui", "N/A")) shouldBe User(2, "yui", "N/A")
    client.call[User, User](Http.PUT("/user"), User(10, "aina", "N/A")) shouldBe User(10, "aina", "N/A")
    client.call[User, User](Http.PATCH("/user"), User(20, "joy", "N/A")) shouldBe User(20, "joy", "N/A")

    client.readAs[User](Http.DELETE("/user/1")) shouldBe User(1, "xxx", "N/A")
    client.call[DeleteRequestBody, User](Http.DELETE("/user/1"), DeleteRequestBody(true)) shouldBe User(1, "xxx", "N/A")

    // Get a response as is
    client.send(Http.GET("/response")).contentString shouldBe "raw response"

    // Using a custom HTTP header
    client.readAs[User](Http.GET("/user/1").withHeader(addRequestId)) shouldBe User(1, "leo", "10")
    client.call[UserRequest, User](
      Http.GET("/user/info").withHeader(addRequestId),
      UserRequest(2, "kai")
    ) shouldBe User(
      2,
      "kai",
      "10"
    )
    client.call[UserRequest, User](
      Http.GET("/user/info2").withHeader(addRequestId),
      UserRequest(2, "kai")
    ) shouldBe User(
      2,
      "kai",
      "10"
    )

    client.readAs[Seq[User]](Http.GET("/user").withHeader(addRequestId)) shouldBe Seq(User(1, "leo", "10"))

    client.call[User, User](Http.POST("/user").withHeader(addRequestId), User(2, "yui", "N/A")) shouldBe User(
      2,
      "yui",
      "10"
    )
    client.call[User, User](Http.POST("/user").withHeader(addRequestId), User(2, "yui", "N/A")) shouldBe User(
      2,
      "yui",
      "10"
    )
    client
      .call[User, Response](
        Http.POST("/user").withHeader(addRequestId),
        User(2, "yui", "N/A")
      ).contentString shouldBe """{"id":2,"name":"yui","requestId":"10"}"""

    client
      .call[User, User](Http.PUT("/user").withHeader(addRequestId), User(10, "aina", "N/A")) shouldBe User(
      10,
      "aina",
      "10"
    )
    client
      .call[User, Response](
        Http.PUT("/user").withHeader(addRequestId),
        User(10, "aina", "N/A")
      ).contentString shouldBe """{"id":10,"name":"aina","requestId":"10"}"""

    client.call[User, User](Http.PATCH("/user").withHeader(addRequestId), User(20, "joy", "N/A")) shouldBe User(
      20,
      "joy",
      "10"
    )
    client
      .call[User, Response](
        Http.PATCH("/user").withHeader(addRequestId),
        User(20, "joy", "N/A")
      ).contentString shouldBe """{"id":20,"name":"joy","requestId":"10"}"""

    client.readAs[User](Http.DELETE("/user/1").withHeader(addRequestId)) shouldBe User(1, "xxx", "10")
    client.call[DeleteRequestBody, User](
      Http.DELETE("/user/1").withHeader(addRequestId),
      DeleteRequestBody(true)
    ) shouldBe User(
      1,
      "xxx",
      "10"
    )
    client
      .send(
        Http.DELETE("/user/1").withHeader(addRequestId)
      ).contentString shouldBe """{"id":1,"name":"xxx","requestId":"10"}"""
  }

  test(
    "fail request",
    design = _.bind[SyncClient].toProvider { (server: NettyServer) =>
      OkHttp.client
        .withRetryContext(_.withMaxRetry(3))
        .noCircuitBreaker
        .newSyncClient(
          // Test for the full URI
          s"http://${server.localAddress}"
        )
    }
  ) { (client: SyncClient) =>
    warn("Starting http client failure tests")

    test("max retry failure") {
      // Test max retry failure
      val ex = intercept[HttpClientMaxRetryException] {
        client.send(Http.GET("/busy"))
      }
      warn(ex.getMessage)
      ex.retryContext.retryCount shouldBe 3
      ex.retryContext.maxRetry shouldBe 3
      // val cause = ex.retryContext.lastError.asInstanceOf[HttpClientException]
      // cause.status shouldBe HttpStatus.InternalServerError_500
    }

    test("non-retryable response") {
      // Non retryable response
      val cause = intercept[HttpClientException] {
        client.send(Http.GET("/forbidden"))
      }
      cause.status shouldBe HttpStatus.Forbidden_403
    }
  }

  test(
    "read timeout",
    design = _.bind[SyncClient].toProvider { (server: NettyServer) =>
      OkHttp.client
        .withReadTimeout(Duration(10, TimeUnit.MILLISECONDS))
        .withRetryContext(_.withMaxRetry(1))
        .newSyncClient(s"http://${server.localAddress}")
    }
  ) { (client: SyncClient) =>
    warn("Starting a read timeout test")

    val ex = intercept[HttpClientMaxRetryException] {
      // sleeps for 3 seconds, which means a timeout happens
      client.readAs[String](Http.GET("/readtimeout"))
    }
    warn(ex.getMessage)
    ex.retryContext.retryCount shouldBe 1
    ex.retryContext.maxRetry shouldBe 1
    ex.retryContext.lastError.getClass shouldBe classOf[java.net.SocketTimeoutException]
  }

  test("support https request") {
    withResource(OkHttp.client.newSyncClient("https://wvlet.org")) { client =>
      val page = client.readAs[String](Http.GET("/airframe/"))
      trace(page)
      page.contains("<html") shouldBe true
    }
    1
  }
}
