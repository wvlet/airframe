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

import wvlet.airframe.http.{HttpClientException, HttpClientMaxRetryException}
import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.Future

/**
  * A standard blocking http client interface
  */
trait HttpSyncClient extends AutoCloseable {

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
trait HttpAsyncClient extends AutoCloseable {

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
