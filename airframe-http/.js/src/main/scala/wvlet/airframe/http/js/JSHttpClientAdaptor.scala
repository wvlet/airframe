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
package wvlet.airframe.http.js

import wvlet.airframe.http.{Http, HttpMessage}

import scala.concurrent.Future
import scala.reflect.runtime.universe

/**
  * An http client for Scala.js that implements only sendSafe for supporting RPC
  * @param client
  */
class JSHttpClientAdaptor(client: JSHttpClient) extends Http.AsyncClient {
  override def close(): Unit = {
    // Nothing to do
  }

  override def sendSafe(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = {
    client.sendRaw(req, requestFilter)
  }

  override def send(
      req: HttpMessage.Request,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = ???

  override private[http] def awaitF[A](f: Future[A]) = ???

  override def get[Resource: universe.TypeTag](
      resourcePath: String,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[Resource] = ???

  override def getResource[ResourceRequest: universe.TypeTag, Resource: universe.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[Resource] = ???

  override def list[OperationResponse: universe.TypeTag](
      resourcePath: String,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

  override def post[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[Resource] = ???

  override def postRaw[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = ???

  override def postOps[Resource: universe.TypeTag, OperationResponse: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

  override def put[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[Resource] = ???

  override def putRaw[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = ???

  override def putOps[Resource: universe.TypeTag, OperationResponse: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

  override def delete[OperationResponse: universe.TypeTag](
      resourcePath: String,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

  override def deleteRaw(
      resourcePath: String,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = ???

  override def deleteOps[Resource: universe.TypeTag, OperationResponse: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

  override def patch[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[Resource] = ???

  override def patchRaw[Resource: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[HttpMessage.Response] = ???

  override def patchOps[Resource: universe.TypeTag, OperationResponse: universe.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: HttpMessage.Request => HttpMessage.Request
  ): Future[OperationResponse] = ???

}
