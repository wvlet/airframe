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
trait RPCSyncClientBase { self: SyncClient =>
  inline def rpc[RequestType, ResponseType](
      resourcePath: String,
      request: RequestType,
      requestFilter: Request => Request
  ): ResponseType = {
    self.sendRPC(resourcePath, Surface.of[RequestType], request, Surface.of[ResponseType], requestFilter).asInstanceOf[ResponseType]
  }

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
  inline def readAs[Resp](request: Request, requestFilter: Request => Request = identity): Resp = {
    self.sendRaw[Resp](request, Surface.of[Resp], requestFilter)
  }

  inline def call[Req, Resp](request: Request, requestContent: Req, requestFilter: Request => Request = identity): Resp = {
    self.sendRaw[Req, Resp](request, Surface.of[Req], Surface.of[Resp], requestContent, requestFilter)
  }
}


trait RPCAsyncClientBase { self: AsyncClient =>
  inline def rpc[RequestType, ResponseType](
    resourcePath: String,
    request: RequestType,
    requestFilter: Request => Request
  ): Future[ResponseType] = {
    self.sendRPC(resourcePath, Surface.of[RequestType], request, Surface.of[ResponseType], requestFilter).asInstanceOf[Future[ResponseType]]
  }
}
