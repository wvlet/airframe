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

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Request, Response, Status, Version}
import com.twitter.util.{Await, Future}
import wvlet.airframe.AirframeSpec
import wvlet.airframe.http._
import wvlet.log.LogSupport

/**
  *
  */
trait SampleApp {
  @Endpoint(path = "/auth")
  def needsAuth(request: Request): String = {
    "passed"
  }
}

trait NoAuth {
  @Endpoint(path = "/noauth")
  def get = "hello"
}

class LogStore extends LogSupport {
  var log                     = Seq.empty[String]
  def lastLog: Option[String] = log.lastOption

  def add(message: String): Unit = {
    debug(s"response log: ${message}")
    log :+= message
  }
}

trait LogFilterExample extends FinagleFilter {
  import wvlet.airframe._
  private val logStore = bind[LogStore]

  override def apply(request: Request, context: FinagleContext): Future[Response] = {
    context(request).map { response =>
      logStore.add(s"${response.statusCode} ${request.path}")
      response
    }
  }
}

trait AuthFilterExample extends FinagleFilter with LogSupport {
  override def apply(request: Request, context: FinagleContext): Future[Response] = {
    debug(s"visit auth filter: ${request} ")
    request.header.get("Authorization") match {
      case Some("valid-user") =>
        context(request)
      case _ =>
        Future.value(Response(Version.Http11, Status.Forbidden))
    }
  }
}

object BadRequestFilter extends FinagleFilter {
  override def apply(request: Request, context: FinagleContext): Future[Response] = {
    val resp = Response(Version.Http11, Status.BadRequest)
    resp.contentString = "bad requet"
    Future.value(resp)
  }
}

/**
  *
  */
class HttpFilterTest extends AirframeSpec {

  "apply filter before the route" in {

    val router =
      Router
        .filter[LogFilterExample]
        .andThen(
          Router(
            Router.filter[AuthFilterExample].andThen[SampleApp],
            Router.add[NoAuth]
          )
        )

    debug(router)

    val myLogStore = new LogStore

    val d = newFinagleServerDesign(router)
      .bind[LogStore].toInstance(myLogStore)
      .noLifeCycleLogging

    d.build[FinagleServer] { server =>
      val address = server.localAddress

      val client = Http.client.newService(address)

      {
        val r = Await.result(client(Request("/auth")))
        debug(r)
        r.statusCode shouldBe 403
        myLogStore.lastLog shouldBe Some("403 /auth")
      }

      {
        val req = Request("/auth")
        req.authorization = "valid-user"
        val r = Await.result(client(req))
        debug(r)
        r.statusCode shouldBe 200
        r.contentString shouldBe "passed"
        myLogStore.lastLog shouldBe Some("200 /auth")
      }

      {
        val r = Await.result(client(Request("/noauth")))
        debug(r)
        r.statusCode shouldBe 200
        r.contentString shouldBe "hello"

        myLogStore.lastLog shouldBe Some("200 /noauth")
      }
    }
  }

}
