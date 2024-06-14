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

import org.scalajs.dom.{Headers, RequestRedirect}
import wvlet.airframe.http.HttpMessage.{ByteArrayMessage, EmptyMessage, Request, Response, StringMessage}
import wvlet.airframe.http.{Compat, HttpMessage, HttpMethod, HttpMultiMap, HttpStatus, ServerAddress}
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import scala.concurrent.{ExecutionContext, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.util.{Failure, Success, Try}

/**
  * An http channel implementation based on Fetch API
  * https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch
  * @param serverAddress
  * @param config
  */
class JSFetchChannel(val destination: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {
  private[client] implicit val executionContext: ExecutionContext = Compat.defaultExecutionContext

  override def close(): Unit = {
    // nothing to do
  }

  override def send(
      req: HttpMessage.Request,
      channelConfig: HttpChannelConfig
  ): HttpMessage.Response = {
    // Blocking call cannot be supported in JS
    ???
  }

  override def sendAsync(
      request: HttpMessage.Request,
      channelConfig: HttpChannelConfig
  ): Rx[HttpMessage.Response] = {
    val path = if request.uri.startsWith("/") then request.uri else s"/${request.uri}"
    val uri  = s"${request.dest.getOrElse(destination).uri}${path}"

    val req = new org.scalajs.dom.RequestInit {
      method = request.method match {
        case HttpMethod.GET     => org.scalajs.dom.HttpMethod.GET
        case HttpMethod.POST    => org.scalajs.dom.HttpMethod.POST
        case HttpMethod.PUT     => org.scalajs.dom.HttpMethod.PUT
        case HttpMethod.HEAD    => org.scalajs.dom.HttpMethod.HEAD
        case HttpMethod.DELETE  => org.scalajs.dom.HttpMethod.DELETE
        case HttpMethod.OPTIONS => org.scalajs.dom.HttpMethod.OPTIONS
        case HttpMethod.PATCH   => org.scalajs.dom.HttpMethod.PATCH
        case _                  => throw new IllegalArgumentException(s"Unsupported HTTP method: ${request.method}")
      }
      headers = new Headers(request.header.entries.map { e =>
        Array[String](e.key, e.value).toJSArray
      }.toJSArray)
      // Follow redirect by default
      redirect = RequestRedirect.follow

      // TODO set timeout with signal parameter
    }

    // Import typedarray package for converting Array[Byte] to js.typedarray.ArrayBuffer
    import js.typedarray.*
    req.body = request.message match {
      case EmptyMessage              => js.undefined
      case StringMessage(content)    => content
      case ByteArrayMessage(content) => content.toTypedArray
      case other                     => other.toContentBytes.toTypedArray
    }

    val future = org.scalajs.dom
      .fetch(uri, req).toFuture
      .flatMap { resp =>
        var r      = wvlet.airframe.http.Http.response(HttpStatus.ofCode(resp.status))
        val header = HttpMultiMap.newBuilder
        resp.headers.foreach { h =>
          header.add(h(0), h(1))
        }
        r = r.withHeader(header.result())
        if r.isContentTypeJson then {
          resp.text().toFuture.map { body =>
            r.withContent(body)
          }
        } else {
          resp.arrayBuffer().toFuture.map { body =>
            r.withContent(new Int8Array(body).toArray)
          }
        }
      }

    Rx.future(future)
  }

}
