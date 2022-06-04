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
trait SyncClientCompat { self: SyncClient =>
  def rpc[Req, Resp](
      resourcePath: String,
      request: Req
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

  def call[Req, Resp](
      request: Request,
      requestContent: Req
  ): Resp = macro HttpMacros.call0[Req, Resp]
}

trait AsyncClientCompat { self: AsyncClient =>
  def rpc[RequestType, ResponseType](
      resourcePath: String,
      request: RequestType
  ): Future[ResponseType] = macro HttpMacros.rpcSendAsync[RequestType, ResponseType]

  /**
    * Read the response as a specified type
    * @param request
    * @tparam Resp
    * @return
    *   a response translated to the specified type
    */
  def readAs[Resp](
      request: Request
  ): Future[Resp] = macro HttpMacros.read0Async[Resp]

  def call[Req, Resp](
      request: Request,
      requestContent: Req
  ): Future[Resp] = macro HttpMacros.call0Async[Req, Resp]
}
