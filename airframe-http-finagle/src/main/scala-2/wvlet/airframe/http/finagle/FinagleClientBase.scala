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
package wvlet.airframe.http.finagle

import wvlet.airframe.http._

import scala.reflect.runtime.{universe => ru}
import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.surface.Surface

import scala.util.control.NonFatal

trait FinagleClientBase extends HttpClient[Future, http.Request, http.Response] { self: FinagleClient =>

  protected val codecFactory: MessageCodecFactory
  private val responseCodec = new HttpResponseBodyCodec[Response]

  private def convert[A: ru.TypeTag](response: Future[Response]): Future[A] = {
    if (implicitly[ru.TypeTag[A]] == ru.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[Future[A]]
    } else {
      // Need a conversion
      val codec = codecFactory.of[A]
      response
        .map { r =>
          val msgpack = responseCodec.toMsgPack(r)
          try {
            codec.unpack(msgpack)
          } catch {
            case NonFatal(e) =>
              val msg =
                s"Failed to parse the response body ${r}: ${r.contentString}"
              warn(msg)
              throw new HttpClientException(
                r,
                HttpStatus.ofCode(r.statusCode),
                msg,
                e
              )
          }
        }
    }
  }

  private def toJson[Resource: ru.TypeTag](resource: Resource): String = {
    val resourceCodec = codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def get[Resource: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    convert[Resource](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  override def getResource[ResourceRequest: ru.TypeTag, Resource: ru.TypeTag](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {

    val resourceSurface = Surface.of[ResourceRequest]
    val path            = HttpClient.buildResourceUri(resourcePath, resourceRequest, resourceSurface)
    convert[Resource](send(newRequest(HttpMethod.GET, path), requestFilter))
  }

  override def list[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  override def post[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    postOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  override def postOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    putOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  override def putOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: ru.TypeTag](
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.DELETE, resourcePath), requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    delete[http.Response](resourcePath, requestFilter)
  }

  override def deleteOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.DELETE, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PATCH, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def patchRaw[Resource: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    patchOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  override def patchOps[Resource: ru.TypeTag, OperationResponse: ru.TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PATCH, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }
}
