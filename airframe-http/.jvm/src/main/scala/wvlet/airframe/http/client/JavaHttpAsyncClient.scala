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

import wvlet.airframe.http.HttpMessage.{Request, Response}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * An wrapper of JavaHttpSyncClient for supporting async response
  * @param syncClient
  */
class JavaHttpAsyncClient(syncClient: JavaHttpSyncClient) extends HttpAsyncClient {

  private implicit val ec = syncClient.config.executionContextProvider()

  override def send(req: Request, requestFilter: Request => Request): Future[Response] = {
    val p = Promise[Response]()
    Future
      .apply {
        syncClient.send(req, requestFilter)
      }
      .transform { result =>
        result match {
          case Success(resp) =>
            p.success(resp)
          case Failure(ex) =>
            p.failure(ex)
        }
        result
      }

    p.future
  }

  override def close(): Unit = {
    // no-op
  }

}
