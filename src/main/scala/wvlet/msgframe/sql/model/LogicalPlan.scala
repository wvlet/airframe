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
package wvlet.msgframe.sql.model

import java.util.Locale

trait LogicalPlan extends Product {
  def modelName = this.getClass.getSimpleName

  /**
    * All child nodes of this plan node
    * @return
    */
  def children: Seq[LogicalPlan]

  /**
    * Expressions associated to this LogicalPlan node
    * @return
    */
  def expressions: Seq[Expression] = {
    def collectExpression(x: Any): Seq[Expression] = {
      case e: Expression     => e :: Nil
      case Some(x)           => collectExpression(x)
      case s: Traversable[_] => s.flatMap(collectExpression)
      case other             => Nil
    }

    productIterator.flatMap { x =>
      collectExpression(x)
    }.toSeq
  }

  def inputAttributes: Seq[Attribute]
  def outputAttributes: Seq[Attribute]
}

/**
  *
  */
sealed trait Expression

/**
  * Attribute is used for column names of relational table inputs and outputs
  */
trait Attribute extends Expression {
  def name: String
}

/**
  * A trait for LogicalPlan nodes that can generate SQL signatures
  */
trait SQLSig {
  def sig: String
}

trait LeafNode extends LogicalPlan {
  override def children: Seq[LogicalPlan]      = Seq.empty
  override def inputAttributes: Seq[Attribute] = Seq.empty
}

trait UnaryNode extends LogicalPlan {
  def child: LogicalPlan
  override def children = child :: Nil

  override def inputAttributes: Seq[Attribute] = child.inputAttributes
}

trait BinaryNode extends LogicalPlan {
  def left: LogicalPlan
  def right: LogicalPlan
  override def children = Seq(left, right)
}

/**
  *
  */
object LogicalPlan {

  case class ParenthesizedExpression(expr: Expression) extends Expression

  // Qualified name (QName), such as table and column names
  case class QName(parts: Seq[String]) extends Expression {
    override def toString = parts.mkString(".")
  }
  object QName {
    def apply(s: String): QName = {
      // TODO handle quotation
      QName(s.split("\\.").toSeq)
    }
  }

  case class UnresolvedAttribute(parts: Seq[String]) extends Attribute {
    def name = parts.mkString(".")
  }

  sealed trait Identifier extends Expression
  case class DigitId(value: Int) extends Identifier {
    override def toString: String = value.toString
  }
  case class UnquotedIdentifier(value: String) extends Identifier {
    override def toString: String = value
  }
  case class BackQuotedIdentifier(value: String) extends Identifier {
    override def toString = s"`${value}`"
  }
  case class QuotedIdentifier(value: String) extends Identifier {
    override def toString = s""""${value}""""
  }

  // Operator for ign relations
  sealed trait Relation extends LogicalPlan with SQLSig

  sealed trait UnaryRelation extends Relation with UnaryNode {
    def inputRelation: Relation = child
    override def child: Relation
  }

