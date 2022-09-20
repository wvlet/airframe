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

import com.twitter.finagle.http
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future
import wvlet.airframe.codec.MessageCodecFactory
import wvlet.airframe.surface.Surface
import scala.reflect.ClassTag

import scala.util.control.NonFatal

trait FinagleClientBase extends HttpClient[Future, http.Request, http.Response] { self: FinagleClient =>
  private val responseCodec = new HttpResponseBodyCodec[Response]

  inline private def convert[A : ClassTag](response: Future[Response]): Future[A] = {
    val cls = implicitly[ClassTag[A]].runtimeClass
    if (cls.isAssignableFrom(classOf[Response])) {
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

  inline private def toJson[Resource : ClassTag](resource: Resource): String = {
    val resourceCodec = codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  inline override def get[Resource : ClassTag](
    resourcePath: String,
    requestFilter: Request => Request = identity
  ): Future[Resource] = {
    convert[Resource](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  inline override def getResource[ResourceRequest : ClassTag, Resource : ClassTag](
    resourcePath: String,
    resourceRequest: ResourceRequest,
    requestFilter: Request => Request = identity
  ): Future[Resource] = {

    val resourceSurface = Surface.of[ResourceRequest]
    val path            = HttpClient.buildResourceUri(resourcePath, resourceRequest, resourceSurface)
    convert[Resource](send(newRequest(HttpMethod.GET, path), requestFilter))
  }

  inline override def list[OperationResponse : ClassTag](
    resourcePath: String,
    requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    convert[OperationResponse](send(newRequest(HttpMethod.GET, resourcePath), requestFilter))
  }

  inline override def post[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  inline override def postRaw[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    postOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  inline override def postOps[Resource : ClassTag, OperationResponse : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.POST, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  inline override def put[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  inline override def putRaw[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    putOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  inline override def putOps[Resource : ClassTag, OperationResponse : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.PUT, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  inline override def delete[OperationResponse : ClassTag](
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

  inline override def deleteOps[Resource : ClassTag, OperationResponse : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val r = newRequest(HttpMethod.DELETE, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  inline override def patch[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val r = newRequest(HttpMethod.PATCH, resourcePath)
    r.setContentTypeJson()
    r.setContentString(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  inline override def patchRaw[Resource : ClassTag](
    resourcePath: String,
    resource: Resource,
    requestFilter: Request => Request = identity
  ): Future[http.Response] = {
    patchOps[Resource, http.Response](resourcePath, resource, requestFilter)
  }

  inline override def patchOps[Resource : ClassTag, OperationResponse : ClassTag](
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
