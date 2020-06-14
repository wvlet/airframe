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

import java.io.{IOException, InputStream, OutputStream}
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.util.zip.{GZIPInputStream, InflaterInputStream}

import wvlet.airframe.codec.{MessageCodec, MessageCodecFactory}
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.control.{Control, IO}
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http._
import wvlet.airframe.http.router.HttpResponseCodec
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.reflect.runtime.universe
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
  * Http sync client implementation using URLConnection
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

      def timeoutMillis(d: Duration): Int = {
        if (d.isFinite) {
          d.toMillis.toInt
        } else {
          0
        }
      }
      conn0.setReadTimeout(timeoutMillis(config.readTimeout))
      conn0.setConnectTimeout(timeoutMillis(config.connectTimeout))
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

      try {
        Control.withResource(conn.getInputStream()) { in: InputStream =>
          readResponse(conn, in)
        }
      } catch {
        case e: IOException if conn.getResponseCode != -1 =>
          // When the request fails, but the server still returns meaningful responses
          // (e.g., 404 NotFound throws FileNotFoundException)
          Control.withResource(conn.getErrorStream()) { err: InputStream =>
            readResponse(conn, err)
          }
      }
    }
  }

  private def readResponse(conn: HttpURLConnection, in: InputStream): Response = {
    val status = HttpStatus.ofCode(conn.getResponseCode)

    val h = HttpMultiMap.newBuilder
    for ((k, vv) <- conn.getHeaderFields().asScala if k != null; v <- vv.asScala) {
      h += k -> v
    }
    val response = Http.response(status).withHeader(h.result())
    val is = response.contentEncoding.map(_.toLowerCase) match {
      case _ if in == null => in
      case Some("gzip")    => new GZIPInputStream(in)
      case Some("deflate") => new InflaterInputStream(in)
      case other           =>
        // For unsupported encoding, read content as bytes
        in
    }

    // TODO: For supporting streaming read, we need to extend HttpMessage class
    val responseContentBytes = IO.readFully(is) { bytes =>
      bytes
    }
    response.withContent(responseContentBytes)
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
  ): OperationResponse = {
    getResource[Resource, OperationResponse](resourcePath, resource, requestFilter)
  }

  override def getResource[
      ResourceRequest: universe.TypeTag,
      Resource: universe.TypeTag
  ](
      resourcePath: String,
      resourceRequest: ResourceRequest,
      requestFilter: Request => Request
  ): Resource = {
    // Read resource as JSON
    val resourceRequestJsonValue = config.codecFactory.of[ResourceRequest].toJSONObject(resourceRequest)
    val queryParams: Seq[String] =
      resourceRequestJsonValue.v.map {
        case (k, j @ JSONArray(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON array value
        case (k, j @ JSONObject(_)) =>
          s"${urlEncode(k)}=${urlEncode(j.toJSON)}" // Flatten the JSON object value
        case (k, other) =>
          s"${urlEncode(k)}=${urlEncode(other.toString)}"
      }

    val r0 = Http.GET(resourcePath)
    val r = (r0.query, queryParams) match {
      case (query, queryParams) if query.isEmpty && queryParams.nonEmpty =>
        r0.withUri(s"${r0.uri}?${queryParams.mkString("&")}")
      case (query, queryParams) if query.nonEmpty && queryParams.nonEmpty =>
        r0.withUri(s"${r0.uri}&${queryParams.mkString("&")}")
      case _ =>
        r0
    }
    convert[Resource](send(r, requestFilter))
  }

  override def list[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = {
    convert[OperationResponse](send(Http.request(resourcePath), requestFilter))
  }

  private def toJson[Resource: TypeTag](resource: Resource): String = {
    val resourceCodec = config.codecFactory.of[Resource]
    // TODO: Support non-json content body
    val json = resourceCodec.toJson(resource)
    json
  }

  override def post[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def postRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = {
    postOps[Resource, Response](resourcePath, resource, requestFilter)
  }

  override def postOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.POST(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def put[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def putRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = putOps[Resource, Response](resourcePath, resource, requestFilter)

  override def putOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.PUT(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def delete[OperationResponse: TypeTag](
      resourcePath: String,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.DELETE(resourcePath)
    convert[OperationResponse](send(r, requestFilter))
  }

  override def deleteRaw(
      resourcePath: String,
      requestFilter: Request => Request
  ): Response = delete[Response](resourcePath, requestFilter)

  override def deleteOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {

    val r = Http.DELETE(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def patch[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Resource = {
    val r = Http.PATCH(resourcePath).withJson(toJson(resource))
    convert[Resource](send(r, requestFilter))
  }

  override def patchRaw[Resource: TypeTag](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): Response = patchOps[Resource, Response](resourcePath, resource, requestFilter)
  override def patchOps[
      Resource: TypeTag,
      OperationResponse: TypeTag
  ](
      resourcePath: String,
      resource: Resource,
      requestFilter: Request => Request
  ): OperationResponse = {
    val r = Http.PATCH(resourcePath).withJson(toJson(resource))
    convert[OperationResponse](send(r, requestFilter))
  }

  override def close(): Unit = {}
}
