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
package example.generic
import wvlet.airframe.http.Endpoint
import wvlet.airframe.http.HttpMessage.Response

import scala.concurrent.Future
import scala.language.higherKinds

/**
  *
  */
trait GenericService[F[_]] {
  @Endpoint(path = "/v1/hello")
  def hello: F[String]

  @Endpoint(path = "/v1/hello_f")
  def concreteFuture: Future[Int]

  @Endpoint(path = "/v1/hello_ops")
  def ops(in: String): Future[Response]

  @Endpoint(path = "/v1/hello_ops2")
  def ops2(in: String): F[Response]

}
