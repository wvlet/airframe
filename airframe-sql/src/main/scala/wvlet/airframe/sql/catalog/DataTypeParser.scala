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
import wvlet.log.LogSupport

import scala.util.parsing.combinator.RegexParsers

object DataTypeParser extends RegexParsers with LogSupport {

  import DataType._

  override def skipWhitespace = true

  private def typeName: Parser[String] = "[a-zA-Z_]([a-zA-Z0-9_]+)?".r

  private def number: Parser[Int] = "[0-9]+".r ^^ {
    _.toInt
  }

  private def identifier: Parser[String] =
    "\"" ~ typeName ~ "\"" ^^ { case _ ~ s ~ _ => s } |
      typeName ^^ { case s => s }

  private def typeParams: Parser[List[DataTypeParam]] = repsep(typeParam, ",")

  private def typeParam: Parser[DataTypeParam] = {
    dataType ^^ { case tpe => DataTypeParam.Unbound(tpe) } |
      number ^^ { case num => DataTypeParam.Numeric(num) }
  }

  private def genericType: Parser[DataType] = typeName ~ opt("(" ~ typeParams ~ ")") ^^ {
    case name ~ None                        => GenericType(name, Seq.empty)
    case name ~ Some(_ ~ optTypeParams ~ _) => GenericType(name, optTypeParams)
  }

  private def intervalDayTimeType: Parser[DataType] = "interval" ~ typeName ~ "to" ~ typeName ^^ {
    case _ ~ from ~ _ ~ to =>
      IntervalDayTimeType(from, to)
  }

  private def recordType: Parser[DataType] = "row" ~ "(" ~ repsep(field, ",") ~ ")" ^^ { case _ ~ _ ~ fields ~ _ =>
    RecordType(fields)
  }

  private def field: Parser[NamedType] = identifier ~ dataType ^^ { case id ~ tpe => NamedType(id, tpe) }

  private def timeType: Parser[DataType] = "time" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimeType(tz.isDefined, Some(precision))
  }

  private def timestampType: Parser[DataType] = "timestamp" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimestampType(tz.isDefined, Some(precision))
  }

  private def primitiveTypeName: Parser[String] = {
    "any" | "null" |
      "string" | "byte" | "char" |
      "short" | "int" | "long" |
      "float" | "real" | "double" |
      "boolean" |
      "json" |
      "binary" |
      "time" |
      "timestamp"
  }

  private def primitiveType: Parser[DataType] = primitiveTypeName ^^ { DataType.primitiveTypeOf(_) }
  private def decimalType: Parser[DataType.DecimalType] =
    "decimal" ~ "(" ~ number ~ "," ~ number ~ ")" ^^ { case _ ~ _ ~ p ~ _ ~ s ~ _ =>
      DecimalType(p, s)
    }

  private def varcharType: Parser[DataType] =
    "varchar" ~ opt("(" ~ (typeName | number) ~ ")") ^^ { case _ ~ _ =>
      StringType
    }

  // private def timestampType: Parser[DataType] =
//    "timestamp" ~ opt("")

  private def arrayType: Parser[DataType.ArrayType] =
    "array" ~ "(" ~ dataType ~ ")" ^^ { case _ ~ _ ~ x ~ _ =>
      ArrayType(x)
    }
  private def mapType: Parser[DataType.MapType] =
    "map" ~ "(" ~ dataType ~ "," ~ dataType ~ ")" ^^ { case _ ~ _ ~ k ~ _ ~ v ~ _ =>
      DataType.MapType(k, v)
    }

  private def namedType: Parser[NamedType] = typeName ~ ":" ~ dataType ^^ { case n ~ _ ~ t => NamedType(n, t) }

  def dataType: Parser[DataType] =
    decimalType |
      varcharType |
      recordType |
      timeType |
      timestampType |
      intervalDayTimeType |
      arrayType |
      mapType |
      recordType |
      primitiveType |
      genericType

  def typeArgs: Parser[List[DataType]] = repsep(dataType, ",")

  private def parseError(msg: String): SQLError = {
    SQLErrorCode.InvalidType.toException(s"Failed to parse SQL Type: ${msg}")
  }

  private def parse[A](target: Parser[A], input: String): A = {
    parseAll(target, input) match {
      case Success(result, next) => result
      case Error(msg, next) =>
        throw parseError(s"${input}: ${msg}")
      case Failure(msg, next) =>
        throw parseError(s"${input}: ${msg}")
    }
  }

  def parseDataType(s: String): DataType           = parse(dataType, s)
  def parseDataTypeList(s: String): List[DataType] = parse(typeArgs, s)

}
