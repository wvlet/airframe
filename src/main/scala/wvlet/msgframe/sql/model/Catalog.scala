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

package wvlet.msgframe.sql.model
import wvlet.airframe.surface.Surface

/**
  *
  */
object Catalog {
  case class Database(name: String)

  sealed trait Schema
  case class AnonSchema(columns: Seq[Column])                extends Schema
  case class TableSchema(name: String, columns: Seq[Column]) extends Schema

  case class Column(name: String, columnType: DataType)

  sealed trait DataType
  case object IntegerType                                    extends DataType
  case object FloatType                                      extends DataType
  case object BooleanType                                    extends DataType
  case object StringType                                     extends DataType
  case object JSONType                                       extends DataType
  case object BinaryType                                     extends DataType
  case object NullType                                       extends DataType
  case object TimestampType                                  extends DataType
  case class ObjectType(surface: Surface)                    extends DataType
  case class AraryType(elemType: DataType)                   extends DataType
  case class MapType(keyType: DataType, valueType: DataType) extends DataType
  case object AnyType                                        extends DataType
}
