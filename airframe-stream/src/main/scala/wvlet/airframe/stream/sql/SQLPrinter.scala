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
import wvlet.airframe.stream.sql.parser.SqlBaseParser.BooleanLiteralContext
import wvlet.log.LogSupport

/**
  *
  */
object SQLPrinter extends LogSupport {

  private def unknown(e: SQLModel): String = {
    if (e != null) {
      warn(s"Unknown model: ${e} ${e.getClass.getSimpleName}")
      e.toString
    } else {
      ""
    }
  }

  def print(m: SQLModel): String = {
    m match {
      case r: Relation   => printRelation(r)
      case e: Expression => printExpression(e)
      case other         => unknown(other)
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
      case Aggregate(selectItems, in, whereExpr, groupingKeys, having) =>
        val b = Seq.newBuilder[String]
        b += "SELECT"
        b += (selectItems.map(x => print(x)).mkString(", "))
        in.map { x =>
          b += "FROM"
          b += printRelation(x)
        }
        whereExpr.map { w =>
          b += "WHERE"
          b += printExpression(w)
        }
        b += s"GROUP BY ${groupingKeys.map(x => printExpression(x)).mkString(", ")}"
        having.map { h =>
          b += "HAVING"
          b += printExpression(h)
        }
        b.result().mkString(" ")
      case Query(withQuery, body) =>
        val s = Seq.newBuilder[String]
        s += "WITH"
        if (withQuery.recursive) {
          s += "RECURSIVE"
        }
        s += withQuery.queries
          .map { q =>
            val columnAliases = q.columnNames.map(x => s"(${x.map(printExpression(_)).mkString(", ")})").getOrElse("")
            s"${printExpression(q.name)}${columnAliases} AS (${printRelation(q.query)})"
          }.mkString(", ")
        s += printRelation(body)
        s.result().mkString(" ")
      case Union(relations, isDistinct) =>
        val op = if (isDistinct) " UNION " else " UNION ALL "
        relations.map(printRelation(_)).mkString(op)
      case Table(t) =>
        printExpression(t)
      case Limit(in, l) =>
        s"${printRelation(in)} LIMIT ${l}"
      case Sort(in, orderBy) =>
        val order = orderBy.map(x => printExpression(x)).mkString(", ")
        s"${printRelation(in)} ORDER BY ${order}"
      case ParenthizedRelation(r) =>
        s"(${printRelation(r)})"
      case AliasedRelation(relation, alias, columnNames) =>
        val r = printRelation(relation)
        val c = columnNames.map(x => s"(${x.mkString(", ")})").getOrElse("")
        relation match {
          case Table(x) => s"${r} AS ${alias}${c}"
          case _        => s"(${r}) AS ${alias}${c}"
        }
      case Join(joinType, left, right, cond) =>
        val l = printRelation(left)
        val r = printRelation(right)
        val c = cond match {
          case NaturalJoin        => ""
          case JoinUsing(columns) => s" USING ${columns.mkString(", ")}"
          case JoinOn(expr)       => s" ON ${printExpression(expr)}"
        }
        joinType match {
          case InnerJoin      => s"${l} JOIN ${r}${c}"
          case LeftOuterJoin  => s"${l} LEFT JOIN ${r}${c}"
          case RightOuterJoin => s"${l} RIGHT JOIN ${r}${c}"
          case FullOuterJoin  => s"${l} FULL OUTER JOIN ${r}${c}"
          case CrossJoin      => s"${l} CROSS JOIN ${r}${c}"
          case ImplicitJoin   => s"${l}, ${r}${c}"
        }
      case other => unknown(other)
    }
  }

