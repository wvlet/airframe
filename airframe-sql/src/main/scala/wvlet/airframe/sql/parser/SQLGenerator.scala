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
import wvlet.airframe.sql.model._
import wvlet.airframe.sql.model.LogicalPlan._

/**
  * Print LogicalPlans As SQL statements
  */
object SQLGenerator extends LogSupport {
  import Expression._

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
        aliases.map { x => b += s"(${x.map(printExpression).mkString(", ")})" }
        b += printRelation(query)
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

  private def findNonEmpty(in: Relation): Option[Relation] = {
    // Look for FROM clause candidates inside Project/Aggregate/Filter nodes
    in match {
      case EmptyRelation => None
      case other         => Some(other)
    }
  }

  private def collectFilterExpression(stack: List[Relation]): Seq[Expression] = {
    // We need to terminate traversal at Project/Aggregate node because these will create another SELECT statement.
    stack.reverse.collect { case f @ Filter(in, filterExpr) =>
      filterExpr
    }
  }

  private def printSetOperation(s: SetOperation, context: List[Relation]): String = {
    val isDistinct = containsDistinctPlan(context)
    val op = s match {
      case Union(relations) =>
        if (isDistinct) "UNION" else "UNION ALL"
      case Except(left, right) =>
        if (isDistinct) "EXCEPT" else "EXCEPT ALL"
      case Intersect(relations) =>
        if (isDistinct) "INTERSECT" else "INTERSECT ALL"
    }
    s.children.map(printRelation).mkString(s" ${op} ")
  }

  private def containsDistinctPlan(context: List[Relation]): Boolean = {
    context.exists {
      case e: Distinct => true
      case _           => false
    }
  }

  private def collectChildFilters(r: Relation): List[Filter] = {
    r match {
      case f @ Filter(in, _) =>
        f :: collectChildFilters(in)
      case other =>
        Nil
    }
  }

  private def printSelection(s: Selection, context: List[Relation]): String = {
    // We need to pull-up Filter operators from child relations to build WHERE clause
    // e.g., Selection(in:Filter(Filter( ...)), ...)

    val childFilters: List[Filter] = collectChildFilters(s.child)
    val nonFilterChild = if (childFilters.nonEmpty) {
      childFilters.last.child
    } else {
      s.child
    }

    val b = Seq.newBuilder[String]
    b += "SELECT"
    if (containsDistinctPlan(context)) {
      b += "DISTINCT"
    }
    b += (s.selectItems.map(printExpression).mkString(", "))

    findNonEmpty(nonFilterChild).map { f =>
      b += "FROM"
      b += printRelation(f)
    }

    val filterSet = s match {
      case Project(_, _) =>
        // Merge parent and child Filters
        collectFilterExpression(context) ++ collectFilterExpression(childFilters)
      case Aggregate(_, _, _, _) =>
        // We cannot push down parent Filters
        collectFilterExpression(childFilters)
    }
    if (filterSet.nonEmpty) {
      b += "WHERE"
      val cond = filterSet.reduce((f1, f2) => And(f1, f2))
      b += printExpression(cond)
    }

    s match {
      case Aggregate(_, _, groupingKeys, having) =>
        b += s"GROUP BY ${groupingKeys.map(printExpression).mkString(", ")}"
        having.map { h =>
          b += "HAVING"
          b += printExpression(h)
        }
      case _ =>
    }
    b.result().mkString(" ")
  }

  def printRelation(r: Relation): String = printRelation(r, List.empty)

