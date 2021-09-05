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
package example.api
import wvlet.airframe.http.RPC

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
