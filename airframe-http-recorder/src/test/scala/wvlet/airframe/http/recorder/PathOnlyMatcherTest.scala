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

import wvlet.airframe.Design
import wvlet.airframe.http.Http
import wvlet.airframe.http.client.SyncClient
import wvlet.airspec.AirSpec

/**
  */
class PathOnlyMatcherTest extends AirSpec {
  protected override def design: Design = {
    val config = HttpRecorderConfig(requestMatcher = HttpRequestMatcher.PathOnlyMatcher)
    Design.newDesign
      .bind[HttpRecorderServer].toInstance(HttpRecorder.createInMemoryServer(config))
      .onStart { recorder => // Record responses
        {
          val req = Http
            .GET("/hello")
            .withHeader("Accept-Encoding", "gzip")
          val resp = Http
            .response()
            .withContent("hello")
          recorder.recordIfNotExists(req, resp)
        }

        {
          val r = Http
            .GET("/hello-hello")
            .withHeader("Accept-Encoding", "gzip")
          val resp = Http
            .response()
            .withContent("hello-hello")
          recorder.recordIfNotExists(r, resp)
        }
      }
      .bind[SyncClient].toProvider { (recorder: HttpRecorderServer) =>
        Http.client.newSyncClient(recorder.localAddress)
      }
  }

  test("support simple path matcher") { (client: SyncClient) =>
    client.readAs[String](Http.GET("/hello")) shouldBe "hello"
    client.readAs[String](Http.GET("/hello-hello")) shouldBe "hello-hello"
  }
}
