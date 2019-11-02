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
import com.twitter.finagle.http
import com.twitter.util.Await
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.airspec.AirSpec

case class SampleResponse(id: Int, name: String)

trait TestMessagePackApi {
  @Endpoint(path = "/v1/hello")
  def hello: SampleResponse = {
    SampleResponse(1, "leo")
  }
}

/**
  *
  */
class MessagePackResponseTest extends AirSpec {
  def `support Accept: application/x-msgpack`: Unit = {
    newFinagleServerDesign(name = "msgpack-test-server", router = Router.of[TestMessagePackApi]).build[FinagleServer] {
      server =>
        withResource(Finagle.newClient(server.localAddress)) { client =>
          val req = http.Request("/v1/hello")
          req.accept = "application/x-msgpack"
          val msgpack = Await.result(client.send(req).map { x =>
            val c = x.content
            val b = new Array[Byte](c.length)
            c.write(b, 0)
            b
          })

          MessageCodec.of[SampleResponse].unpackMsgPack(msgpack) shouldBe Some(SampleResponse(1, "leo"))
        }
    }
  }
}
