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
import com.twitter.finagle.http.Request
import com.twitter.util.{Await, Future}
import wvlet.airframe.AirframeSpec
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
      .bind[FinagleServerConfig].toInstance(FinagleServerConfig(port))

  "FinagleRouter" should {

    "work with Airframe" in {

      d.build[FinagleServer] { server =>
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
