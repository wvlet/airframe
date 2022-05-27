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
