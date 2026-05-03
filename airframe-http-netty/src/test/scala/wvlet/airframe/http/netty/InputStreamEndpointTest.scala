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

import wvlet.airframe.http.*
import wvlet.airframe.http.client.SyncClient
import wvlet.airspec.AirSpec

import java.io.InputStream

object InputStreamEndpointTest {

  class InputStreamApi {
    @Endpoint(method = HttpMethod.POST, path = "/upload")
    def upload(body: InputStream): String = {
      val bytes = body.readAllBytes()
      s"received ${bytes.length} bytes"
    }

    @Endpoint(method = HttpMethod.POST, path = "/echo")
    def echo(body: InputStream): HttpMessage.Response = {
      val bytes = body.readAllBytes()
      Http.response(HttpStatus.Ok_200).withContent(bytes)
    }
  }
}

class InputStreamEndpointTest extends AirSpec {
  import InputStreamEndpointTest.*

  initDesign {
    _.add(
      Netty.server
        .withRouter(RxRouter.of[InputStreamApi])
        .designWithSyncClient
    )
  }

  test("receive small binary body via InputStream") { (client: SyncClient) =>
    val data    = "hello world".getBytes("UTF-8")
    val request = Http.POST("/upload").withContent(data)
    val resp    = client.send(request)
    resp.contentString shouldBe s"received ${data.length} bytes"
  }

  test("echo binary content via InputStream") { (client: SyncClient) =>
    val data = new Array[Byte](1024)
    scala.util.Random.nextBytes(data)
    val request = Http.POST("/echo").withContent(data)
    val resp    = client.send(request)
    resp.contentBytes shouldBe data
  }

  test("handle large body via InputStream") { (client: SyncClient) =>
    // Create a body larger than default 8MB threshold to trigger file-backed buffering
    val data = new Array[Byte](9 * 1024 * 1024)
    scala.util.Random.nextBytes(data)
    val request = Http.POST("/upload").withContent(data)
    val resp    = client.send(request)
    resp.contentString shouldBe s"received ${data.length} bytes"
  }

  test("handle empty body via InputStream") { (client: SyncClient) =>
    val request = Http.POST("/upload")
    val resp    = client.send(request)
    resp.contentString shouldBe "received 0 bytes"
  }
}
