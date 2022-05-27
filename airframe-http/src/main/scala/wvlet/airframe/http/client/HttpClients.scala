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

import wvlet.airframe.codec.MessageCodec
import wvlet.airframe.http.HttpClient.urlEncode
import wvlet.airframe.http.{
  Http,
  HttpClientConfig,
  HttpClientException,
  HttpClientMaxRetryException,
  HttpResponseBodyCodec
}
import wvlet.airframe.http.HttpMessage.{Request, Response}
import wvlet.airframe.json.JSON.{JSONArray, JSONObject}
import wvlet.airframe.surface.Surface

import scala.concurrent.Future

/**
  * A standard blocking http client interface
  */
trait SyncClient extends SyncClientBase with AutoCloseable {

  private[client] def config: HttpClientConfig

  /**
    * Send an HTTP request and get the response. It will throw an exception for non-successful responses. For example,
    * when receiving non-retryable status code (e.g., 4xx), it will throw HttpClientException. For server side failures
    * (5xx responses), this continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, HttpClientMaxRetryException will be thrown.
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is occurred
    */
  def send(req: Request, requestFilter: Request => Request = identity): Response

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried). Unlike [[send()]],
    * this method returns a regular Http Response object even for non-retryable responses (e.g., 4xx error code). For
    * retryable responses (e.g., 5xx) this continues retry until the max retry count.
    *
    * After reaching the max retry count, it will return a the last response even for 5xx status code.
    */
  def sendSafe(req: Request, requestFilter: Request => Request = identity): Response = {
    try {
      send(req, requestFilter)
    } catch {
      case e: HttpClientException =>
        e.response.toHttpResponse
    }
  }
}

/**
  * A standard async http client interface for Scala Future
  */
trait AsyncClient extends AutoCloseable {
  private[client] def config: HttpClientConfig

  /**
    * Send an HTTP request and get the response in Scala Future type.
    *
    * It will return `Future[HttpClientException]` for non-successful responses. For example, when receiving
    * non-retryable status code (e.g., 4xx), it will return Future[HttpClientException]. For server side failures (5xx
    * responses), this continues request retry until the max retry count.
    *
    * If it exceeds the number of max retry attempts, it will return Future[HttpClientMaxRetryException].
    */
  def send(req: Request, requestFilter: Request => Request = identity): Future[Response]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    *
    * @param req
    * @param requestFilter
    * @return
    */
  def sendSafe(req: Request, requestFilter: Request => Request = identity): Future[Response]

}

object HttpClients {

  private val standardResponseCodec = new HttpResponseBodyCodec[Response]

  private[client] def convertAs[A](response: Response, surface: Surface): A = {
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

  /**
    * Build a GET request for RPC calls
    * @param resourcePath
    * @param requestBody
    * @return
    */
  private[client] def buildGETRequest(resourcePath: String, requestBody: JSONObject): Request = {
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
