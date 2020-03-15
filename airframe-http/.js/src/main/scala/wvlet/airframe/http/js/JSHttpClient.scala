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
import org.scalajs.dom.window
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{ResultClass, Retry}
import wvlet.airframe.http.HttpClient.defaultBeforeRetryAction
import wvlet.airframe.http.HttpMessage._
import wvlet.airframe.http._
import wvlet.airframe.surface.Surface

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.{Failure, Success}

object JSHttpClient {
  val defaultClient = {
    val protocol = window.location.protocol
    val hostname = window.location.hostname
    val port     = window.location.port.toInt
    val address  = ServerAddress(hostname, port, protocol)
    JSHttpClient(address)
  }

  def defaultHttpClientRetrier: RetryContext = {
    Retry
      .withBackOff(maxRetry = 3)
      .withResultClassifier(HttpClientException.classifyHttpResponse[Response])
      .withErrorClassifier { e: Throwable => Retry.nonRetryableFailure(e) }
      .beforeRetry(defaultBeforeRetryAction[Request])
  }
}

case class JSHttpClientConfig(
    requestFilter: Request => Request = identity,
    retryContext: RetryContext = JSHttpClient.defaultHttpClientRetrier
) {
  def withRequestFilter(newRequestFilter: Request => Request): JSHttpClientConfig =
    this.copy(requestFilter = newRequestFilter)
  def noRetry: JSHttpClientConfig = this.copy(retryContext = retryContext.noRetry)
}

/**
  * HttpClient utilities for Scala.js
  */
case class JSHttpClient(address: ServerAddress, config: JSHttpClientConfig = JSHttpClientConfig()) {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def withConfig(newConfig: JSHttpClientConfig): JSHttpClient = {
    this.copy(config = newConfig)
  }

  /**
    * Send the request. If necessary, retry the request
    * @param retryContext
    * @param request
    * @return
    */
  private def dispatch(retryContext: RetryContext, request: Request): Future[Response] = {
    val xhr = new dom.XMLHttpRequest()
    xhr.open(request.method.toString, s"${address.uri}${request.path}")
    xhr.responseType = "arraybuffer"
    xhr.timeout = 0
    xhr.withCredentials = false
    request.header.entries.foreach { x => xhr.setRequestHeader(x.key, x.value) }

    val promise           = Promise[Response]()
    val data: Array[Byte] = request.contentBytes
    if (data.isEmpty) {
      xhr.send()
    } else {
      val input: InputData = ByteBuffer.wrap(data)
      xhr.send(input)
    }

    xhr.onreadystatechange = { (e: dom.Event) =>
      if (xhr.readyState == 4) { // Ajax request is DONE
        val resp = Http.response(HttpStatus.ofCode(xhr.status))
        retryContext.resultClassifier(resp) match {
          case ResultClass.Succeeded =>
            //if ((xhr.status >= 200 && xhr.status < 300) || xhr.status == 304)
            val arrayBuffer = xhr.response.asInstanceOf[ArrayBuffer]
            val dst         = new Array[Byte](arrayBuffer.byteLength)
            TypedArrayBuffer.wrap(arrayBuffer).get(dst, 0, arrayBuffer.byteLength)
            promise.success(resp.withContent(dst))
          case ResultClass.Failed(isRetryable, cause, extraWait) =>
            if (!retryContext.canContinue) {
              promise.failure(HttpClientMaxRetryException(resp, retryContext, cause))
            } else if (!isRetryable) {
              promise.failure(cause)
            } else {
              dispatch(retryContext.nextRetry(cause), request).onComplete {
                case Success(resp) => promise.success(resp)
                case Failure(e)    => promise.failure(e)
              }
            }
        }
      }
    }

    val future = promise.future
    future
  }

  def send[OperationResponse](
      originalRequest: Request,
      operationResponseSurface: Surface,
      requestFilter: Request => Request = identity
  ): Future[OperationResponse] = {
    // Apply the default request filter first, and then apply the custom filter
    val request = requestFilter(config.requestFilter(originalRequest))
    dispatch(config.retryContext, request).map { resp =>
      operationResponseSurface.rawType match {
        case c if c == classOf[HttpMessage.Response] =>
          resp.asInstanceOf[OperationResponse]
        case _ =>
          val responseCodec =
            MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
          responseCodec.fromMsgPack(resp.contentBytes)
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
