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
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax.InputData
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http._
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import java.nio.ByteBuffer
import java.io.IOException
import scala.concurrent.{ExecutionContext, Promise, TimeoutException}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.util.Try

class JSHttpClientChannel(serverAddress: ServerAddress, private[client] val config: HttpClientConfig)
    extends HttpChannel
    with LogSupport {

  private[client] implicit val executionContext: ExecutionContext = config.newExecutionContext

  override def close(): Unit = {
    // nothing to do
  }

  override def send(request: HttpMessage.Request, channelConfig: HttpChannelConfig): HttpMessage.Response = ???

  override def sendAsync(
      request: HttpMessage.Request,
      channelConfig: HttpChannelConfig
  ): Rx[HttpMessage.Response] = {

    val xhr = new dom.XMLHttpRequest()

    val path = if (request.uri.startsWith("/")) request.uri else s"/${request.uri}"
    val uri  = s"${serverAddress.uri}${path}"

    trace(s"Sending request: ${request}: ${uri}")
    xhr.open(request.method, uri)
    xhr.responseType = "arraybuffer"
    xhr.timeout = 0
    xhr.withCredentials = false
    // Setting the header must be called after xhr.open(...)
    request.header.entries.foreach { x => xhr.setRequestHeader(x.key, x.value) }

    val promise = Promise[Response]()

    xhr.onerror = { (e: dom.Event) =>
      if (!promise.isCompleted) {
        promise.failure(new IOException(s"Request failed for unknown reason: ${request}"))
      }
    }
    xhr.ontimeout = { (e: dom.Event) =>
      if (!promise.isCompleted) {
        promise.failure(new TimeoutException(s"Request timed out: ${request}"))
      }
    }

    val data: Array[Byte] = request.contentBytes
    if (data.isEmpty) {
      xhr.send()
    } else {
      val input: InputData = ByteBuffer.wrap(data)
      xhr.send(input)
    }

    xhr.onreadystatechange = { (e: dom.Event) =>
      if (xhr.readyState == 4) { // Ajax request is DONE
        // Prepare HttpMessage.Response
        var resp = Http.response(HttpStatus.ofCode(xhr.status))
        // This part needs to be exception-free
        Try {
          // Set response headers
          val header = HttpMultiMap.newBuilder
          xhr
            .getAllResponseHeaders()
            .split("\n")
            .foreach { line =>
              line.split(":") match {
                case Array(k, v) => header += k.trim -> v.trim
                case _           =>
              }
            }
          resp = resp.withHeader(header.result())
        }

        // This part also needs to be exception-free
        Try {
          // Read response content
          Option(xhr.response).foreach { r =>
            val arrayBuffer = r.asInstanceOf[ArrayBuffer]
            val dst         = new Array[Byte](arrayBuffer.byteLength)
            TypedArrayBuffer.wrap(arrayBuffer).get(dst, 0, arrayBuffer.byteLength)
            resp = resp.withContent(dst)
          }
        }
        trace(s"Get response: ${resp}")
        if (!promise.isCompleted) {
          promise.success(resp)
        }
      }
    }
    Rx.future(promise.future)
  }
}
