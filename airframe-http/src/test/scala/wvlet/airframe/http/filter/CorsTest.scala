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
package wvlet.airframe.http.filter

import wvlet.airframe.http.{Http, HttpMessage, HttpMethod, RxHttpEndpoint, RxHttpFilter}
import wvlet.airframe.rx.Rx
import wvlet.airspec.AirSpec

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

class CorsTest extends AirSpec {

  private val maxAge = Duration(1, TimeUnit.HOURS)

  private def policy = Cors.Policy(
    allowsOrigin = {
      case origin if origin.startsWith("console") => Some(origin)
      case origin if origin.endsWith("td.com")    => Some(origin)
      case _                                      => None
    },
    allowsMethods = method => Some(Seq("GET")),
    allowsHeaders = headers => Some(headers),
    exposedHeaders = Seq("x-airframe-rpc"),
    supportsCredentials = true,
    maxAge = Some(maxAge)
  )

  private val corsFilter = Cors.newFilter(policy)
  private val endpoint = new RxHttpEndpoint {
    override def apply(request: HttpMessage.Request): Rx[HttpMessage.Response] = {
      Rx.single(Http.response())
    }
  }

  test("CorsFilter handles preflight requests") {
    val request = Http
      .OPTIONS("/")
      .withHeader("Origin", "thetd.com")
      .withHeader("Access-Control-Request-Method", "BRR")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe Some("thetd.com")
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe Some("true")
      resp.getHeader("Access-Control-Allow-Methods") shouldBe Some("GET")
      resp.getHeader("Vary") shouldBe Some("Origin")
      resp.getHeader("Access-Control-Max-Age") shouldBe Some(maxAge.toSeconds.toString)
      resp.contentString shouldBe empty
    }
  }

  test("CorsFilter responds to invalid preflight requests without CORS headers") {
    val request = Http
      .OPTIONS("/")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe empty
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe empty
      resp.getHeader("Access-Control-Allow-Methods") shouldBe empty
      resp.getHeader("Access-Control-Max-Age") shouldBe empty
      resp.getHeader("Vary") shouldBe Some("Origin")
      resp.contentString shouldBe empty
    }
  }

  test("CorsFilter responds to unacceptable cross-origin requests without CORS headers") {
    val request = Http
      .OPTIONS("/")
      .withHeader("Origin", "theclub")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe empty
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe empty
      resp.getHeader("Access-Control-Allow-Methods") shouldBe empty
      resp.getHeader("Access-Control-Max-Age") shouldBe empty
      resp.getHeader("Vary") shouldBe Some("Origin")
      resp.contentString shouldBe empty
    }
  }

  test("CorsFilter handles simple requests") {
    val request = Http
      .GET("/")
      .withHeader("Origin", "thetd.com")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe Some("thetd.com")
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe Some("true")
      resp.getHeader("Access-Control-Expose-Headers") shouldBe Some("x-airframe-rpc")
      resp.contentString shouldBe empty
    }
  }

  test("CorsFilter handles simple requests with multiple origins") {
    val request = Http
      .GET("/")
      .withHeader("Origin", "thetd.com,console")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe Some("thetd.com,console")
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe Some("true")
      resp.getHeader("Access-Control-Expose-Headers") shouldBe Some("x-airframe-rpc")
      resp.contentString shouldBe empty
    }
  }

  test("CorsFilter does not add response headers to simple requests if request headers aren't present") {
    val request = Http
      .GET("/")

    corsFilter.apply(request, endpoint).map { resp =>
      resp.getHeader("Access-Control-Allow-Origin") shouldBe empty
      resp.getHeader("Access-Control-Allow-Credentials") shouldBe empty
      resp.getHeader("Access-Control-Expose-Headers") shouldBe empty
      resp.contentString shouldBe empty
    }
  }
}
