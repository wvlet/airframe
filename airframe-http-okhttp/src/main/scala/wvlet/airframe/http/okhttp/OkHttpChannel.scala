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
package wvlet.airframe.http.okhttp

import okhttp3.{HttpUrl, MediaType, RequestBody}
import wvlet.airframe.http.client.{HttpChannelConfig, HttpChannel, HttpClientConfig}
import wvlet.airframe.http.*
import wvlet.airframe.rx.Rx
import wvlet.log.LogSupport

import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {
  private[this] val client = {
    var builder = new okhttp3.OkHttpClient.Builder()
      .readTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS)
      .connectTimeout(config.connectTimeout.toMillis, TimeUnit.MILLISECONDS)

    if (config.useHttp1) {
      // Enforce using HTTP/1.1
      import scala.jdk.CollectionConverters.*
      builder = builder.protocols(List(okhttp3.Protocol.HTTP_1_1).asJava)
    }
    builder.build()
  }

  override def close(): Unit = {
    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
  }

  private def prepareClient(channelConfig: HttpChannelConfig): okhttp3.OkHttpClient = {
    var newClient = this.client
    if (
      channelConfig.connectTimeout != config.connectTimeout ||
      channelConfig.readTimeout != config.readTimeout
    ) {
      newClient = newClient
        .newBuilder()
        .connectTimeout(channelConfig.connectTimeout.toMillis, TimeUnit.MILLISECONDS)
        .readTimeout(channelConfig.readTimeout.toMillis, TimeUnit.MILLISECONDS)
        .build()
    }
    newClient
  }

  override def send(req: HttpMessage.Request, channelConfig: HttpChannelConfig): HttpMessage.Response = {
    val request: okhttp3.Request = convertRequest(req)

    val newClient = prepareClient(channelConfig)
    val response  = newClient.newCall(request).execute()
    response.toHttpResponse
  }

  override def sendAsync(req: HttpMessage.Request, channelConfig: HttpChannelConfig): Rx[HttpMessage.Response] = {
    val request: okhttp3.Request = convertRequest(req)
    val newClient                = prepareClient(channelConfig)
    val v                        = Rx.variable[Option[HttpMessage.Response]](None)
    newClient
      .newCall(request).enqueue(new okhttp3.Callback {
        override def onFailure(call: okhttp3.Call, e: IOException): Unit = {
          v.setException(e)
        }
        override def onResponse(call: okhttp3.Call, response: okhttp3.Response): Unit = {
          v.set(Some(response.toHttpResponse))
          v.stop()
        }
      })
    v.filter(_.isDefined).map(_.get)
  }

  private def convertRequest(request: HttpMessage.Request): okhttp3.Request = {
    val query = request.query
    val queryParams: String = if (query.isEmpty) {
      null
    } else {
      query.entries
        .map { x =>
          s"${x.key}=${x.value}"
        }.mkString("&")
    }

    val url = HttpUrl
      .get(serverAddress.uri).newBuilder()
      .encodedPath(request.path)
      .encodedQuery(queryParams)
      .build()
    var resp = new okhttp3.Request.Builder().url(url)
    request.header.entries.foreach { x =>
      resp = resp.addHeader(x.key, x.value)
    }
    request.method match {
      case HttpMethod.GET | _ if request.message.isEmpty =>
        resp = resp.method(request.method, null)
      case _ =>
        resp = resp.method(
          request.method,
          RequestBody.create(request.contentBytes, request.contentType.map(MediaType.parse(_)).orNull)
        )
    }
    resp.build()
  }

}