  def printRelation(r: Relation, context: List[Relation] = List.empty): String = {
    r match {
      case s: SetOperation =>
        // Need to pass the context to disginguish union/union all, etc.
        printSetOperation(s, context)
      case Filter(in, filterExpr) =>
        printRelation(in, r :: context)
      case Distinct(in) =>
        printRelation(in, r :: context)
      case p @ Project(in, selectItems) =>
        printSelection(p, context)
      case a @ Aggregate(in, selectItems, groupingKeys, having) =>
        printSelection(a, context)
      case Query(withQuery, body) =>
        val s = seqBuilder
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
      case TableRef(t) =>
        printExpression(t)
      case Limit(in, l) =>
        val s = seqBuilder
        s += printRelation(in, context)
        s += s"LIMIT ${l.sqlExpr}"
        s.result().mkString(" ")
      case Sort(in, orderBy) =>
        val s = seqBuilder
        s += printRelation(in, context)
        s += "ORDER BY"
        s += orderBy.map(x => printExpression(x)).mkString(", ")
        s.result().mkString(" ")
      case ParenthesizedRelation(r) =>
        s"(${printRelation(r, context)})"
      case AliasedRelation(relation, alias, columnNames) =>
        val r = printRelation(relation, context)
        val c = columnNames.map(x => s"(${x.mkString(", ")})").getOrElse("")
        relation match {
          case TableRef(x)              => s"${r} AS ${alias.sqlExpr}${c}"
          case ParenthesizedRelation(x) => s"${r} AS ${alias.sqlExpr}${c}"
          case Unnest(_, _)             => s"${r} AS ${alias.sqlExpr}${c}"
          case Lateral(_)               => s"${r} AS ${alias.sqlExpr}${c}"
          case _                        => s"(${r}) AS ${alias.sqlExpr}${c}"
        }
      case Join(joinType, left, right, cond) =>
        val l = printRelation(left)
        val r = printRelation(right)
        val c = cond match {
          case NaturalJoin        => ""
          case JoinUsing(columns) => s" USING (${columns.map(_.sqlExpr).mkString(", ")})"
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
      case Unnest(cols, ord) =>
        val b = seqBuilder
        b += s"UNNEST (${cols.map(printExpression).mkString(", ")})"
        if (ord) {
          b += "WITH ORDINALITY"
        }
        b.result().mkString(" ")
      case Lateral(q) =>
        val b = seqBuilder
        b += "LATERAL"
        b += s"(${printRelation(q)})"
        b.result().mkString(" ")
      case LateralView(in, exprs, tableAlias, columnAliases) =>
        val b = seqBuilder
        b += printRelation(in)
        b += "LATERAL VIEW explode ("
        b += exprs.map(printExpression).mkString(", ")
        b += ")"
        b += printExpression(tableAlias)
        b += "AS"
        b += columnAliases.map(printExpression).mkString(", ")
        b.result().mkString(" ")
      case other => unknown(other)
    }
  }

  def printDDL(e: DDL): String = {
    e match {
      case CreateSchema(name, ifNotExists, propsOpt) =>
        val e = if (ifNotExists) "IF NOT EXISTS " else ""
        val w = propsOpt.map(props => s" WITH (${props.map(printExpression).mkString(", ")})").getOrElse("")
        s"CREATE SCHEMA ${e}${name.sqlExpr}${w}"
      case DropSchema(name, ifExists, cascade) =>
        val s = Seq.newBuilder[String]
        s += "DROP SCHEMA"
        if (ifExists) {
          s += "IF EXISTS"
        }
        s += name.sqlExpr
        if (cascade) {
          s += "CASCADE"
        }
        s.result().mkString(" ")
      case RenameSchema(from, to) =>
        s"ALTER SCHEMA ${from.sqlExpr} RENAME TO ${to.sqlExpr}"
      case CreateTable(name, ifNotExists, tableElements) =>
        val e     = if (ifNotExists) " IF NOT EXISTS " else ""
        val elems = tableElements.map(printExpression).mkString(", ")
        s"CREATE TABLE ${e}${name} (${elems})"
      case CreateTableAs(name, ifNotExists, columnAliases, query) =>
        val e = if (ifNotExists) " IF NOT EXISTS " else ""
        val aliases =
          columnAliases
            .map { x => s"(${x.map(printExpression).mkString(", ")})" }.getOrElse("")
        s"CREATE TABLE ${e}${name.sqlExpr}${aliases} AS ${print(query)}"
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
      case i: Identifier =>
        i.sqlExpr
      case l: Literal =>
        l.sqlExpr
      case GroupingKey(k) =>
        printExpression(k)
      case ParenthesizedExpression(expr) =>
        s"(${printExpression(expr)})"
      case SingleColumn(ex, alias) =>
        val col = printExpression(ex)
        alias
          .map(x => s"${col} AS ${printExpression(x)}")
          .getOrElse(col)
      case AllColumns(prefix) =>
        prefix.map(p => s"${p}.*").getOrElse("*")
      case a: Attribute =>
        a.name
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
        s"${k.sqlExpr} = ${v.sqlExpr}"
      case ColumnDef(name, tpe) =>
        s"${printExpression(name)} ${printExpression(tpe)}"
      case ColumnType(tpe) =>
        tpe
      case ColumnDefLike(table, includeProperties) =>
        val inc = if (includeProperties) "INCLUDING" else "EXCLUDING"
        s"LIKE ${printExpression(table)} ${inc} PROPERTIES"
      case ArrayConstructor(values) =>
        s"ARRAY[${values.map(printExpression).mkString(", ")}]"
      case RowConstructor(values) =>
        s"(${values.map(printExpression).mkString(", ")})"
      case Parameter(index) =>
        "?"
      case other => unknown(other)
    }
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
