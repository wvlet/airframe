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
package wvlet.airframe.http
import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.control.Retry
import wvlet.airframe.control.Retry.RetryContext
import wvlet.airframe.http.client.HttpClients.urlEncode
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.http.internal.HttpResponseBodyCodec
import wvlet.airframe.http.client.HttpClients.urlEncode
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.airframe.surface.Surface
import wvlet.log.LogSupport

import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.Try

/**
  * Asynchronous HTTP Client interface
  *
  * @tparam F
  *   An abstraction for Future type (e.g., Resolves the differences between Twitter Future, Scala Future, etc.)
  * @tparam Req
  * @tparam Resp
  */
@deprecated("Use wvlet.airframe.http.client.Sync/AsyncClient", "24.5.0")
trait HttpClient[F[_], Req, Resp] extends HttpClientBase[F, Req, Resp] with AutoCloseable {

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses (after reaching
    * the max retry limit)
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is happend
    */
  def send(req: Req, requestFilter: Req => Req = identity): F[Resp]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  def sendSafe(req: Req, requestFilter: Req => Req = identity): F[Resp]

  /**
    * Await the response and extract the return value
    *
    * @param f
    * @tparam A
    * @return
    */
  private[http] def awaitF[A](f: F[A]): A

  def syncClient: HttpSyncClient[Req, Resp] = new HttpSyncClientAdapter(this)
}

/**
  * A synchronous HTTP Client interface
  *
  * @tparam Req
  * @tparam Resp
  */
trait HttpSyncClient[Req, Resp] extends HttpSyncClientBase[Req, Resp] with AutoCloseable {

  def send(req: Req, requestFilter: Req => Req = identity): Resp

  def sendSafe(req: Req, requestFilter: Req => Req = identity): Resp

  private val standardResponseCodec = new HttpResponseBodyCodec[Response]

  protected def convertAs[A](response: Response, surface: Surface): A = {
    if (classOf[Response].isAssignableFrom(surface.rawType)) {
      // Can return the response as is
      response.asInstanceOf[A]
    } else {
      // Need a conversion
      val codec   = MessageCodec.ofSurface(surface)
      val msgpack = standardResponseCodec.toMsgPack(response)
      val obj     = codec.unpack(msgpack)
      obj.asInstanceOf[A]
    }
  }

  protected def buildGETRequest(resourcePath: String, requestBody: JSONObject): Request = {
    val queryParams: Seq[String] =
      requestBody.v.map {
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
    r
  }

}

/**
  * A synchronous HttpClient that awaits responses.
  *
  * @param asyncClient
  * @tparam F
  * @tparam Req
  * @tparam Resp
  */
class HttpSyncClientAdapter[F[_], Req, Resp](asyncClient: HttpClient[F, Req, Resp])
    extends HttpSyncClientAdapterBase[F, Req, Resp](asyncClient) {

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses (after reaching
    * the max retry)
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is happend
    */
  override def send(req: Req, requestFilter: Req => Req = identity): Resp = awaitF(asyncClient.send(req, requestFilter))

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  override def sendSafe(req: Req, requestFilter: Req => Req = identity): Resp =
    awaitF(asyncClient.sendSafe(req, requestFilter))

  override def close(): Unit = {
    asyncClient.close()
  }
}

object HttpClient extends LogSupport {}
