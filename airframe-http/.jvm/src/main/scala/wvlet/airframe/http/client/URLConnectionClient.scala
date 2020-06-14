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

import java.io.{InputStream, OutputStream}
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{Control, IO}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.http.router.HttpResponseCodec

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe.TypeTag

case class URLConnectionClientConfig(
    requestFilter: Request => Request = identity,
    connectionFilter: HttpURLConnection => HttpURLConnection = identity,
    readTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    connectTimeout: Duration = Duration(90, TimeUnit.SECONDS),
    retryContext: RetryContext = HttpClient.defaultHttpClientRetry[Request, Response],
    codecFactory: MessageCodecFactory = MessageCodecFactory.defaultFactoryForJSON,
    followRedirect: Boolean = true
)

/**
  *
 */
class URLConnectionClient(address: ServerAddress, config: URLConnectionClientConfig)
    extends HttpSyncClient[Request, Response] {

  override def send(
      req: Request,
      requestFilter: Request => Request
  ): Response = {

    // Apply the default filter first and then the given custom filter
    val request = requestFilter(config.requestFilter(req))

    val url = s"${address.uri}${request.uri}"

    config.retryContext.run {
      val conn0: HttpURLConnection = new java.net.URL(url).openConnection().asInstanceOf[HttpURLConnection]
      conn0.setRequestMethod(request.method)
      for (e <- request.header.entries) {
        conn0.setRequestProperty(e.key, e.value)
      }
      conn0.setDoInput(true)
      conn0.setReadTimeout(config.readTimeout.toMillis.toInt)
      conn0.setConnectTimeout(config.connectTimeout.toMillis.toInt)
      conn0.setInstanceFollowRedirects(config.followRedirect)

      val conn    = config.connectionFilter(conn0)
      val content = req.contentBytes
      if (content.nonEmpty) {
        conn.setDoOutput(true)
        Control.withResource(conn.getOutputStream()) { out: OutputStream =>
          out.write(content)
          out.flush()
        }
      }

      Control.withResource(conn.getInputStream()) { in: InputStream =>
        val status = HttpStatus.ofCode(conn.getResponseCode)

        val header = HttpMultiMap.newBuilder
        for ((k, vv) <- conn.getHeaderFields().asScala if k != null; v <- vv.asScala) {
          header += k -> v
        }
        // TODO: Support stream?
        val responseContentBytes = IO.readFully(in) { bytes =>
          bytes
        }

        val response = Http.response(status).withHeader(header.result()).withContent(responseContentBytes)
        response
      }
    }
  }

  override def sendSafe(
      req: Request,
      requestFilter: Request => Request
  ): Response = {
    try {
      send(req, requestFilter)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }

  private val responseCodec = new HttpResponseCodec[Response]

  private def convert[A: TypeTag](response: Response): A = {
    if (implicitly[TypeTag[A]] == scala.reflect.runtime.universe.typeTag[Response]) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      // Need a conversion
      val codec   = MessageCodec.of[A]
      val msgpack = responseCodec.toMsgPack(response)
      codec.unpack(msgpack)
    }
  }

  override def get[Resource: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): Resource = {
    convert[Resource](send(Http.request(resourcePath), requestFilter))
  }

  override def getOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = ???

  override def list[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = {
    convert[OperationResponse](send(Http.request(resourcePath), requestFilter))
  }

  override def post[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = ???

  override def postRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = ???
  override def postOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = ???
  override def put[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = ???
  override def putRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = ???
  override def putOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = ???
  override def delete[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = ???
  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request
  ): Response = ???
  override def deleteOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = ???
  override def patch[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = ???
  override def patchRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = ???
  override def patchOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse       = ???
  override def close(): Unit = ???
}
