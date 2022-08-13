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

/**
  * SQL types that can be used without a real DBMS catalog.
  */
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
