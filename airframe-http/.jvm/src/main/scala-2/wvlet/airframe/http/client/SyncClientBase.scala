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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Http, HttpResponseBodyCodec}
import wvlet.airframe.surface.Surface

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

/**
  */
trait SyncClientBase {
  self: SyncClient =>

  protected def convert[A: TypeTag](response: Response): A = {
    if (
      implicitly[TypeTag[A]] == scala.reflect.runtime.universe
        .typeTag[Response]
    ) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      val standardResponseCodec = new HttpResponseBodyCodec[Response]
      // Need a conversion
      val codec   = MessageCodec.of[A]
      val msgpack = standardResponseCodec.toMsgPack(response)
      codec.unpack(msgpack)
    }
  }

  protected def getInternal[Resource](
      resourcePath: String,
      requestFilter: Request => Request,
      resourceSurface: Surface
  ): Resource = {
    HttpClients.convertAs[Resource](send(Http.request(resourcePath), requestFilter), resourceSurface)
  }

  def get[Resource: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Resource = {
    getInternal[Resource](resourcePath, requestFilter, Surface.of[Resource])
  }

  def getOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  }

  def getResource[
      ResourceRequest: universe.TypeTag,
      Resource: universe.TypeTag
  ](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request => Request = identity
  ): Resource = {
    // Read resource as JSON
    val resourceRequestJsonValue = {
      self.config.codecFactory.of[ResourceRequest].toJSONObject(resourceRequest)
    }
    val req = HttpClients.buildGETRequest(resourcePath, resourceRequestJsonValue)
    convert[Resource](send(req, requestFilter))
  }

  def list[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    convert[OperationResponse](send(Http.request(resourcePath), requestFilter))
  }

  private def toJson[Resource: TypeTag](resource: Resource): String = {
    val resourceCodec = config.codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  def post[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Resource = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  def postRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Response = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  def postOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  def put[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Resource = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  def putRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Response =
    putOps[Resource, Response](resourcePath, resource, requestFilter)

  def putOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  def delete[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    val r = Http.DELETE(resourcePath)
    convert[OperationResponse](send(r, requestFilter))
  }

  def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Response = delete[Response](resourcePath, requestFilter)

  def deleteOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): OperationResponse = {

    val r = Http.DELETE(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  def patch[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Resource = {
    val r = Http
      .POST(resourcePath)
      .withHeader("X-HTTP-Method-Override", "PATCH")
      .withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  def patchRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Response =
    patchOps[Resource, Response](resourcePath, resource, requestFilter)

  def patchOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): OperationResponse = {
    // Workaround: URLConnection doesn't support PATCH
    // https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
    val r = Http
      .PATCH(resourcePath)
      .withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

}
