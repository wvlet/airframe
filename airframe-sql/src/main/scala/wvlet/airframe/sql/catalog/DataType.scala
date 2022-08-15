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
import wvlet.log.LogSupport

abstract class DataType(val typeName: String) {
  def baseTypeName: String      = typeName
  override def toString: String = typeName
  def isBound: Boolean          = true

  def bind(typeArgMap: Map[String, DataType]): DataType = this
}

case class NamedType(name: String, dataType: DataType) {
  def typeName: String = s"${name}:${dataType}"
}

object DataType extends LogSupport {

  /**
    * DataType parameter for representing concrete types like timestamp(2), and abstract types like timestamp(p).
    */
  sealed trait DataTypeParam {
    def typeName: String
    def isBound: Boolean
    def bind(typeArgMap: Map[String, DataType]): DataTypeParam
  }
  object DataTypeParam {
    case class Numeric(value: Int) extends DataTypeParam {
      override def toString: String = typeName
      def typeName: String          = s"${value}"

      override def isBound: Boolean                                       = true
      override def bind(typeArgMap: Map[String, DataType]): DataTypeParam = this
    }
    case class Unbound(dataType: DataType) extends DataTypeParam {
      override def toString: String = typeName
      override def typeName: String = s"${dataType.toString}"
      override def isBound: Boolean = dataType.isBound

      override def bind(typeArgMap: Map[String, DataType]): DataTypeParam = {
        dataType.bind(typeArgMap) match {
          case dt if dt eq dataType => this
          case dt                   => Unbound(dt)
        }
      }
    }
  }

  /**
    * UnboundType is a type with type variables
    * @param name
    */
  case class UnboundType(name: String) extends DataType(name) {
    override def isBound: Boolean = false
    override def bind(typeArgMap: Map[String, DataType]): DataType = {
      typeArgMap.get(name) match {
        case Some(t) => t
        case None    => this
      }
    }
  }

  case class GenericType(override val typeName: String, typeArgs: Seq[DataTypeParam] = Seq.empty)
      extends DataType(typeNameOf(typeName, typeArgs)) {
    override def isBound: Boolean = typeArgs.forall(_.isBound)

    override def bind(typeArgMap: Map[String, DataType]): DataType = {
      GenericType(typeName, typeArgs.map(_.bind(typeArgMap)))
    }
  }

  case class IntervalDayTimeType(from: String, to: String) extends DataType(s"interval ${from} to ${to}")
  case class TimeType(withTimeZone: Boolean = false, precision: Option[DataTypeParam] = None)
      extends DataType(typeNameOf("time", precision.toSeq))
  case class TimestampType(withTimeZone: Boolean = false, precision: Option[DataTypeParam] = None)
      extends DataType(typeNameOf("timestamp", precision.toSeq))

  private def typeNameOf(name: String, typeArgs: Seq[DataTypeParam]): String = {
    if (typeArgs.isEmpty)
      name
    else {
      s"${name}(${typeArgs.mkString(",")})"
    }
  }

  case object UnknownType extends DataType("?")
  case object AnyType     extends DataType("any")
  case object NullType    extends DataType("null")

  case object BooleanType extends DataType("boolean")

  abstract class NumericType(override val typeName: String) extends DataType(typeName)
  case object ByteType                                      extends NumericType("byte")
  case object ShortType                                     extends NumericType("short")
  case object IntegerType                                   extends NumericType("integer")
  case object LongType                                      extends NumericType("long")

  abstract class FractionType(override val typeName: String) extends NumericType(typeName)
  case object FloatType                                      extends FractionType("float")
  case object DoubleType                                     extends FractionType("double")

  case class CharType(length: Int)    extends DataType(s"char(${length})")
  case object StringType              extends DataType("string")
  case class VarcharType(length: Int) extends DataType(s"varchar(${length})")
  case class DecimalType(precision: Int, scale: Int) extends DataType(s"decimal(${precision},${scale})") {
    override def baseTypeName: String = "decimal"
  }

  case object JsonType                     extends DataType("json")
  case object BinaryType                   extends DataType("binary")
  case class ArrayType(elemType: DataType) extends DataType(s"array(${elemType.typeName})")
  case class MapType(keyType: DataType, valueType: DataType)
      extends DataType(s"map(${keyType.typeName},${valueType.typeName})")
  case class RecordType(elems: Seq[NamedType]) extends DataType(s"record(${elems.map(_.typeName).mkString(",")})")

  def primitiveTypeOf(dataType: String): DataType = {
    dataType match {
      case "?"                                                    => UnknownType
      case "any"                                                  => AnyType
      case "null"                                                 => NullType
      case "string" | "varchar"                                   => StringType
      case "byte" | "char" | "short" | "int" | "integer" | "long" => LongType
      case "float" | "real" | "double"                            => DoubleType
      case "boolean"                                              => BooleanType
      case "json"                                                 => JsonType
      case "binary"                                               => BinaryType
      case _ =>
        warn(s"Unknown type: ${dataType}")
        UnknownType
    }
  }

  def parse(typeName: String): DataType = {
    DataTypeParser.parse(typeName)
  }

  def parseArgs(typeArgs: String): List[DataType] = {
    DataTypeParser.parseTypeList(typeArgs)
  }

}
