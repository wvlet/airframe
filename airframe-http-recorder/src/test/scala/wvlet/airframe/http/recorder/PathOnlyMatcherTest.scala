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
package wvlet.airframe.http.recorder

import com.twitter.finagle.http.{Request, Response}
import wvlet.airframe.Design
import wvlet.airframe.http.finagle.{FinagleClient, FinagleSyncClient}
import wvlet.airspec.AirSpec

/**
  *
  */
class PathOnlyMatcherTest extends AirSpec {
  protected override def design: Design = {
    val config = HttpRecorderConfig(requestMatcher = HttpRequestMatcher.PathOnlyMatcher)
    Design.newDesign
      .bind[HttpRecorderServer].toInstance(HttpRecorder.createInMemoryServer(config))
      .onStart { recorder => // Record responses
        {
          val req = Request("/hello")
          req.headerMap.put("Accept-Encoding", "gzip")
          val resp = Response()
          resp.contentString = "hello"
          recorder.recordIfNotExists(req, resp)
        }

        {
          val r = Request("/hello-hello")
          r.headerMap.put("Accept-Encoding", "gzip")
          val resp = Response()
          resp.contentString = "hello-hello"
          recorder.recordIfNotExists(r, resp)
        }
      }
      .bind[FinagleSyncClient].toProvider { recorder: HttpRecorderServer =>
        FinagleClient.newSyncClient(recorder.localAddress)
      }
  }

  def `support simple path matcher`(client: FinagleSyncClient): Unit = {
    client.get[String]("/hello") shouldBe "hello"
    client.get[String]("/hello-hello") shouldBe "hello-hello"
  }
}
