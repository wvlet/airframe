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
package wvlet.airframe.stream.sql
import wvlet.airframe.stream.spi.SQLModel
import wvlet.airframe.stream.spi.SQLModel._
import wvlet.log.LogSupport

/**
  *
  */
object SQLPrinter extends LogSupport {
  def print(m: SQLModel): String = {
    m match {
      case r: Relation   => printRelation(r)
      case e: Expression => printExpression(e)
      case other =>
        warn(s"unknown model: ${other} (${other.getClass.getSimpleName})")
        other.toString
    }
  }

  def printRelation(r: Relation): String = {
    r match {
      case Select(distinct, selectItems, in, whereExpr) =>
        val b = Seq.newBuilder[String]
        b += "SELECT"
        if (distinct) {
          b += "DISTINCT"
        }
        b += (selectItems.map(x => print(x)).mkString(", "))
        in.map { x =>
          b += "FROM"
          b += printRelation(x)
        }
        whereExpr.map { w =>
          b += "WHERE"
          b += printExpression(w)
        }
        b.result().mkString(" ")
      case Table(t) =>
        printExpression(t)
      case other =>
        warn(s"Unknown relation: ${other}")
        other.toString
    }
  }

  def printExpression(e: Expression): String = {
    e match {
      case SingleColumn(ex, alias) =>
        val col = printExpression(ex)
        alias
          .map(x => s"${col} AS ${x}")
          .getOrElse(col)
      case QName(parts) =>
        parts.mkString(".")
      case c: ConditionalExpression =>
        printConditionalExpression(c)
      case other =>
        warn(s"Unknown Expression: ${e} ${e.getClass.getSimpleName}")
        e.toString
    }
  }

  def printConditionalExpression(c: ConditionalExpression): String = {
    c match {
      case NoOp => ""
      case Eq(a, b) =>
        s"${printExpression(a)} = ${printExpression(b)}"
      case NotEq(a, b) =>
        s"${printExpression(a)} != ${printExpression(b)}"
      case And(a, b) =>
        s"${printExpression(a)} AND ${printExpression(b)}"
      case Or(a, b) =>
        s"${printExpression(a)} OR ${printExpression(b)}"
      case LessThan(a, b) =>
        s"${printExpression(a)} < ${printExpression(b)}"
      case LessThanOrEq(a, b) =>
        s"${printExpression(a)} <= ${printExpression(b)}"
      case GreaterThan(a, b) =>
        s"${printExpression(a)} > ${printExpression(b)}"
      case GreaterThanOrEq(a, b) =>
        s"${printExpression(a)} >= ${printExpression(b)}"
      case Between(a, b) =>
        s"BETWEEN ${printExpression(a)} and ${printExpression(b)}"
      case Between(a, b) =>
        s"BETWEEN ${printExpression(a)} and ${printExpression(b)}"
      case IsNull(a) =>
        s"${printExpression(a)} IS NULL"
      case IsNotNull(a) =>
        s"${printExpression(a)} IS NOT NULL"
      case In(list) =>
        val in = list.map(x => printExpression(x)).mkString(", ")
        s"IN ${in}"
      case NotIn(list) =>
        val in = list.map(x => printExpression(x)).mkString(", ")
        s"NOT IN ${in}"
      case InSubQuery(in) =>
        s"IN (${printRelation(in)})"
      case NotInSubQuery(in) =>
        s"NOT IN (${printRelation(in)})"
      case Like(e) =>
        s"LIKE ${print(e)}"
      case NotLike(e) =>
        s"NOT LIKE ${print(e)}"
      case DistinctFrom(e) =>
        s"DISTINCT FROM ${print(e)}"
      case NotDistinctFrom(e) =>
        s"NOT DISTINCT FROM ${print(e)}"
      case other =>
        warn(s"Unknown ConditionalExpression: ${other} ${other.getClass.getSimpleName}")
        other.toString
    }
  }
}
