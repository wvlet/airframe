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

trait SQLType

object SQLType {
  case class GenericType(name: String, typeArgs: Seq[SQLTypeParam]) extends SQLType {
    override def toString: String = {
      if (typeArgs.isEmpty) name
      else
        s"${name}(${typeArgs.mkString(",")})"
    }
  }

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
  case class TypeVariable(name: String) extends SQLTypeParam {
    override def toString: String = typeName
    override def typeName: String = s"${name}"
  }
}

import SQLType._

object SQLTypeParser extends RegexParsers with LogSupport {

  private def typeName: Parser[String] = "[a-zA-Z_]([a-zA-Z0-9_]+)?".r
  private def number: Parser[Int]      = "[0-9]+".r ^^ { _.toInt }

  def typeParams: Parser[List[SQLTypeParam]] = repsep(typeParam, ",")

  def typeParam: Parser[SQLTypeParam] = typeName ^^ { case name => TypeVariable(name) } |
    number ^^ { case num => NumericTypeParam(num) }

  def genericType: Parser[SQLType] = typeName ~ opt("(" ~ typeParams ~ ")") ^^ {
    case name ~ None                        => GenericType(name, Seq.empty)
    case name ~ Some(_ ~ optTypeParams ~ _) => GenericType(name, optTypeParams)
  }

  def sqlType: Parser[SQLType] = genericType

  def parseSQLType(s: String): Option[SQLType] = {
    parseAll(sqlType, s) match {
      case Success(result, next) => Some(result)
      case Error(msg, next) =>
        warn(msg)
        None
      case Failure(msg, next) =>
        warn(msg)
        None
    }
  }

}
