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

import wvlet.airframe.{Design, newDesign}
import wvlet.airframe.http.{ChannelConfig, Http, HttpMessage, HttpStatus, HttpSyncClient}
import wvlet.airspec.AirSpec
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Future}

class ClientLoggingFilterTest extends AirSpec {

  class DummyHttpChannel extends HttpChannel with LogSupport {
    override def send(req: HttpMessage.Request, channelConfig: ChannelConfig): HttpMessage.Response = {
      Http.response(HttpStatus.Ok_200).withJson("""{"message":"hello"}""")
    }

    override def sendAsync(req: HttpMessage.Request, channelConfig: ChannelConfig): Future[HttpMessage.Response] = ???

    override private[client] implicit def executionContext: ExecutionContext = ???
    override def close(): Unit                                               = {}
  }

  protected override def design: Design = {
    newDesign.bind[SyncClient].toInstance {
      val filter = new ClientLoggingFilter
      new SyncClientImpl(new DummyHttpChannel, Http.client.withClientFilter(filter))
    }
  }

  test("test") { (client: SyncClient) =>
    client.send(Http.GET("/"))
  }

}
