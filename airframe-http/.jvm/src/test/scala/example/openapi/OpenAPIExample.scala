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
package example.openapi
import wvlet.airframe.http.HttpMessage.Response
import wvlet.airframe.http.{Endpoint, HttpMethod, RPC}

import scala.concurrent.Future

/**
  */
@RPC
trait OpenAPIRPCExample {
  import OpenAPIRPCExample._

  def zeroAryRPC: Unit
  def rpcWithPrimitive(p1: Int): Int
  def rpcWithMultiplePrimitives(p1: Int, p2: String): Int
  def rpcWithComplexParam(p1: RPCRequest): RPCResponse
  def rpcWithMultipleParams(p1: Int, p2: RPCRequest): RPCResponse
  def rpcWithOption(p1: Option[String]): Unit
  def rpcWithPrimitiveAndOption(p1: String, p2: Option[String]): Unit
  def rpcWithOptionOfComplexType(p1: Option[RPCRequest]): Unit
  def rpcWithFutureResponse: Future[String]
  def rpcWithRawResponse: Response
}

object OpenAPIRPCExample {
  case class RPCRequest(
      x1: Int,
      x2: Long,
      x3: Boolean,
      x4: Float,
      x5: Double,
      x6: Array[String],
      x7: Seq[String],
      x8: Map[String, Any],
      x9: Option[Int] = None
  )
  case class RPCResponse(y1: String, y2: Boolean)
}

trait OpenAPIEndpointExample {
  import OpenAPIEndpointExample._

  @Endpoint(method = HttpMethod.GET, path = "/v1/get0")
  def get0(): Unit

  @Endpoint(method = HttpMethod.GET, path = "/v1/get1/:id")
  def get1(id: Int): Unit

  @Endpoint(method = HttpMethod.GET, path = "/v1/get2/:id/:name")
  def get2(id: Int, name: String): Unit

  @Endpoint(method = HttpMethod.GET, path = "/v1/get3/:id")
  def get3(id: Int, p1: String): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/post1")
  def post1(): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/post2/:id")
  def post2(id: Int): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/post3/:id/:name")
  def post3(id: Int, name: String): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/post4/:id")
  def post4(id: Int, p1: String): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/post5")
  def post5(p1: EndpointRequest): EndpointResponse

  @Endpoint(method = HttpMethod.POST, path = "/v1/post6/:id")
  def post6(id: Int, p1: EndpointRequest): EndpointResponse

  @Endpoint(method = HttpMethod.PUT, path = "/v1/put1")
  def put1(): Unit

  @Endpoint(method = HttpMethod.DELETE, path = "/v1/delete1")
  def delete1(): Unit

  @Endpoint(method = HttpMethod.PATCH, path = "/v1/patch1")
  def patch1(): Unit

  @Endpoint(method = HttpMethod.HEAD, path = "/v1/head1")
  def head1(): Unit

  @Endpoint(method = HttpMethod.OPTIONS, path = "/v1/options1")
  def options1(): Unit

  @Endpoint(method = HttpMethod.TRACE, path = "/v1/trace1")
  def trace1(): Unit

  @Endpoint(method = HttpMethod.POST, path = "/v1/multi_method")
  def multi1(): Unit

  @Endpoint(method = HttpMethod.OPTIONS, path = "/v1/multi_method")
  def multi2(): Unit
}

object OpenAPIEndpointExample {
  case class EndpointRequest(
      x1: Int,
      x2: Long,
      x3: Boolean,
      x4: Float,
      x5: Double,
      x6: Array[String],
      x7: Seq[String],
      x8: Map[String, Any],
      x9: Option[Int] = None
  )
  case class EndpointResponse(y1: String, y2: Boolean)
}
