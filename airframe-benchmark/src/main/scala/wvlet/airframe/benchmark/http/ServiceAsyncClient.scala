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
package wvlet.airframe.benchmark.http

/**
  */
import wvlet.airframe.http._
import wvlet.airframe.http.client.{AsyncClient, SyncClient}

import scala.language.higherKinds
import scala.collection.immutable.Map
import scala.concurrent.Future

class ServiceClient[F[_], Req, Resp](private val client: HttpClient[F, Req, Resp]) extends AutoCloseable {
  override def close(): Unit              = { client.close() }
  def getClient: HttpClient[F, Req, Resp] = client
  object Greeter {
    def hello(name: String, requestFilter: Req => Req = identity): F[String] = {
      client.postOps[Map[String, Any], String](
        resourcePath = s"/wvlet.airframe.benchmark.http.Greeter/hello",
        Map("name" -> name),
        requestFilter = requestFilter
      )
    }
  }
}

class NewServiceAsyncClient(private val client: AsyncClient) extends AutoCloseable {
  override def close(): Unit = { client.close() }
  def getClient: AsyncClient = client
  object Greeter {
    def hello(name: String): Future[String] = {
      client.call[Map[String, Any], String](
        Http.POST("/wvlet.airframe.benchmark.http.Greeter/hello"),
        Map("name" -> name)
      )
    }
  }
}
