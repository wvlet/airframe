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
package wvlet.airframe.http.grpc

import wvlet.airframe.http.Router
import wvlet.airframe.http.grpc.example.Greeter
import wvlet.airspec.AirSpec

object GrpcServerFactoryTest extends AirSpec {

  private val r = Router.add[Greeter]

  test("Build multiple gRPC servers") { f: GrpcServerFactory =>
    val s1 = f.newServer(gRPC.server.withName("grpc1").withRouter(r))
    val s2 = f.newServer(gRPC.server.withName("grpc2").withRouter(r))
  }
}
