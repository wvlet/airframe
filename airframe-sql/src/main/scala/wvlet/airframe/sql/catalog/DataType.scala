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
  override def toString: String = typeName
  def baseTypeName: String      = typeName
  def typeParams: Seq[DataType] = Seq.empty

  def isBound: Boolean                                  = typeParams.forall(_.isBound)
  def bind(typeArgMap: Map[String, DataType]): DataType = this
}

case class NamedType(name: String, dataType: DataType) {
  def typeName: String = s"${name}:${dataType}"
}

object DataType extends LogSupport {

  /**
    * DataType parameter for representing concrete types like timestamp(2), and abstract types like timestamp(p).
    */
  sealed trait TypeParameter

  /**
    * Constant type used for arguments of varchar(n), char(n), decimal(p, q), etc.
    */
  case class IntConstant(value: Int) extends DataType(s"${value}") with TypeParameter
  case class TypeVariable(name: String) extends DataType(name) with TypeParameter {
    override def isBound: Boolean = false
    override def bind(typeArgMap: Map[String, DataType]): DataType = {
      typeArgMap.get(name) match {
        case Some(t) => t
        case None    => this
      }
    }
  }

  case class GenericType(override val typeName: String, override val typeParams: Seq[DataType] = Seq.empty)
      extends DataType(typeNameOf(typeName, typeParams)) {
    override def isBound: Boolean = typeParams.forall(_.isBound)

    override def bind(typeArgMap: Map[String, DataType]): DataType = {
      GenericType(typeName, typeParams.map(_.bind(typeArgMap)))
    }
  }

  case class IntervalDayTimeType(from: String, to: String) extends DataType(s"interval ${from} to ${to}")

  sealed trait TimestampField
  object TimestampField {
    case object TIME      extends TimestampField
    case object TIMESTAMP extends TimestampField
  }
  case class TimestampType(field: TimestampField, withTimeZone: Boolean = true, precision: Option[DataType] = None)
      extends DataType(
        typeNameOf(field.toString.toLowerCase, precision.toSeq) + (if (withTimeZone) " with time zone" else "")
      ) {
    override def typeParams: Seq[DataType] = precision.toSeq
  }

  private def typeNameOf(name: String, typeArgs: Seq[DataType]): String = {
    if (typeArgs.isEmpty)
      name
    else {
      s"${name}(${typeArgs.mkString(",")})"
    }
  }

  case object AnyType  extends DataType("any")
  case object NullType extends DataType("null")

  case object BooleanType                                   extends DataType("boolean")
  abstract class NumericType(override val typeName: String) extends DataType(typeName)
  case object ByteType                                      extends NumericType("byte")
  case object ShortType                                     extends NumericType("short")
  case object IntegerType                                   extends NumericType("integer")
  case object LongType                                      extends NumericType("long")

  abstract class FractionType(override val typeName: String) extends NumericType(typeName)
  case object FloatType                                      extends FractionType("float")
  case object RealType                                       extends FractionType("real")
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

  def parse(typeName: String): DataType = {
    DataTypeParser.parse(typeName)
  }

  def parseArgs(typeArgs: String): List[DataType] = {
    DataTypeParser.parseTypeList(typeArgs)
  }

}
