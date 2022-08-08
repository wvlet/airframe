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
import wvlet.airframe.sql.catalog.DataType.{ArrayType, DecimalType, GenericType}

import scala.util.parsing.combinator.RegexParsers

abstract class DataType(val typeName: String) {
  def baseTypeName: String      = typeName
  override def toString: String = typeName
}

sealed abstract trait DataTypeParam {
  def typeName: String
}
case class NumericTypeParam(value: Int) extends DataTypeParam {
  def typeName: String = s"${value}"
}
case class TypeVariable(name: String) extends DataTypeParam {
  override def typeName: String = s"${name}"
}

case class NamedType(name: String, dataType: DataType) {
  def typeName: String = s"${name}:${dataType}"
}

object DataType extends LogSupport {

  private def toTypeName(name: String, typeArgs: Seq[DataTypeParam]): String = {
    if (typeArgs.isEmpty)
      name
    else {
      s"${name}(${typeArgs.mkString(",")})"
    }
  }

  case class GenericType(name: String, typeArgs: Seq[DataTypeParam]) extends DataType(toTypeName(name, typeArgs))

  case object UnknownType extends DataType("?")
  case object AnyType     extends DataType("any")
  case object NullType    extends DataType("null")
  case object BooleanType extends DataType("boolean")
  case object StringType  extends DataType("string")
  case object LongType    extends DataType("long")
  case object DoubleType  extends DataType("double")
  case class DecimalType(precision: Int, scale: Int) extends DataType(s"decimal(${precision},${scale})") {
    override def baseTypeName: String = "decimal"
  }
  case object JsonType                     extends DataType("json")
  case object BinaryType                   extends DataType("binary")
  case object TimestampType                extends DataType("timestamp")
  case class ArrayType(elemType: DataType) extends DataType(s"array[${elemType.typeName}]")
  case class MapType(keyType: DataType, valueType: DataType)
      extends DataType(s"map[${keyType.typeName},${valueType.typeName}]")
  case class RecordType(elems: Seq[NamedType]) extends DataType(s"{${elems.map(_.typeName).mkString(",")}}")

  def primitiveTypeOf(dataType: String): DataType = {
    dataType match {
      case "?"                                        => UnknownType
      case "any"                                      => AnyType
      case "null"                                     => NullType
      case "string" | "varchar"                       => StringType
      case "byte" | "char" | "short" | "int" | "long" => LongType
      case "float" | "double"                         => DoubleType
      case "boolean"                                  => BooleanType
      case "json"                                     => JsonType
      case "binary"                                   => BinaryType
      case "timestamp"                                => TimestampType
      case _ =>
        warn(s"Unknown type: ${dataType}. Using 'any' instead")
        AnyType
    }
  }

  def parse(typeName: String): Option[DataType] = {
    DataTypeParser.parseDataType(typeName)
  }

  def parseArgs(typeArgs: String): List[DataType] = {
    DataTypeParser.parseDataTypeList(typeArgs)
  }
}

object DataTypeParser extends RegexParsers with LogSupport {
  override def skipWhitespace = true

  private def typeName: Parser[String] = "[a-zA-Z]+".r
  private def number: Parser[Int]      = "[0-9]+".r ^^ { _.toInt }

  private def primitiveType: Parser[DataType] = typeName ^^ { DataType.primitiveTypeOf(_) }
  private def decimalType: Parser[DataType.DecimalType] =
    "decimal" ~ "(" ~ number ~ "," ~ number ~ ")" ^^ { case _ ~ _ ~ p ~ _ ~ s ~ _ =>
      DecimalType(p, s)
    }

  private def varcharType: Parser[DataType] =
    "varchar" ~ opt("(" ~ (typeName | number) ~ ")") ^^ { case _ => DataType.StringType }

  private def arrayType: Parser[DataType.ArrayType] =
    "array" ~ "[" ~ dataType ~ "]" ^^ { case _ ~ _ ~ x ~ _ =>
      ArrayType(x)
    }
  private def mapType: Parser[DataType.MapType] =
    "map" ~ "[" ~ dataType ~ "," ~ dataType ~ "]" ^^ { case _ ~ _ ~ k ~ _ ~ v ~ _ =>
      DataType.MapType(k, v)
    }

  private def namedType: Parser[NamedType] = typeName ~ ":" ~ dataType ^^ { case n ~ _ ~ t => NamedType(n, t) }
  private def recordType: Parser[DataType.RecordType] =
    "{" ~ namedType ~ rep("," ~ namedType) ~ "}" ^^ { case _ ~ head ~ tail ~ _ =>
      DataType.RecordType(head +: tail.map(_._2).toSeq)
    }

  def dataType: Parser[DataType] = decimalType | varcharType | arrayType | mapType | recordType | primitiveType

  def typeArgs: Parser[List[DataType]] = repsep(dataType, ",")

//  def typeParams: Parser[List[DataTypeParam]] = repsep(typeParam, ",")
//
//  def typeParam: Parser[DataTypeParam] = typeName ^^
//
//  def genericType: Parser[DataType] = typeName ~ opt(typeParams) ^^ { case name ~ optTypeParams =>
//    GenericType(name, optTypeParams.getOrElse(Seq.empty))
//  }

  def parseDataType(s: String): Option[DataType] = {
    parseAll(dataType, s) match {
      case Success(result, next) => Some(result)
      case Error(msg, next) =>
        warn(msg)
        None
      case Failure(msg, next) =>
        warn(msg)
        None
    }
  }

  def parseDataTypeList(s: String): List[DataType] = {
    parseAll(typeArgs, s) match {
      case Success(result, next) => result
      case Error(msg, next) =>
        warn(msg)
        List.empty
      case Failure(msg, next) =>
        warn(msg)
        List.empty
    }
  }

}
