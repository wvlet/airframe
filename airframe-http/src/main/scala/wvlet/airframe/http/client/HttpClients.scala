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
trait HttpSyncClient {

  /**
    * Send an HTTP request and get the response. It will throw an exception for non successful responses (after reaching
    * the max retry limit)
    *
    * @throws HttpClientMaxRetryException
    *   if max retry reaches
    * @throws HttpClientException
    *   for non-retryable error is happend
    */
  def send(req: Request, requestFilter: Request => Request = identity): Response

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
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
  * A standard asyn http client interface using Scala Future
  */
trait HttpAsyncClient {

  /**
    * Send an HTTP request and get the response. It will return an exception for non successful responses (after
    * reaching the max retry limit)
    */
  def send(req: Request, requestFilter: Request => Request = identity): Future[Response]

  /**
    * Send an HTTP request and returns a response (or the last response if the request is retried)
    */
  def sendSafe(req: Request, requestFilter: Request => Request = identity): Future[Response]
}