  case class ParenthesizedRelation(child: Relation) extends UnaryRelation {
    override def sig: String                      = child.sig
    override def inputAttributes: Seq[Attribute]  = child.inputAttributes
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }
  case class AliasedRelation(child: Relation, alias: String, columnNames: Option[Seq[String]]) extends UnaryRelation {
    override def sig: String = child.sig

    override def inputAttributes: Seq[Attribute]  = child.inputAttributes
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Values(rows: Seq[Expression]) extends Relation with LeafNode {
    override def sig: String = {
      s"V[${rows.length}]"
    }
    override def outputAttributes: Seq[Attribute] = ???
  }

  case class Table(name: QName) extends Relation with LeafNode {
    override def sig: String                      = "T"
    override def outputAttributes: Seq[Attribute] = Nil
  }
  case class RawSQL(sql: String) extends Relation with LeafNode {
    override def sig: String                      = "Q"
    override def outputAttributes: Seq[Attribute] = Nil
  }

  // Deduplicate the input releation
  case class Distinct(child: Relation) extends UnaryRelation {
    override def sig: String                      = s"DD(${child.sig})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Sort(child: Relation, orderBy: Seq[SortItem]) extends UnaryRelation {
    override def sig: String                      = s"O[${orderBy.length}](${child.sig})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Limit(child: Relation, limit: Int) extends UnaryRelation {
    override def sig: String                      = s"L(${child.sig})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Filter(child: Relation, filterExpr: Expression) extends UnaryRelation {
    override def sig: String                      = s"F(${child.sig})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case object EmptyRelation extends Relation with LeafNode {
    override def sig                              = ""
    override def outputAttributes: Seq[Attribute] = Nil
  }

  private def isSelectAll(selectItems: Seq[SelectItem]): Boolean = {
    selectItems.exists {
      case AllColumns(x) => true
      case _             => false
    }
  }

  case class Project(child: Relation, isDistinct: Boolean, selectItems: Seq[SelectItem]) extends UnaryRelation {
    override def sig: String = {
      val prefix = if (isDistinct) "distinct " else ""
      val proj   = if (isSelectAll(selectItems)) "*" else s"${selectItems.length}"
      s"P[${prefix}${proj}](${child.sig})"
    }

    // TODO
    override def outputAttributes: Seq[Attribute] = ???
  }

  case class Aggregate(child: Relation,
                       selectItems: Seq[SelectItem],
                       groupingKeys: Seq[Expression],
                       having: Option[Expression])
      extends UnaryRelation {

    override def sig: String = {
      val proj = if (isSelectAll(selectItems)) "*" else s"${selectItems.length}"
      s"A[${proj},${groupingKeys.length}](${child.sig})"
    }

    override def toString =
      s"Aggregate[${groupingKeys.mkString(",")}](Select[${selectItems.mkString(", ")}(${child})"

    // TODO
    override def outputAttributes: Seq[Attribute] = ???
  }

  case class Query(withQuery: With, body: Relation) extends Relation {
    override def children: Seq[LogicalPlan] = {
      val b = Seq.newBuilder[LogicalPlan]
      b ++= withQuery.children
      b += body
      b.result()
    }

    override def sig: String = {
      val wq = for (q <- withQuery.queries) yield {
        s"${q.query.sig}"
      }
      val wq_s = wq.mkString(",")

      s"W[${wq_s}](${body.sig})"
    }
  }
  case class With(recursive: Boolean, queries: Seq[WithQuery]) extends LogicalPlan {
    override def children: Seq[LogicalPlan] = queries.flatMap(_.children)
    override def inputAttributes: Seq[Attribute]  = ???
    override def outputAttributes: Seq[Attribute] = ???
  }
  case class WithQuery(name: Identifier, query: Relation, columnNames: Option[Seq[Identifier]])
      extends LogicalPlan
      with UnaryNode {
    override def child: LogicalPlan = query
    override def inputAttributes: Seq[Attribute] = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = query.outputAttributes
  }

  // Joins
  case class Join(joinType: JoinType, left: Relation, right: Relation, cond: JoinCriteria) extends Relation {
    override def children: Seq[LogicalPlan] = Seq(left, right)
    override def sig: String = {
      s"${joinType.symbol}(${left.sig},${right.sig})"
    }
    override def inputAttributes: Seq[Attribute]  = ???
    override def outputAttributes: Seq[Attribute] = ???
  }
  sealed abstract class JoinType(val symbol: String)
  // Exact match (= equi join)
  case object InnerJoin extends JoinType("J")
  // Joins for preserving left table entries
  case object LeftOuterJoin extends JoinType("LJ")
  // Joins for preserving right table entries
  case object RightOuterJoin extends JoinType("RJ")
  // Joins for preserving both table entries
  case object FullOuterJoin extends JoinType("FJ")
  // Cartesian product of two tables
  case object CrossJoin extends JoinType("CJ")
  // From clause contains only table names, and
  // Where clause specifies join criteria
  case object ImplicitJoin extends JoinType("J")

  sealed trait JoinCriteria extends Expression
  case object NaturalJoin   extends JoinCriteria with LeafNode
  case class JoinUsing(columns: Seq[String]) extends JoinCriteria with LeafNode {
    override def toString: String = s"JoinUsing(${columns.mkString(",")})"
  }
  case class JoinOn(expr: Expression) extends JoinCriteria with UnaryNode {
    override def child: LogicalPlan = expr
  }

  sealed trait SelectItem extends Expression
  case class AllColumns(prefix: Option[QName]) extends SelectItem with LeafNode {
    override def toString = s"${prefix.map(x => s"${x}.*").getOrElse("*")}"
  }
  case class SingleColumn(expr: Expression, alias: Option[Expression]) extends SelectItem {
    override def children: Seq[LogicalPlan] = {
      val b = Seq.newBuilder[LogicalPlan]
      b += expr
      alias.map(b += _)
      b.result()
    }
    override def toString = alias.map(a => s"${expr} as ${a}").getOrElse(s"${expr}")
  }

  case class SortItem(sortKey: Expression, ordering: Option[SortOrdering] = None, nullOrdering: Option[NullOrdering])
      extends Expression
      with UnaryNode {
    override def child: LogicalPlan = sortKey
  }

  sealed trait SetOperation extends Relation
  case class Intersect(relations: Seq[Relation], isDistinct: Boolean) extends SetOperation {
    override def children: Seq[LogicalPlan] = relations
    override def sig = {
      val prefix = if (isDistinct) "IX[distinct]" else "IX"
      s"${prefix}(${relations.map(_.sig).mkString(",")})"
    }
  }
  case class Except(left: Relation, right: Relation, isDistinct: Boolean) extends SetOperation with BinaryNode {
    override def sig = {
      val prefix = if (isDistinct) "EX[distinct]" else "EX"
      s"${prefix}(${left.sig},${right.sig})"
    }
  }
  case class Union(relations: Seq[Relation], isDistinct: Boolean) extends SetOperation {
    override def children: Seq[LogicalPlan] = relations
    override def toString = {
      val name = if (isDistinct) "Union" else "UnionAll"
      s"${name}(${relations.mkString(",")})"
    }
    override def sig = {
      val prefix = if (isDistinct) "U[distinct]" else "U"
      val in     = relations.map(_.sig).mkString(",")
      s"${prefix}(${in})"
    }
  }

  // Sort ordering
  sealed trait SortOrdering
  case object Ascending extends SortOrdering {
    override def toString = "ASC"
  }
  case object Descending extends SortOrdering {
    override def toString = "DESC"
  }

  sealed trait NullOrdering
  case object NullIsFirst extends NullOrdering {
    override def toString = "NULLS FIRST"
  }
  case object NullIsLast extends NullOrdering {
    override def toString = "NULLS LAST"
  }

  case object UndefinedOrder extends NullOrdering

  // Window functions
  case class Window(partitionBy: Seq[Expression], orderBy: Seq[SortItem], frame: Option[WindowFrame])
      extends LogicalPlan {
    override def children: Seq[LogicalPlan] = {
      val b = Seq.newBuilder[LogicalPlan]
      b ++= partitionBy
      b ++= orderBy
      frame.map(b += _)
      b.result()
    }
  }

  sealed trait FrameType
  case object RangeFrame extends FrameType {
    override def toString = "RANGE"
  }
  case object RowsFrame extends FrameType {
    override def toString = "ROWS"
  }

  sealed trait FrameBound
  case object UnboundedPreceding extends FrameBound {
    override def toString: String = "UNBOUNDED PRECEDING"
  }
  case object UnboundedFollowing extends FrameBound {
    override def toString: String = "UNBOUNDED FOLLOWING"
  }
  case class Preceding(n: Long) extends FrameBound {
    override def toString: String = s"${n} PRECEDING"
  }

  case class Following(n: Long) extends FrameBound {
    override def toString: String = s"${n} FOLLOWING"
  }
  case object CurrentRow extends FrameBound {
    override def toString: String = "CURRENT ROW"
  }

  case class WindowFrame(frameType: FrameType, start: FrameBound, end: Option[FrameBound])
      extends LogicalPlan
      with LeafNode {
    override def toString: String = {
      val s = Seq.newBuilder[String]
      s += frameType.toString
      if (end.isDefined) {
        s += "BETWEEN"
      }
      s += start.toString
      if (end.isDefined) {
        s += "AND"
        s += end.get.toString
      }
      s.result().mkString(" ")
    }
  }

  // Function
  case class FunctionCall(name: QName,
                          args: Seq[Expression],
                          isDistinct: Boolean,
                          filter: Option[Expression],
                          window: Option[Window])
      extends Expression {
    override def children: Seq[LogicalPlan] = {
      val b = Seq.newBuilder[LogicalPlan]
      b ++= args
      filter.map(b += _)
      window.map(b ++= _.children)
      b.result()
    }

    def functionName: String = name.toString.toLowerCase(Locale.US)
    override def toString    = s"FunctionCall(${name}, ${args.mkString(", ")}, distinct:${isDistinct}, window:${window})"
  }
  case class LambdaExpr(body: Expression, args: Seq[String]) extends Expression with UnaryNode {
    override def child: LogicalPlan = body
  }

  class Ref(name: QName) extends Expression

  // Conditional expression
  sealed trait ConditionalExpression                              extends Expression
  case object NoOp                                                extends ConditionalExpression
  case class Eq(left: Expression, right: Expression)              extends ConditionalExpression
  case class NotEq(left: Expression, right: Expression)           extends ConditionalExpression
  case class And(left: Expression, right: Expression)             extends ConditionalExpression
  case class Or(left: Expression, right: Expression)              extends ConditionalExpression
  case class Not(child: Expression)                               extends ConditionalExpression
  case class LessThan(left: Expression, right: Expression)        extends ConditionalExpression
  case class LessThanOrEq(left: Expression, right: Expression)    extends ConditionalExpression
  case class GreaterThan(left: Expression, right: Expression)     extends ConditionalExpression
  case class GreaterThanOrEq(left: Expression, right: Expression) extends ConditionalExpression
  case class Between(e: Expression, a: Expression, b: Expression) extends ConditionalExpression
  case class IsNull(child: Expression)                            extends ConditionalExpression
  case class IsNotNull(child: Expression)                         extends ConditionalExpression
  case class In(a: Expression, list: Seq[Expression])             extends ConditionalExpression
  case class NotIn(a: Expression, list: Seq[Expression])          extends ConditionalExpression
  case class InSubQuery(a: Expression, in: Relation)              extends ConditionalExpression
  case class NotInSubQuery(a: Expression, in: Relation)           extends ConditionalExpression
  case class Like(left: Expression, right: Expression)            extends ConditionalExpression
  case class NotLike(left: Expression, right: Expression)         extends ConditionalExpression
  case class DistinctFrom(left: Expression, right: Expression)    extends ConditionalExpression
  case class NotDistinctFrom(left: Expression, right: Expression) extends ConditionalExpression

  case class IfExpr(cond: ConditionalExpression, onTrue: Expression, onFalse: Expression) extends Expression
  case class CaseExpr(operand: Option[Expression], whenClauses: Seq[WhenClause], defaultValue: Option[Expression])
      extends Expression
  case class WhenClause(condition: Expression, result: Expression) extends Expression

  case class Exists(child: Expression) extends Expression

  // Arithmetic expr
  abstract sealed class BinaryExprType(val symbol: String)
  case object Add      extends BinaryExprType("+")
  case object Subtract extends BinaryExprType("-")
  case object Multiply extends BinaryExprType("*")
  case object Divide   extends BinaryExprType("/")
  case object Modulus  extends BinaryExprType("%")

  sealed trait ArithmeticExpression extends Expression
  case class ArithmeticBinaryExpr(exprType: BinaryExprType, left: Expression, right: Expression)
      extends ArithmeticExpression
  case class ArithmeticUnaryExpr(sign: Sign, child: Expression) extends ArithmeticExpression

  abstract sealed class Sign(val symbol: String)
  case object Positive extends Sign("+")
  case object Negative extends Sign("-")

  // Set quantifier
  sealed trait SetQuantifier extends Expression {
    def isDistinct: Boolean
  }
  case object All extends SetQuantifier {
    override def isDistinct: Boolean = false
  }
  case object Distinct extends SetQuantifier {
    override def isDistinct: Boolean = true
  }

  // Literal
  sealed trait Literal extends Expression
  case object NullLiteral extends Literal {
    override def toString: String = "NULL"
  }
  sealed trait BooleanLiteral extends Literal
  case object TrueLiteral extends BooleanLiteral {
    override def toString: String = "TRUE"
  }
  case object FalseLiteral extends BooleanLiteral {
    override def toString: String = "FALSE"
  }
  case class StringLiteral(value: String) extends Literal {
    override def toString = s"'${value}'"
  }
  case class TimeLiteral(value: String) extends Literal {
    override def toString = s"TIME '${value}'"
  }
  case class TimestampLiteral(value: String) extends Literal {
    override def toString = s"TIMESTAMP '${value}'"
  }
  case class DecimalLiteral(value: String) extends Literal {
    override def toString = s"DECIMAL '${value}'"
  }
  case class CharLiteral(value: String) extends Literal {
    override def toString = s"CHAR '${value}'"
  }
  case class DoubleLiteral(value: Double) extends Literal {
    override def toString = value.toString
  }
  case class LongLiteral(value: Long) extends Literal {
    override def toString = value.toString
  }
  case class IntervalLiteral(value: String, sign: Sign, startField: IntervalField, end: Option[IntervalField])
      extends Literal {
    override def toString: String = {
      s"INTERVAL ${sign.symbol} '${value}' ${startField}"
    }
  }

  case class GenericLiteral(tpe: String, value: String) extends Literal {
    override def toString = s"${tpe} '${value}'"
  }
  case class BinaryLiteral(binary: String) extends Literal

  sealed trait IntervalField extends Expression
  case object Year           extends IntervalField
  case object Month          extends IntervalField
  case object Day            extends IntervalField
  case object Hour           extends IntervalField
  case object Minute         extends IntervalField
  case object Second         extends IntervalField

  // Value constructor
  case class ArrayConstructor(values: Seq[Expression])                        extends Expression
  abstract sealed class CurrentTimeBase(name: String, precision: Option[Int]) extends Expression
  case class CurrentTime(precision: Option[Int])                              extends CurrentTimeBase("current_time", precision)
  case class CurrentDate(precision: Option[Int])                              extends CurrentTimeBase("current_date", precision)
  case class CurrentTimestamp(precision: Option[Int])                         extends CurrentTimeBase("current_timestamp", precision)
  case class CurrentLocalTime(precision: Option[Int])                         extends CurrentTimeBase("localtime", precision)
  case class CurrentLocalTimeStamp(precision: Option[Int])                    extends CurrentTimeBase("localtimestamp", precision)

  // 1-origin parameter
  case class Parameter(index: Int)               extends Expression
  case class SubQueryExpression(query: Relation) extends Expression

  case class Cast(expr: Expression, tpe: String, tryCast: Boolean = false) extends Expression

  // DDL
  sealed trait DDL extends LogicalPlan with LeafNode with SQLSig {
    override def outputAttributes: Seq[Attribute] = Seq.empty
  }
  case class CreateSchema(schema: QName, ifNotExists: Boolean, properties: Option[Seq[SchemaProperty]])
      extends DDL {
    override def sig                              = "CS"

  }
  case class SchemaProperty(key: Identifier, value: Expression) extends Expression
  case class DropSchema(schema: QName, ifExists: Boolean, cascade: Boolean) extends DDL {
    override def sig                              = "DS"
  }

  case class RenameSchema(schema: QName, renameTo: Identifier) extends DDL {
    override def sig                              = "RS"
  }
  case class CreateTable(table: QName, ifNotExists: Boolean, tableElems: Seq[TableElement]) extends DDL with {
    override def sig                        = "CT"
  }

  case class CreateTableAs(table: QName,
                           ifNotEotExists: Boolean,
                           columnAliases: Option[Seq[Identifier]],
                           query: Relation)
      extends DDL {
    override def sig                        = s"CT(${query.sig})"
    override def inputAttributes: Seq[Attribute] = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = Nil
  }
  case class DropTable(table: QName, ifExists: Boolean) extends DDL {
    override def sig                = "DT"
  }
  trait Update extends LogicalPlan with SQLSig

  case class InsertInto(table: QName, columnAliases: Option[Seq[Identifier]], query: Relation) extends Update with UnaryRelation {
    override def child : Relation = query
    override def sig                        = s"I(${query.sig})"
    override def inputAttributes: Seq[Attribute] = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = Nil
  }
  case class Delete(table: QName, where: Option[Expression]) extends Update with LeafNode {
    override def sig = "D"
    override def inputAttributes: Seq[Attribute]  = Nil
    override def outputAttributes: Seq[Attribute] = Nil
  }

  case class RenameTable(table: QName, renameTo: QName) extends DDL {
    override def sig                        = "RT"
  }
  case class RenameColumn(table: QName, column: Identifier, renameTo: Identifier) extends DDL {
    override def sig                        = "RC"
  }
  case class DropColumn(table: QName, column: Identifier) extends DDL {
    override def sig                        = "DC"
  }
  case class AddColumn(table: QName, column: ColumnDef) extends DDL {
    override def sig                        = "AC"
  }

  sealed trait TableElement extends Expression
  case class ColumnDef(columnName: Identifier, tpe: ColumnType) extends TableElement
  case class ColumnType(tpe: String) extends Expression

  case class ColumnDefLike(tableName: QName, includeProperties: Boolean) extends TableElement
  case class CreateView(viewName: QName, replace: Boolean, query: Relation) extends DDL {
    override def sig                        = "CV"
  }
  case class DropView(viewName: QName, ifExists: Boolean) extends DDL {
    override def sig   = "DV"
  }
}

object SQLFunction {

  //case class AggregateExpression(func: AggregateFunction, mode: AggregateMode, isDistinct: Boolean) extends Expression

  sealed trait AggregateMode
  case object PartialAggregate extends AggregateMode
  case object FinalAggregate   extends AggregateMode
  case object FullAggregate    extends AggregateMode

  sealed trait AggregateFunction
  case class Sum(e: Expression)                      extends AggregateFunction
  case class Avg(e: Expression)                      extends AggregateFunction
  case class Count(e: Expression)                    extends AggregateFunction
  case class CountDistinct(e: Expression)            extends AggregateFunction
  case class CountMinSketch(e: Expression)           extends AggregateFunction
  case class CountApproximateDistinct(e: Expression) extends AggregateFunction
  case class ApproximatePercentile(e: Expression)    extends AggregateFunction
  case class Min(e: Expression)                      extends AggregateFunction
  case class Max(e: Expression)                      extends AggregateFunction
  case class First(e: Expression)                    extends AggregateFunction
  case class Last(e: Expression)                     extends AggregateFunction

}
