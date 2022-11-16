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

  private def typeParams: Parser[List[DataType]] = repsep(typeParam, ",")

  private def typeParam: Parser[DataType] = {
    dataType ^^ { case tpe => tpe } |
      number ^^ { case num => DataType.IntConstant(num) }
  }

  private def genericType: Parser[DataType] = typeName ~ opt("(" ~ typeParams ~ ")") ^? {
    case name ~ Some(_ ~ optTypeParams ~ _) if isKnownGenericTypeName(name) => GenericType(name, optTypeParams)
    case name ~ None if isKnownGenericTypeName(name)                        => GenericType(name)
    case name ~ None                                                        => TypeVariable(name)
  }

  private def intervalDayTimeType: Parser[DataType] = "interval" ~ typeName ~ "to" ~ typeName ^^ {
    case _ ~ from ~ _ ~ to =>
      IntervalDayTimeType(from, to)
  }

  private def recordType: Parser[DataType] = "row" ~ "(" ~ repsep(field, ",") ~ ")" ^^ { case _ ~ _ ~ fields ~ _ =>
    RecordType(fields)
  }

  private def field: Parser[DataType] = {
    identifier ~ dataType ^^ { case id ~ tpe => NamedType(id, tpe) } |
      dataType ^^ { case d => d }
  }
  // typeName ~ ":" ~ dataType ^^ { case n ~ _ ~ t => NamedType(n, t) }

  private def timeType: Parser[DataType] = "time" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimestampType(TimestampField.TIME, tz.isDefined, Some(precision))
  }

  private def timestampType: Parser[DataType] = "timestamp" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimestampType(TimestampField.TIMESTAMP, tz.isDefined, Some(precision))
  }

  private def decimalType: Parser[DataType.DecimalType] =
    "decimal" ~ "(" ~ typeParam ~ "," ~ typeParam ~ ")" ^^ { case _ ~ _ ~ p ~ _ ~ s ~ _ =>
      DecimalType(p, s)
    }

  private def varcharType: Parser[DataType] =
    "varchar" ~ opt("(" ~ typeParam ~ ")") ^^ { case _ ~ t =>
      t match {
        case Some(_ ~ p ~ _) => VarcharType(Some(p))
        case _               => VarcharType(None)
      }
    }

  private def arrayType: Parser[DataType.ArrayType] = {
    "array" ~ "(" ~ dataType ~ ")" ^^ { case _ ~ _ ~ x ~ _ => ArrayType(x) } |
      "array" ~ "<" ~ dataType ~ ">" ^^ { case _ ~ _ ~ x ~ _ => ArrayType(x) }
  }

  private def mapType: Parser[DataType.MapType] = {
    "map" ~ "(" ~ dataType ~ "," ~ dataType ~ ")" ^^ { case _ ~ _ ~ k ~ _ ~ v ~ _ => MapType(k, v) } |
      "map" ~ "<" ~ dataType ~ "," ~ dataType ~ ">" ^^ { case _ ~ _ ~ k ~ _ ~ v ~ _ => MapType(k, v) }
  }

  private def primitiveType: Parser[DataType] = {
    typeName ^? { case str if DataType.isPrimitiveTypeName(str) => DataType.getPrimitiveType(str) }
    // nullType | booleanType | numericType | stringType | anyType | jsonType | binaryType | dateType | ipAddressType
  }

  def dataType: Parser[DataType] = {
    // interval type needs to come first before primitive int, integer types
    intervalDayTimeType | primitiveType | dataTypeWithParam | genericType
  }

  def dataTypeWithParam: Parser[DataType] = {
    decimalType | varcharType | timeType | timestampType | arrayType | mapType | recordType
  }

  def typeArgs: Parser[List[DataType]] = repsep(dataType, ",")

  private def parseError(msg: String): SQLError = {
    SQLErrorCode.InvalidType.newException(s"Failed to parse DataType ${msg}", None)
  }

  private def parse[A](target: Parser[A], input: String): A = {
    parseAll(target, input) match {
      case Success(result, next) => result
      case Error(msg, next) =>
        throw parseError(s"'${input}': ${msg}")
      case Failure(msg, next) =>
        throw parseError(s"'${input}': ${msg}")
    }
  }

  def parse(s: String): DataType               = parse(dataType, s)
  def parseTypeList(s: String): List[DataType] = parse(typeArgs, s)

}
