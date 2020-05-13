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
import java.io.FileInputStream

import com.twitter.finagle.http.Request
import wvlet.airframe.Design
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.finagle.filter.HttpAccessLogWriter.JSONHttpAccessLogWriter
import wvlet.airframe.http.finagle.filter.{HttpAccessLogConfig, HttpAccessLogFilter, HttpAccessLogWriter}
import wvlet.airframe.http.{Endpoint, HttpStatus, Router}
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil

/**
  *
 */
object HttpAccessLogTest extends AirSpec {

  trait MyService {
    @Endpoint(path = "/user/:id")
    def user(id: String) = {
      s"hello user:${id}"
    }
  }

  private val router = Router.add[MyService]

  test("Record access logs") {
    val inMemoryLogWriter = HttpAccessLogWriter.inMemoryLogWriter

    test(
      "contain all basic parameters",
      design = Finagle.server
        .withLoggingFilter(new HttpAccessLogFilter(httpAccessLogWriter = inMemoryLogWriter))
        .withRouter(router).designWithSyncClient
    ) { client: FinagleSyncClient =>
      val resp = client.get[String](
        "/user/1?session_id=xxx",
        { r: Request =>
          // Add a custom header
          r.headerMap.put("X-App-Version", "1.0")
          r
        }
      )
      resp shouldBe "hello user:1"

      val log = inMemoryLogWriter.getLogs.head
      debug(log)
      log.get("time") shouldBe defined
      log.get("method") shouldBe Some("GET")
      log.get("path") shouldBe Some("/user/1")
      log.get("uri") shouldBe Some("/user/1?session_id=xxx")
      log.get("query_string") shouldBe Some("session_id=xxx")
      log.get("request_size") shouldBe Some(0)
      log.get("remote_host") shouldBe defined
      log.get("remote_port") shouldBe defined
      log.get("response_time_ms") shouldBe defined
      log.get("status_code") shouldBe Some(200)
      log.get("status_code_name") shouldBe Some(HttpStatus.Ok_200.reason)
      // Custom headers
      log.get("x_app_version") shouldBe Some("1.0")
    }
  }

  test("JSON access log") {
    IOUtil.withTempFile("target/http_access_log_test.json") { f =>
      test(
        "Write logs in JSON",
        design = Finagle.server
          .withRouter(router)
          .withLoggingFilter(
            new HttpAccessLogFilter(
              httpAccessLogWriter = new JSONHttpAccessLogWriter(HttpAccessLogConfig(fileName = f.getPath()))
            )
          )
          .designWithSyncClient
      ) { client: FinagleSyncClient =>
        val resp = client.get[String]("/user/2")
        resp shouldBe "hello user:2"
      }

      // Read the JSON log file
      val json = IOUtil.readAsString(f)
      debug(json)

      // Parse the JSON log
      val log = MessageCodec.of[Map[String, Any]].fromJson(json)
      debug(log)
      log.get("time") shouldBe defined
      log.get("method") shouldBe Some("GET")
      log.get("path") shouldBe Some("/user/2")
      log.get("uri") shouldBe Some("/user/2")
      log.get("query_string") shouldBe empty
      log.get("request_size") shouldBe Some(0)
      log.get("remote_host") shouldBe defined
      log.get("remote_port") shouldBe defined
      log.get("response_time_ms") shouldBe defined
      log.get("status_code") shouldBe Some(200)
      log.get("status_code_name") shouldBe Some(HttpStatus.Ok_200.reason)
    }
  }

}
