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

package wvlet.msgframe.sql.parser
import wvlet.log.LogSupport
import wvlet.msgframe.sql.model.LogicalPlan._
import wvlet.msgframe.sql.model.{Attribute, Expression, LogicalPlan, _}

/**
  * Print LogicalPlans As SQL statements
  */
object SQLGenerator extends LogSupport {

  private def unknown(e: Any): String = {
    if (e != null) {
      warn(s"Unknown model: ${e} ${e.getClass.getSimpleName}")
      e.toString
    } else {
      ""
    }
  }

  private def seqBuilder = Seq.newBuilder[String]

  def print(m: LogicalPlan): String = {
    m match {
      case InsertInto(table, aliases, query) =>
        val b = seqBuilder
        b += "INSERT INTO"
        b += printExpression(table)
        aliases.map { x =>
          b += s"(${x.map(printExpression).mkString(", ")})"
        }
        b += print(query)
        b.result().mkString(" ")
      case Delete(table, condOpt) =>
        val b = seqBuilder
        b += "DELETE FROM"
        b += printExpression(table)
        condOpt.map { x =>
          b += "WHERE"
          b += printExpression(x)
        }
        b.result().mkString(" ")
      case r: Relation => printRelation(r)
      case d: DDL      => printDDL(d)
      case other       => unknown(other)
    }
  }

  private def findSelectItems(in: Relation): Option[Seq[SelectItem]] = {
    in match {
      case Project(in, isDistinct, selectItems) =>
        Some(selectItems)
      case Aggregate(in, selectItems, groupingKeys, having) =>
        Some(selectItems)
      case u: UnaryRelation =>
        findSelectItems(u.inputRelation)
      case _ =>
        None
    }
  }
  private def findFromClause(in: Relation): Option[Relation] = {
    // Look for FROM clause candidates inside Project/Aggregate/Filter nodes
    in match {
      case Project(in, _, _) =>
        findFromClause(in)
      case Aggregate(in, _, _, _) =>
        findFromClause(in)
      case Filter(in, filter) =>
        findFromClause(in)
      case EmptyRelation =>
        None
      case other =>
        Some(other)
    }
  }

  private def findWhereClause(in: Relation): Option[Expression] = {
    // We need to terminate traversal at Project/Aggregate node because these will create another SELECT statement.
    in match {
      case Project(_, _, _) =>
        None
      case Aggregate(_, _, _, _) =>
        None
      case Filter(in, filterExpr) =>
        Some(filterExpr)
      case u: UnaryRelation =>
        findWhereClause(u.inputRelation)
      case other =>
        None
    }
  }

