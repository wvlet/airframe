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
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Http, HttpSyncClient}
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag

/**
  */
trait URLConnectionClientBase extends HttpSyncClient[Request, Response] { self: URLConnectionClient =>
  protected def convert[A: TypeTag](response: Response): A = {
    if (
      implicitly[TypeTag[A]] == scala.reflect.runtime.universe
        .typeTag[Response]
    ) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      // Need a conversion
      val codec   = MessageCodec.of[A]
      val msgpack = self.responseCodec.toMsgPack(response)
      codec.unpack(msgpack)
    }
  }

  override def get[Resource: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): Resource = {
    convert[Resource](send(Http.request(resourcePath), requestFilter))
  }

  override def getOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  }

  override def getResource[
      ResourceRequest: universe.TypeTag,
      Resource: universe.TypeTag
  ](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request => Request
  ): Resource = {
    // Read resource as JSON
    val resourceRequestJsonValue = {
      self.config.codecFactory.of[ResourceRequest].toJSONObject(resourceRequest)
    }
    val req = buildGETRequest(resourcePath, resourceRequestJsonValue)
    convert[Resource](send(req, requestFilter))
  }

  override def list[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = {
    convert[OperationResponse](send(Http.request(resourcePath), requestFilter))
  }

  private def toJson[Resource: TypeTag](resource: Resource): String = {
    val resourceCodec = config.codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def post[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def postOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response =
    putOps[Resource, Response](resourcePath, resource, requestFilter)

  override def putOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.DELETE(resourcePath)
    convert[OperationResponse](send(r, requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request
  ): Response = delete[Response](resourcePath, requestFilter)

  override def deleteOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {

    val r = Http.DELETE(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http
      .POST(resourcePath)
      .withHeader("X-HTTP-Method-Override", "PATCH")
      .withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def patchRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response =
    patchOps[Resource, Response](resourcePath, resource, requestFilter)
  override def patchOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    // Workaround: URLConnection doesn't support PATCH
    //https://stackoverflow.com/questions/25163131/httpurlconnection-invalid-http-method-patch
    val r = Http
      .POST(resourcePath)
      .withHeader("X-HTTP-Method-Override", "PATCH")
      .withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

}
