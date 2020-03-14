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

import org.scalajs.dom.{XMLHttpRequest, window}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.airframe.surface.Surface

import scala.concurrent.Future
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

/**
  * HttpClient utilities for Scala.js
  */
object JSHttpClient {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  def send[Response](
      method: String,
      path: String,
      data: InputData = null,
      responseCodec: MessageCodec[Response],
      headers: Map[String, String] = Map.empty
  ): Future[Response] = {
    val protocol = window.location.protocol
    val hostname = window.location.hostname
    val port     = window.location.port
    val fullUrl  = s"${protocol}//${hostname}${if (port.isEmpty) "" else ":" + port}${path}"

    val future =
      Ajax(
        method = method,
        url = fullUrl,
        data = data,
        headers = Map(
          // Use MessagePack RPC
          "Accept"       -> "application/x-msgpack",
          "Content-Type" -> "application/x-msgpack"
        ) ++ headers,
        timeout = 0,
        withCredentials = false,
        responseType = "arraybuffer"
      )
    future.map { xhr: XMLHttpRequest =>
      val arrayBuffer = xhr.response.asInstanceOf[ArrayBuffer]
      val dst         = new Array[Byte](arrayBuffer.byteLength)
      TypedArrayBuffer.wrap(arrayBuffer).get(dst, 0, arrayBuffer.byteLength)
      responseCodec.fromMsgPack(dst)
    }
  }

  def get[Resource](
      path: String,
      resourceSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[Resource] = {
    val resourceCodec = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    send("GET", path = path, responseCodec = resourceCodec, headers = headers)
  }

  def getOps[Resource, OperationResponse](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[OperationResponse] = {

    // Flatten the input Resource objects into URL query parameter as GET request usually will not receive the message body
    val resourceCodec            = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceRequestJsonValue = resourceCodec.toJSONObject(resource)
    val queryParams: Seq[String] =
      resourceRequestJsonValue.v.map {
        case (k, j @ JSONArray(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON array value
        case (k, j @ JSONObject(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON object value
        case (k, other) =>
          s"${urlEncode(k)}=${urlEncode(other.toString)}"
      }

    val pathWithParams = new StringBuilder()
    pathWithParams.append(path)
    pathWithParams.append("?")
    pathWithParams.append(queryParams.mkString("&"))

    val operationResponseCodec =
      MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
    send("GET", path = pathWithParams.result(), responseCodec = operationResponseCodec, headers = headers)
  }

  def post[Resource](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[Resource] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    send("POST", path = path, data = resourceMsgpack, responseCodec = resourceCodec, headers = headers)
  }

  def postOps[Resource, OperationResponse](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[OperationResponse] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    val operationResponseCodec =
      MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
    send("POST", path = path, data = resourceMsgpack, responseCodec = operationResponseCodec, headers = headers)
  }

  def put[Resource](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[Resource] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    send("PUT", path = path, data = resourceMsgpack, responseCodec = resourceCodec, headers = headers)
  }

  def putOps[Resource, OperationResponse](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[OperationResponse] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    val operationResponseCodec =
      MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
    send("PUT", path = path, data = resourceMsgpack, responseCodec = operationResponseCodec, headers = headers)
  }

  def delete[Resource](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[Resource] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    send("DELETE", path = path, data = resourceMsgpack, responseCodec = resourceCodec, headers = headers)
  }

  def deleteOps[Resource, OperationResponse](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[OperationResponse] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    val operationResponseCodec =
      MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
    send("DELETE", path = path, data = resourceMsgpack, responseCodec = operationResponseCodec, headers = headers)
  }

  def patch[Resource](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[Resource] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    send("PATCH", path = path, data = resourceMsgpack, responseCodec = resourceCodec, headers = headers)
  }

  def patchOps[Resource, OperationResponse](
      path: String,
      resource: Resource,
      resourceSurface: Surface,
      operationResponseSurface: Surface,
      headers: Map[String, String] = Map.empty
  ): Future[OperationResponse] = {
    val resourceCodec   = MessageCodec.ofSurface(resourceSurface).asInstanceOf[MessageCodec[Resource]]
    val resourceMsgpack = ByteBuffer.wrap(resourceCodec.toMsgPack(resource))
    val operationResponseCodec =
      MessageCodec.ofSurface(operationResponseSurface).asInstanceOf[MessageCodec[OperationResponse]]
    send("PATCH", path = path, data = resourceMsgpack, responseCodec = operationResponseCodec, headers = headers)
  }

  private def urlEncode(s: String): String = {
    scala.scalajs.js.URIUtils.encodeURI(s)
  }
}
