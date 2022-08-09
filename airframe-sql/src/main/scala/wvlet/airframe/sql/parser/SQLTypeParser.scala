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
package wvlet.airframe.sql.parser

import wvlet.log.LogSupport

import scala.util.parsing.combinator.RegexParsers

sealed trait SQLType

object SQLType {
  case class GenericType(name: String, typeArgs: Seq[SQLTypeParam]) extends SQLType {
    override def toString: String = {
      if (typeArgs.isEmpty) name
      else
        s"${name}(${typeArgs.mkString(",")})"
    }
  }

  case class IntervalDayTimeType(from: String, to: String) extends SQLType {
    override def toString: String = s"interval ${from} to ${to}"
  }

  case class Field(name: String, sqlType: SQLType)
  case class RowType(fields: Seq[Field]) extends SQLType

  case class TimeType(withTimeZone: Boolean, precision: Option[SQLTypeParam] = None)      extends SQLType
  case class TimestampType(withTimeZone: Boolean, precision: Option[SQLTypeParam] = None) extends SQLType

  private def toTypeName(name: String, typeArgs: Seq[SQLTypeParam]): String = {
    if (typeArgs.isEmpty)
      name
    else {
      s"${name}(${typeArgs.mkString(",")})"
    }
  }

  sealed abstract trait SQLTypeParam {
    def typeName: String
  }

  case class NumericTypeParam(value: Int) extends SQLTypeParam {
    override def toString: String = typeName
    def typeName: String          = s"${value}"
  }
  case class TypeParam(sqlType: SQLType) extends SQLTypeParam {
    override def toString: String = typeName
    override def typeName: String = s"${sqlType.toString}"
  }
}

import SQLType._

/**
  * A parser for generic SQL types that can be returned from Trino, other DBMS, etc.
  */
object SQLTypeParser extends RegexParsers with LogSupport {
  override def skipWhitespace: Boolean = true

  private def identifier: Parser[String] =
    "\"" ~ typeName ~ "\"" ^^ { case _ ~ s ~ _ => s } |
      typeName ^^ { case s => s }

  private def typeName: Parser[String] = "[a-zA-Z_]([a-zA-Z0-9_]+)?".r
  private def number: Parser[Int]      = "[0-9]+".r ^^ { _.toInt }

  private def typeParams: Parser[List[SQLTypeParam]] = repsep(typeParam, ",")

  private def typeParam: Parser[SQLTypeParam] = {
    sqlType ^^ { case tpe => TypeParam(tpe) } |
      number ^^ { case num => NumericTypeParam(num) }
  }

  private def genericType: Parser[SQLType] = typeName ~ opt("(" ~ typeParams ~ ")") ^^ {
    case name ~ None                        => GenericType(name, Seq.empty)
    case name ~ Some(_ ~ optTypeParams ~ _) => GenericType(name, optTypeParams)
  }

  private def intervalDayTimeType: Parser[SQLType] = "interval" ~ typeName ~ "to" ~ typeName ^^ {
    case _ ~ from ~ _ ~ to =>
      IntervalDayTimeType(from, to)
  }

  private def rowType: Parser[SQLType] = "row" ~ "(" ~ repsep(field, ",") ~ ")" ^^ { case _ ~ _ ~ fields ~ _ =>
    RowType(fields)
  }
  private def field: Parser[Field] = identifier ~ sqlType ^^ { case id ~ tpe => Field(id, tpe) }

  private def timeType: Parser[SQLType] = "time" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimeType(tz.isDefined, Some(precision))
  }
  private def timestampType: Parser[SQLType] = "timestamp" ~ "(" ~ typeParam ~ ")" ~ opt("with time zone") ^^ {
    case _ ~ _ ~ precision ~ _ ~ tz => TimestampType(tz.isDefined, Some(precision))
  }

  def sqlType: Parser[SQLType] = intervalDayTimeType | rowType | timeType | timestampType | genericType

  def parseSQLType(s: String): SQLType = {
    parseAll(sqlType, s) match {
      case Success(result, next) => result
      case Error(msg, next) =>
        throw new SQLParseError(s"Failed to parse SQL type ${s}: ${msg}", 0, 0, null)
      case Failure(msg, next) =>
        throw new SQLParseError(s"Failed to parse SQL type ${s}: ${msg}", 0, 0, null)
    }
  }

}
