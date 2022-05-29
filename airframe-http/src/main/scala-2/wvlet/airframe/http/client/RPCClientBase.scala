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
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.impl.HttpMacros

import scala.concurrent.Future
import scala.language.experimental.macros

/**
  * Scala 2 specific helper method to make an RPC request
  */
trait RPCSyncClientBase { self: SyncClient =>
  def rpc[Req, Resp](
      resourcePath: String,
      request: Req,
      requestFilter: Request => Request
  ): Resp = macro HttpMacros.rpcSend[Req, Resp]

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
  def readAs[Resp](
      request: Request
  ): Resp = macro HttpMacros.read0[Resp]

  /**
    * Read the response as a specified type
    * @param request
    * @tparam Resp
    * @return
    *   response translated to the specified type
    *
    * @throws HttpClientException
    *   if failed to read or process the response
    */
  def readAs[Resp](
      request: Request,
      requestFilter: Request => Request
  ): Resp = macro HttpMacros.read1[Resp]

  def call[Req, Resp](
      request: Request,
      requestContent: Req
  ): Resp = macro HttpMacros.call0[Req, Resp]

  def call[Req, Resp](
      request: Request,
      requestContent: Req,
      requestFilter: Request => Request
  ): Resp = macro HttpMacros.call1[Req, Resp]
}

trait RPCAsyncClientBase { self: AsyncClient =>
  def rpc[RequestType, ResponseType](
      resourcePath: String,
      request: RequestType,
      requestFilter: Request => Request
  ): Future[ResponseType] = macro HttpMacros.rpcSendAsync[RequestType, ResponseType]
}
