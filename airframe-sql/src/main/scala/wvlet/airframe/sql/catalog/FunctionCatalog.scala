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
package wvlet.airframe.sql.catalog

import wvlet.airframe.sql.{SQLError, SQLErrorCode}

/**
  * Manage the list of unbounded functions, whose types are not resolved yet.
  */
trait FunctionCatalog {
  def listFunctions: Seq[SQLFunction]
}

trait SQLFunction {
  def name: String
}

trait SQLFunctionArg {
  def name: String
  def dataType: DataType
}

case class UnboundFunctionArgument(name: String, dataTypeName: String) extends SQLFunctionArg {
  def bind(typeArg: DataType): BoundFunctionArgument = BoundFunctionArgument(name, typeArg)
  override def dataType: DataType                    = ???
}
case class BoundFunctionArgument(name: String, dataType: DataType)

case class UnboundFunction(name: String, args: Seq[SQLFunctionArg]) extends SQLFunction {
  def bind(typeArgMap: Map[String, DataType]): BoundFunction = {
    val boundArgs: Seq[BoundFunctionArgument] = args.map {
      case b: BoundFunctionArgument =>
        b
      case u: UnboundFunctionArgument =>
        typeArgMap.get(u.dataTypeName) match {
          case Some(dataType) =>
            BoundFunctionArgument(name, dataType)
          case None =>
            throw SQLErrorCode.UnknownDataType.toException(
              s"unknown data type: ${u.dataTypeName}"
            )
        }
    }
    BoundFunction(name, boundArgs)
  }
}

case class BoundFunction(name: String, args: Seq[BoundFunctionArgument]) extends SQLFunction