  def printExpression(e: Expression): String = {
    e match {
      case i: Identifier =>
        i.toString
      case ParenthizedExpression(expr) =>
        s"(${printExpression(expr)})"
      case SingleColumn(ex, alias) =>
        val col = printExpression(ex)
        alias
          .map(x => s"${col} AS ${printExpression(x)}")
          .getOrElse(col)
      case AllColumns(prefix) =>
        prefix.map(p => s"${p}.*").getOrElse("*")
      case l: Literal =>
        printLiteral(l)
      case SortItem(key, ordering, nullOrdering) =>
        val k  = printExpression(key)
        val o  = ordering.map(x => s" ${x}").getOrElse("")
        val no = nullOrdering.map(x => s" ${x}").getOrElse("")
        s"${k}${o}${no}"
      case FunctionCall(name, args, distinct, filter, window) =>
        val argList = args.map(printExpression(_)).mkString(", ")
        val d       = if (distinct) "DISTINCT " else ""
        s"${name}(${d}${argList})"
      case QName(parts) =>
        parts.mkString(".")
      case Cast(expr, tpe, tryCast) =>
        val cmd = if (tryCast) "TRY_CAST" else "CAST"
        s"${cmd}(${printExpression(expr)} AS ${tpe})"
      case c: ConditionalExpression =>
        printConditionalExpression(c)
      case ArithmeticBinaryExpr(tpe, left, right) =>
        s"${printExpression(left)} ${tpe.symbol} ${printExpression(right)}"
      case ArithmeticUnaryExpr(sign, value) =>
        s"${sign.symbol} ${printExpression(value)}"
      case Exists(subQuery) =>
        s"EXISTS(${printExpression(subQuery)})"
      case SubQueryExpression(query) =>
        s"(${printRelation(query)})"
      case CaseExpr(operand, whenClauses, defaultValue) =>
        val s = Seq.newBuilder[String]
        s += "CASE"
        operand.map(x => s += printExpression(x))
        whenClauses.map { w =>
          s += "WHEN"
          s += printExpression(w.condition)
          s += "THEN"
          s += printExpression(w.result)
        }
        defaultValue.map { x =>
          s += "ELSE"
          s += printExpression(x)
        }
        s += "END"
        s.result().mkString(" ")
      case other => unknown(other)
    }
  }
  def printLiteral(l: Literal): String = {
    l.toString
  }

  def printConditionalExpression(c: ConditionalExpression): String = {
    c match {
      case NoOp => ""
      case Eq(a, b) =>
        s"${printExpression(a)} = ${printExpression(b)}"
      case NotEq(a, b) =>
        s"${printExpression(a)} <> ${printExpression(b)}"
      case And(a, b) =>
        s"${printExpression(a)} AND ${printExpression(b)}"
      case Or(a, b) =>
        s"${printExpression(a)} OR ${printExpression(b)}"
      case Not(e) =>
        s"NOT ${printExpression(e)}"
      case LessThan(a, b) =>
        s"${printExpression(a)} < ${printExpression(b)}"
      case LessThanOrEq(a, b) =>
        s"${printExpression(a)} <= ${printExpression(b)}"
      case GreaterThan(a, b) =>
        s"${printExpression(a)} > ${printExpression(b)}"
      case GreaterThanOrEq(a, b) =>
        s"${printExpression(a)} >= ${printExpression(b)}"
      case Between(e, a, b) =>
        s"${printExpression(e)} BETWEEN ${printExpression(a)} and ${printExpression(b)}"
      case IsNull(a) =>
        s"${printExpression(a)} IS NULL"
      case IsNotNull(a) =>
        s"${printExpression(a)} IS NOT NULL"
      case In(a, list) =>
        val in = list.map(x => printExpression(x)).mkString(", ")
        s"${printExpression(a)} IN (${in})"
      case NotIn(a, list) =>
        val in = list.map(x => printExpression(x)).mkString(", ")
        s"${printExpression(a)} NOT IN (${in})"
      case InSubQuery(a, in) =>
        s"${printExpression(a)} IN (${printRelation(in)})"
      case NotInSubQuery(a, in) =>
        s"${printExpression(a)} NOT IN (${printRelation(in)})"
      case Like(a, e) =>
        s"${printExpression(a)} LIKE ${print(e)}"
      case NotLike(a, e) =>
        s"${printExpression(a)} NOT LIKE ${print(e)}"
      case DistinctFrom(a, e) =>
        s"${printExpression(a)} IS DISTINCT FROM ${print(e)}"
      case NotDistinctFrom(a, e) =>
        s"${printExpression(a)} IS NOT DISTINCT FROM ${print(e)}"
      case other => unknown(other)
    }
  }
}
