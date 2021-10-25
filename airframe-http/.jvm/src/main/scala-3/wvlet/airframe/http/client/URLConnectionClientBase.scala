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
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.{Http, HttpSyncClient}
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.airframe.surface.Surface

/**
  */
trait URLConnectionClientBase extends HttpSyncClient[Request, Response] { self: URLConnectionClient =>

  inline private def convert[A](response: Response): A = {
    if (classOf[Response].isAssignableFrom(Surface.of[A].rawType)) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      // Need a conversion
      val codec   = MessageCodec.of[A]
      val msgpack = responseCodec.toMsgPack(response)
      codec.unpack(msgpack)
    }
  }

  override def get[Resource](
          resourcePath: String,
          requestFilter: Request => Request
  ): Resource = {
    convert[Resource](send(Http.request(resourcePath), requestFilter))
  }

  inline override def getOps[
          Resource,
          OperationResponse
  ](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): OperationResponse = {


    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  }

  inline override def getResource[
          ResourceRequest,
          Resource
  ](
          resourcePath: String,
          resourceRequest: ResourceRequest,
          requestFilter: Request => Request
  ): Resource = {
    // Read resource as JSON
    val resourceRequestJsonValue =
      self.config.codecFactory.of[ResourceRequest].toJSONObject(resourceRequest)
    val req = buildGETRequest(resourcePath, resourceRequestJsonValue)
    convertAs[Resource](send(req, requestFilter), Surface.of[Resource])
  }

  override def list[OperationResponse](
          resourcePath: String,
          requestFilter: Request => Request
  ): OperationResponse = {
    convert[OperationResponse](send(Http.request(resourcePath), requestFilter))
  }

  private def toJson[Resource](resource: Resource): String = {
    val resourceCodec = config.codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def post[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): Resource = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): Response = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def postOps[
          Resource,
          OperationResponse
  ](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): Resource = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): Response =
    putOps[Resource, Response](resourcePath, resource, requestFilter)

  override def putOps[
          Resource,
          OperationResponse
  ](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse](
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
          Resource,
          OperationResponse
  ](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): OperationResponse = {

    val r = Http.DELETE(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource](
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

  override def patchRaw[Resource](
          resourcePath: String,
          resource: Resource,
          requestFilter: Request => Request
  ): Response =
    patchOps[Resource, Response](resourcePath, resource, requestFilter)
  override def patchOps[
          Resource,
          OperationResponse
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
