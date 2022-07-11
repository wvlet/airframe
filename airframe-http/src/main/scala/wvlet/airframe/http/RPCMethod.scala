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

import wvlet.airframe.surface.{MethodSurface, Surface, TypeName}

import scala.collection.immutable.ListMap

/**
  * RPC endpoint information
  * @param path
  * @param rpcInterfaceClass
  * @param rpcMethodSurface
  */
case class RPCMethod(
    // RPC endpoint path starting with '/'
    path: String,
    rpcInterface: String,
    methodName: String,
    requestSurface: Surface,
    responseSurface: Surface
) {

  /**
    * Generate a map representation of this method for logging purpose
    */
  def logData: ListMap[String, Any] = ListMap(
    "rpc_interface" -> rpcInterface,
    "rpc_method"    -> methodName
  )
}
