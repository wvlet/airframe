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

import wvlet.airframe.Design
import wvlet.airframe.http.{Endpoint, Router}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

class HttpParamExample extends LogSupport {
  @Endpoint(path = "/api/get")
  def get(p1: Option[Int]): Option[Int] = {
    info(p1)
    p1
  }
}

/**
  *
  */
class HttpParamTest extends AirSpec {

  protected override def design: Design = {
    val r = Router.add[HttpParamExample]
    newFinagleServerDesign(router = r)
      .bind[FinagleSyncClient].toProvider { server: FinagleServer =>
        Finagle.client.noRetry
          .newSyncClient(server.localAddress)
      }
  }

  def `support Option[Int]`(client: FinagleSyncClient): Unit = {
    val result = client.get[Option[Int]]("/api/get")
    result shouldBe empty
  }
}
