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
import wvlet.airframe.control.Control.withResource
import wvlet.airframe.http.client.SyncClient
import wvlet.airframe.http.{Http, HttpMethod, HttpServer, HttpStatus}
import wvlet.airspec.AirSpec

class NettyServerTest extends AirSpec {
  initDesign(_ + Netty.server.design)

  test("NettyServer should be available") { (server: NettyServer) =>
    test("double start should be ignored") {
      server.start
    }
  }

  test("can't start server after closing it") { (server: NettyServer) =>
    server.close()
    intercept[IllegalStateException] {
      server.start
    }
  }

  test("safely close multiple times") { (server: HttpServer) =>
    server.close()
    server.close()
  }
}
