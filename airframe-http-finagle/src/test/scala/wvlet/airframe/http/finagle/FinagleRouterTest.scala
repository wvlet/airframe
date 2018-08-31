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

import com.twitter.finagle.{Http, ListeningServer, Service, SimpleFilter}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.util.{Await, Future}
import javax.annotation.{PostConstruct, PreDestroy}
import wvlet.airframe.AirframeSpec
import wvlet.airframe._
import wvlet.airframe.http._
import wvlet.log.LogSupport
import wvlet.log.io.IOUtil

case class RichInfo(version: String, name: String, details: RichNestedInfo)
case class RichNestedInfo(serverType: String)

trait MyApi extends LogSupport {
  @Endpoint(path = "/v1/info")
  def getInfo: String = {
    "hello MyApi"
  }

  @Endpoint(path = "/v1/rich_info")
  def getRichInfo: RichInfo = {
    RichInfo("0.1", "MyApi", RichNestedInfo("test-server"))
  }

  @Endpoint(path = "/v1/future")
  def futureString: Future[String] = {
    Future.value("hello")
  }

  @Endpoint(path = "/v1/rich_info_future")
  def futureRichInfo: Future[RichInfo] = {
    Future.value(getRichInfo)
  }
}

case class MyServerConfig(port: Int)

trait MyApiServer extends LogSupport {
  private val config = bind[MyServerConfig]

  val service = new SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      service(request).rescue {
        case e: Throwable =>
          logger.warn(e.getMessage)
          Future.value(Response(Status.InternalServerError))
      }
    }
  } andThen bind[FinagleRouter] andThen
    new Service[Request, Response] {
      def apply(req: Request): Future[Response] = Future.value(Response())
    }

  var server: Option[ListeningServer] = None

  @PostConstruct
  def start {
    info(s"Starting the server at http://localhost:${config.port}")
    server = Some(Http.serve(s":${config.port}", service))
  }

  @PreDestroy
  def stop = {
    info(s"Stopping the server")
    server.map(_.close())
  }
}

/**
  *
  */
class FinagleRouterTest extends AirframeSpec {
  val port   = IOUtil.unusedPort
  val router = Router.of[MyApi]

  val d =
    finagleDefaultDesign
      .bind[Router].toInstance(router)
      .bind[MyApi].toSingleton
      .bind[MyServerConfig].toInstance(MyServerConfig(port))

  "FinagleRouter" should {

    "work with Airframe" in {

      d.build[MyApiServer] { server =>
        val client = Http.client
          .newService(s"localhost:${port}")
        val f1 = client(Request("/v1/info")).map { response =>
          debug(response.contentString)
        }
        val f2 = client(Request("/v1/rich_info")).map { r =>
          debug(r.contentString)
        }

        Await.result(f1.join(f2))

        // making many requests
        val futures = (0 until 5).map { x =>
          client(Request("/v1/rich_info")).map { response =>
            response.contentString
          }
        }

        val result = Await.result(Future.collect(futures))
        debug(result.mkString(", "))

        // Future response
        Await.result(client(Request("/v1/future")).map { response =>
          response.contentString
        }) shouldBe "hello"

        {
          val json = Await.result(client(Request("/v1/rich_info_future")).map { response =>
            response.contentString
          })

          json shouldBe """{"version":"0.1","name":"MyApi","details":{"serverType":"test-server"}}"""
        }

      }
    }
  }
}
