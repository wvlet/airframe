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

import okhttp3.{HttpUrl, Request}
import wvlet.airframe.http.{ChannelConfig, HttpClientConfig, HttpMessage, ServerAddress}
import wvlet.airframe.http.client.HttpChannel
import wvlet.log.LogSupport

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

class OkHttpChannel(serverAddress: ServerAddress, config: HttpClientConfig) extends HttpChannel with LogSupport {
  private[this] val client = {
    new okhttp3.OkHttpClient.Builder()
      .readTimeout(config.readTimeout.toMillis, TimeUnit.MILLISECONDS)
      .connectTimeout(config.connectTimeout.toMillis, TimeUnit.MILLISECONDS)
      .addInterceptor(new OkHttpRetryInterceptor(config.retryContext))
      .build()
  }

  override def close(): Unit = {
    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
  }

  override def send(req: HttpMessage.Request, channelConfig: ChannelConfig): HttpMessage.Response = {
    info(req)
    val request: okhttp3.Request = buildRequest(req)

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

    val response = newClient.newCall(request).execute()
    response.toHttpResponse
  }

  override def sendAsync(req: HttpMessage.Request, channelConfig: ChannelConfig): Future[HttpMessage.Response] = ???

  private def buildRequest(request: HttpMessage.Request): okhttp3.Request = {
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
    val resp = new okhttp3.Request.Builder().url(url)
    request.header.entries.foreach { x =>
      resp.addHeader(x.key, x.value)
    }
    resp.build()
  }

}
