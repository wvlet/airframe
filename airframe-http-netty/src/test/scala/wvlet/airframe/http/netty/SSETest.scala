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
package wvlet.airframe.http.netty

import wvlet.airframe.http.{Endpoint, Http, RxRouter}
import wvlet.airframe.http.HttpMessage.{Response, ServerSentEvent}
import wvlet.airframe.http.client.AsyncClient
import wvlet.airspec.AirSpec

class SSEApi {
  @Endpoint("/v1/sse")
  def sse(): Response = {
    Http
      .response()
      .withContentType("text/event-stream")
      .withContent(s"""data: hello stream
           |
           |data: another stream message
           |data: with two lines
           |
           |event: custom-event
           |data: hello custom event
           |
           |: this is a comment
           |
           |id: 123
           |data: hello again
           |
           |id: 1234
           |event: custom-event
           |data: hello again 2
           |
           |retry: 1000
           |data: need to retry
           |""".stripMargin)
  }
}

class SSETest extends AirSpec {
  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[SSEApi])
        .designWithAsyncClient
    )
  }

  test("read sse events") { (client: AsyncClient) =>
    val rx = client.send(
      Http.GET("/v1/sse")
    )
    rx.map { resp =>
      resp.statusCode shouldBe 200

      val events = resp.events.map { e =>
        debug(e)
        e
      }.toSeq

      val expected = List(
        ServerSentEvent(data = "hello stream"),
        ServerSentEvent(data = "another stream message\nwith two lines"),
        ServerSentEvent(event = Some("custom-event"), data = "hello custom event"),
        ServerSentEvent(id = Some("123"), data = "hello again"),
        ServerSentEvent(id = Some("1234"), event = Some("custom-event"), data = "hello again 2"),
        ServerSentEvent(retry = Some(1000), data = "need to retry")
      )

      trace(events.mkString("\n"))
      trace(expected.mkString("\n"))
      events shouldBe expected
    }
  }
}
