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

import wvlet.airframe.control.Control
import wvlet.airframe.http.Http
import wvlet.airspec.AirSpec

class NettyServerTest extends AirSpec {
  test("Start a server") {
    Control.withResource(Netty.server.newServer) { server =>
      Control.withResource(Http.client.newSyncClient(server.localAddress)) { client =>
        val resp = client.send(Http.GET("/v1/status"))
        info(resp)
      }
    }
  }
}