  def printRelation(r: Relation): String = {
    r match {
      case EmptyRelation          => ""
      case Filter(in, filterExpr) =>
        // TODO Need to find whether this is a top level SQL node or not
        val b = seqBuilder
        b += "SELECT"
        findSelectItems(in).map { selectItems =>
          b += selectItems.map(printExpression _).mkString(", ")
        }
        findFromClause(in).map { f =>
          b += "FROM"
          b += print(f)
        }
        b += "WHERE"
        b += printExpression(filterExpr)
        b.result().mkString(" ")
      case Project(in, isDistinct, selectItems) =>
        val b = Seq.newBuilder[String]
        b += "SELECT"
        if (isDistinct) {
          b += "DISTINCT"
        }
        b += (selectItems.map(printExpression).mkString(", "))
        findFromClause(in).map { f =>
          b += "FROM"
          b += printRelation(f)
        }
        findWhereClause(in).map { f =>
          b += "WHERE"
          b += printExpression(f)
        }
        b.result().mkString(" ")
      case Aggregate(in, selectItems, groupingKeys, having) =>
        val b = Seq.newBuilder[String]
        b += "SELECT"
        b += (selectItems.map(printExpression).mkString(", "))
        findFromClause(in).map { f =>
          b += "FROM"
          b += printRelation(f)
        }
        findWhereClause(in).map { f =>
          b += "WHERE"
          b += printExpression(f)
        }
        b += s"GROUP BY ${groupingKeys.map(printExpression).mkString(", ")}"
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
      case Except(left, right, distinct) =>
        val op = if (distinct) " EXCEPT " else " EXCEPT ALL "
        val l  = printRelation(left)
        val r  = printRelation(right)
        s"${l} ${op} ${r}"
      case Intersect(relations, isDistinct) =>
        val op = if (isDistinct) " INTERSECT " else " INTERSECT ALL "
        relations.map(printRelation(_)).mkString(op)
      case Table(t) =>
        printExpression(t)
      case Limit(in, l) =>
        s"${printRelation(in)} LIMIT ${l}"
      case Sort(in, orderBy) =>
        val order = orderBy.map(x => printExpression(x)).mkString(", ")
        s"${printRelation(in)} ORDER BY ${order}"
      case ParenthesizedRelation(r) =>
        s"(${printRelation(r)})"
      case AliasedRelation(relation, alias, columnNames) =>
        val r = printRelation(relation)
        val c = columnNames.map(x => s"(${x.mkString(", ")})").getOrElse("")
        relation match {
          case Table(x)                 => s"${r} AS ${alias}${c}"
          case ParenthesizedRelation(x) => s"${r} AS ${alias}${c}"
          case _                        => s"(${r}) AS ${alias}${c}"
        }
      case Join(joinType, left, right, cond) =>
        val l = printRelation(left)
        val r = printRelation(right)
        val c = cond match {
          case NaturalJoin        => ""
          case JoinUsing(columns) => s" USING (${columns.mkString(", ")})"
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
      case Values(exprs) =>
        s"(VALUES ${exprs.map(printExpression _).mkString(", ")})"
      case other => unknown(other)
    }
  }

  def printDDL(e: DDL): String = {
    e match {
      case CreateSchema(name, ifNotExists, propsOpt) =>
        val e = if (ifNotExists) "IF NOT EXISTS " else ""
        val w = propsOpt.map(props => s" WITH (${props.map(printExpression).mkString(", ")})").getOrElse("")
        s"CREATE SCHEMA ${e}${name}${w}"
      case DropSchema(name, ifExists, cascade) =>
        val s = Seq.newBuilder[String]
        s += "DROP SCHEMA"
        if (ifExists) {
          s += "IF EXISTS"
        }
        s += name.toString
        if (cascade) {
          s += "CASCADE"
        }
        s.result().mkString(" ")
      case RenameSchema(from, to) =>
        s"ALTER SCHEMA ${from} RENAME TO ${to}"
      case CreateTable(name, ifNotExists, tableElements) =>
        val e     = if (ifNotExists) " IF NOT EXISTS " else ""
        val elems = tableElements.map(printExpression).mkString(", ")
        s"CREATE TABLE ${e}${name} (${elems})"
      case CreateTableAs(name, ifNotExists, columnAliases, query) =>
        val e = if (ifNotExists) " IF NOT EXISTS " else ""
        val aliases =
          columnAliases
            .map { x =>
              s"(${x.map(printExpression).mkString(", ")})"
            }
            .getOrElse("")
        s"CREATE TABLE ${e}${name}${aliases} AS ${print(query)}"
      case DropTable(table, ifExists) =>
        val b = Seq.newBuilder[String]
        b += "DROP TABLE"
        if (ifExists) {
          b += "IF EXISTS"
        }
        b += printExpression(table)
        b.result().mkString(" ")
      case RenameTable(from, to) =>
        val b = seqBuilder
        b += "ALTER TABLE"
        b += printExpression(from)
        b += "RENAME TO"
        b += printExpression(to)
        b.result().mkString(" ")
      case RenameColumn(table, from, to) =>
        val b = seqBuilder
        b += "ALTER TABLE"
        b += printExpression(table)
        b += "RENAME COLUMN"
        b += printExpression(from)
        b += "TO"
        b += printExpression(to)
        b.result().mkString(" ")
      case DropColumn(table, col) =>
        val b = seqBuilder
        b += "ALTER TABLE"
        b += printExpression(table)
        b += "DROP COLUMN"
        b += printExpression(col)
        b.result().mkString(" ")
      case AddColumn(table, colDef) =>
        val b = seqBuilder
        b += "ALTER TABLE"
        b += printExpression(table)
        b += "ADD COLUMN"
        b += printExpression(colDef)
        b.result().mkString(" ")
      case CreateView(name, replace, query) =>
        val b = seqBuilder
        b += "CREATE"
        if (replace) {
          b += "OR REPLACE"
        }
        b += "VIEW"
        b += printExpression(name)
        b += "AS"
        b += print(query)
        b.result().mkString(" ")
      case DropView(name, ifExists) =>
        val b = seqBuilder
        b += "DROP VIEW"
        if (ifExists) {
          b += "IF EXISTS"
        }
        b += printExpression(name)
        b.result().mkString(" ")
    }
  }

  def printExpression(e: Expression): String = {
    e match {
      case a: Attribute =>
        a.name
      case i: Identifier =>
        i.toString
      case ParenthesizedExpression(expr) =>
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
        val wd = window
          .map { w =>
            val s = Seq.newBuilder[String]

            if (w.partitionBy.nonEmpty) {
              s += "PARTITION BY"
              s += w.partitionBy.map(x => printExpression(x)).mkString(", ")
            }
            if (w.orderBy.nonEmpty) {
              s += "ORDER BY"
              s += w.orderBy.map(x => printExpression(x)).mkString(", ")
            }
            w.frame.map(x => s += x.toString)
            s" OVER (${s.result().mkString(" ")})"
          }
          .getOrElse("")
        s"${name}(${d}${argList})${wd}"
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
      case w: WindowFrame =>
        w.toString
      case SchemaProperty(k, v) =>
        s"${k} = ${v}"
      case ColumnDef(name, tpe) =>
        s"${printExpression(name)} ${printExpression(tpe)}"
      case ColumnType(tpe) =>
        tpe
      case ColumnDefLike(table, includeProperties) =>
        val inc = if (includeProperties) "INCLUDING" else "EXCLUDING"
        s"LIKE ${printExpression(table)} ${inc} PROPERTIES"
      case ArrayConstructor(values) =>
        s"ARRAY[${values.map(printExpression).mkString(", ")}]"
      case Parameter(index) =>
        "?"
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
        s"${printExpression(a)} LIKE ${printExpression(e)}"
      case NotLike(a, e) =>
        s"${printExpression(a)} NOT LIKE ${printExpression(e)}"
      case DistinctFrom(a, e) =>
        s"${printExpression(a)} IS DISTINCT FROM ${printExpression(e)}"
      case NotDistinctFrom(a, e) =>
        s"${printExpression(a)} IS NOT DISTINCT FROM ${printExpression(e)}"
      case other => unknown(other)
    }
  }
}
