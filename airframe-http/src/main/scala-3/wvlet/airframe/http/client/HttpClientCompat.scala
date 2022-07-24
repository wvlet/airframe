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

import wvlet.airframe.http.HttpClientException
import wvlet.airframe.surface.Surface
import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.Future

/**
  * Scala 3 specific helper method to make an RPC request
  */
trait SyncClientCompat { self: SyncClient =>

  /**
    * Read the response as a specified type
    * @param request
    * @tparam Resp
    * @return
    *   a response translated to the specified type
    *
    * @throws HttpClientException
    *   if failed to read or process the response
    */
  inline def readAs[Resp](req: Request): Resp = {
    self.readAsInternal[Resp](req, Surface.of[Resp])
  }

  inline def call[Req, Resp](
      req: Request,
      requestContent: Req
  ): Resp = {
    self.callInternal[Req, Resp](req, Surface.of[Req], Surface.of[Resp], requestContent)
  }
}

trait AsyncClientCompat { self: AsyncClient =>
  inline def readAs[Resp](req: Request): Future[Resp] = {
    self.readAsInternal[Resp](req, Surface.of[Resp])
  }

  inline def call[Req, Resp](
      req: Request,
      requestContent: Req
  ): Future[Resp] = {
    self.callInternal[Req, Resp](req, Surface.of[Req], Surface.of[Resp], requestContent)
  }
}
