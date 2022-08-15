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

/**
  * Manage the list of unbounded functions, whose types are not resolved yet.
  */
trait FunctionCatalog {
  def listFunctions: Seq[SQLFunction]
}

trait SQLFunction {
  def name: String
  def args: Seq[DataType]
  def returnType: DataType
}

trait SQLFunctionType {
  def dataType: DataType
}

case class BoundFunction(name: String, args: Seq[DataType], returnType: DataType) extends SQLFunction {
  require(args.forall(_.isBound), s"Found unbound arguments: ${this}")
  require(returnType.isBound, s"return type: ${returnType} is not bound")
}

case class UnboundFunction(name: String, args: Seq[DataType], returnType: DataType) extends SQLFunction {
  def bind(typeArgMap: Map[String, DataType]): BoundFunction = {
    BoundFunction(name, args.map(_.bind(typeArgMap)), returnType.bind(typeArgMap))
  }
}

object UnboundFunction {
  def parse(name: String, argTypeStr: String, returnTypeStr: String): SQLFunction = {
    val argTypes = DataTypeParser.parseTypeList(argTypeStr)
    val retType  = DataTypeParser.parse(returnTypeStr)
    UnboundFunction(name, argTypes, retType)
  }
}
