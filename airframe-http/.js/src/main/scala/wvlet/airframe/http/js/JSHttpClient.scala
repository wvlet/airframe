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
import java.nio.ByteBuffer

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.ext.AjaxException
import org.scalajs.dom.{XMLHttpRequest, window}
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpMessage.Request
import wvlet.airframe.http.{Http, HttpClient, HttpMessage, HttpMethod, HttpStatus}
import wvlet.airframe.surface.Surface

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object JSHttpClient {
  val defaultClient = new JSHttpClient()
}

/**
  * HttpClient utilities for Scala.js
  */
class JSHttpClient {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def send[OperationResponse](
      originalRequest: Request,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {

    val request = requestFilter(originalRequest)

    val protocol = window.location.protocol
    val hostname = window.location.hostname
    val port     = window.location.port
    val fullUri  = s"${protocol}//${hostname}${if (port.isEmpty) "" else ":" + port}${request.path}"

    val xhr     = new dom.XMLHttpRequest()
    val promise = Promise[dom.XMLHttpRequest]()

    // TODO Use our custom retry logic like FinagleClient
    xhr.onreadystatechange = { (e: dom.Event) =>
      if (xhr.readyState == 4) {
        if ((xhr.status >= 200 && xhr.status < 300) || xhr.status == 304)
          promise.success(xhr)
        else
          promise.failure(AjaxException(xhr))
      }
    }
    xhr.open(request.method.toString, fullUri)
    xhr.responseType = "arraybuffer"
    xhr.timeout = 0
    xhr.withCredentials = false
    request.header.entries.foreach { x => xhr.setRequestHeader(x.key, x.value) }
    val data: Array[Byte] = request.contentBytes
    if (data.isEmpty) {
      xhr.send()
    } else {
      val input: InputData = ByteBuffer.wrap(data)
      xhr.send(input)
    }

    val future = promise.future
    future.map { xhr: XMLHttpRequest =>
      val arrayBuffer = xhr.response.asInstanceOf[ArrayBuffer]
      val dst         = new Array[Byte](arrayBuffer.byteLength)
      TypedArrayBuffer.wrap(arrayBuffer).get(dst, 0, arrayBuffer.byteLength)

      operationResponseSurface.rawType match {
        case c if c == classOf[HttpMessage.Response] =>
          Http
            .response(HttpStatus.ofCode(xhr.status))
            .withContent(dst).asInstanceOf[OperationResponse]
        case _ =>
          val responseCodec =
            MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
          responseCodec.fromMsgPack(dst)
      }
    }
  }

  def sendResource[Resource](
      request: Request,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request
  ): Future[Resource] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = resourceCodec.toMsgPack(resource)
    send(request.withContent(resourceMsgpack), resourceSurface, requestFilter)
  }

  def sendResourceOps[Resource, OperationResponse](
      request: Request,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request
  ): Future[OperationResponse] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = resourceCodec.toMsgPack(resource)
    send(request.withContent(resourceMsgpack), operationResponseSurface, requestFilter = requestFilter)
  }

  def get[Resource](
      resourcePath: String,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    val resourceCodec = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    send(Http.request(HttpMethod.GET, resourcePath), resourceSurface, requestFilter = requestFilter)
  }

  def getOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    val path = HttpClient.buildResourceUri(resourcePath, resource, resourceSurface)
    send(
      Http.request(HttpMethod.GET, path),
      operationResponseSurface,
      requestFilter = requestFilter
    )
  }

  def post[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.POST, resourcePath), resource, resourceSurface, requestFilter)
  }

  def postOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.POST, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def put[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.PUT, resourcePath), resource, resourceSurface, requestFilter)
  }

  def putOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.PUT, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def delete[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.DELETE, resourcePath), resource, resourceSurface, requestFilter)
  }

  def deleteOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.DELETE, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }

  def patch[Resource](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[Resource] = {
    sendResource(Http.request(HttpMethod.PATCH, resourcePath), resource, resourceSurface, requestFilter)
  }

  def patchOps[Resource, OperationResponse](
      resourcePath: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    sendResourceOps[Resource, OperationResponse](
      Http.request(HttpMethod.PATCH, resourcePath),
      resource,
      resourceSurface,
      operationResponseSurface,
      requestFilter
    )
  }
}
