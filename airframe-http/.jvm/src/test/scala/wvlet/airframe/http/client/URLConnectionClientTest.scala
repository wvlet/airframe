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
package wvlet.airframe.http.client
import wvlet.airframe.control.Control
import wvlet.airframe.http.{Http, HttpStatus}
import wvlet.airspec.AirSpec

/**
  *
 */
class URLConnectionClientTest extends AirSpec {

  test("Create an http client") {
    Control.withResource(Http.client.newSyncClient("https://wvlet.org")) { client =>
      val resp = client.sendSafe(Http.GET("/airframe/index.html"))
      debug(resp)
      resp.status shouldBe HttpStatus.Ok_200
      debug(resp.contentString)

      val errorResp = client.sendSafe(Http.GET("/non-existing-path"))
      debug(errorResp)
      errorResp.status shouldBe HttpStatus.NotFound_404
      debug(errorResp.contentString)
    }
  }

}
