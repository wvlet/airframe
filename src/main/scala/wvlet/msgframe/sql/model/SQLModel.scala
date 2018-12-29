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

import wvlet.airframe.surface.Surface
import wvlet.msgframe.sql.model.SQLModel.Expression

object SQLSchema {
  sealed trait Schema
  case class AnonSchema(columns: Seq[Column])                extends Schema
  case class TableSchema(name: String, columns: Seq[Column]) extends Schema

  case class Column(name: String, columnType: DataType)

  sealed trait DataType
  case object IntegerType                                    extends DataType
  case object FloatType                                      extends DataType
  case object BooleanType                                    extends DataType
  case object StringType                                     extends DataType
  case object JSONType                                       extends DataType
  case object BinaryType                                     extends DataType
  case object NullType                                       extends DataType
  case object TimestampType                                  extends DataType
  case class ObjectType(surface: Surface)                    extends DataType
  case class AraryType(elemType: DataType)                   extends DataType
  case class MapType(keyType: DataType, valueType: DataType) extends DataType
  case object AnyType                                        extends DataType
}

trait SQLModel {
  def children: Seq[SQLModel]
}

/**
  *
  */
object SQLModel {

  trait Leaf extends SQLModel {
    override def children: Seq[SQLModel] = Seq.empty
  }

  trait UnaryNode extends SQLModel {

    override def children
  }

  sealed trait Expression extends SQLModel

  case class ParenthizedExpression(expr: Expression) extends Expression

  // Qualified name (QName), such as table and column names
  case class QName(parts: Seq[String]) extends Expression {
    override def toString = parts.mkString(".")
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

  object QName {
    def apply(s: String): QName = {
      // TODO handle quotation
      QName(s.split("\\.").toSeq)
    }
  }

  // Operator for ign relations
  sealed trait Relation                                                                           extends SQLModel
  case class ParenthizedRelation(relation: Relation)                                              extends Relation
  case class AliasedRelation(relation: Relation, alias: String, columnNames: Option[Seq[String]]) extends Relation
  case class Values(rows: Seq[Expression])                                                        extends Relation
  case class Table(name: QName)                                                                   extends Relation
  case class RawSQL(sql: String)                                                                  extends Relation
  //case class Filter(in: Relation, filterExpr: Expression)                                         extends Relation
  case class Sort(in: Relation, orderBy: Seq[SortItem]) extends Relation
  case class Limit(in: Relation, limit: Int)            extends Relation
  case class Select(isDistinct: Boolean = false,
                    selectItems: Seq[SelectItem],
                    in: Option[Relation],
                    whereExpr: Option[Expression])
      extends Relation {
    override def toString =
      s"Select[${selectItems.mkString(",")}](${in.getOrElse("None")},distinct:${isDistinct},where:${whereExpr})"
  }
  case class Aggregate(selectItems: Seq[SelectItem],
                       in: Option[Relation],
                       whereExpr: Option[Expression],
                       groupingKeys: Seq[Expression],
                       having: Option[Expression])
      extends Relation {
    override def toString =
      s"Aggregate[${groupingKeys.mkString(",")}](Select[${selectItems.mkString(", ")}(${in},where:${whereExpr}))"
  }

  case class Query(withQuery: With, body: Relation) extends Relation

  case class WithQuery(name: Identifier, query: Relation, columnNames: Option[Seq[Identifier]]) extends SQLModel
  case class With(recursive: Boolean, queries: Seq[WithQuery])                                  extends SQLModel

  // Joins
  case class Join(joinType: JoinType, left: Relation, right: Relation, cond: JoinCriteria) extends Relation
  sealed trait JoinType
  // Exact match (= equi join)
  case object InnerJoin extends JoinType
  // Joins for preserving left table entries
  case object LeftOuterJoin extends JoinType
  // Joins for preserving right table entries
  case object RightOuterJoin extends JoinType
  // Joins for preserving both table entries
  case object FullOuterJoin extends JoinType
  // Cartesian product of two tables
  case object CrossJoin extends JoinType
  // From clause contains only table names, and
  // Where clause specifies join criteria
  case object ImplicitJoin extends JoinType

  sealed trait JoinCriteria
  case object NaturalJoin extends JoinCriteria
  case class JoinUsing(columns: Seq[String]) extends JoinCriteria {
    override def toString: String = s"JoinUsing(${columns.mkString(",")})"
  }
  case class JoinOn(expr: Expression) extends JoinCriteria

  sealed trait SelectItem extends Expression
  case class AllColumns(prefix: Option[QName]) extends SelectItem {
    override def toString = s"${prefix.map(x => s"${x}.*").getOrElse("*")}"
  }
  case class SingleColumn(expr: Expression, alias: Option[Expression]) extends SelectItem {
    override def toString = alias.map(a => s"${expr} as ${a}").getOrElse(s"${expr}")
  }

  case class SortItem(sortKey: Expression, ordering: Option[SortOrdering] = None, nullOrdering: Option[NullOrdering])
      extends Expression

  sealed trait SetOperation                                               extends Relation
  case class Intersect(relations: Seq[Relation], isDistinct: Boolean)     extends SetOperation
  case class Except(left: Relation, right: Relation, isDistinct: Boolean) extends SetOperation
  case class Union(relations: Seq[Relation], isDistinct: Boolean) extends SetOperation {
    override def toString = {
      val name = if (isDistinct) "Union" else "UnionAll"
      s"${name}(${relations.mkString(",")})"
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
  case object NullIsFirst    extends NullOrdering
  case object NullIsLast     extends NullOrdering
  case object UndefinedOrder extends NullOrdering

  // Window functions
  case class Window(partitionBy: Seq[Expression], orderBy: Seq[SortItem], frame: Option[WindowFrame]) extends SQLModel

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

  case class WindowFrame(frameType: FrameType, start: FrameBound, end: Option[FrameBound]) extends SQLModel {
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
    def functionName: String = name.toString.toLowerCase(Locale.US)
    override def toString    = s"FunctionCall(${name}, ${args.mkString(", ")}, distinct:${isDistinct}, window:${window})"
  }
  case class LambdaExpr(body: Expression, args: Seq[String]) extends Expression

  class Ref(name: QName) extends Expression

  // Conditional expression
  sealed trait ConditionalExpression                              extends Expression
  case object NoOp                                                extends ConditionalExpression
  case class Eq(a: Expression, b: Expression)                     extends ConditionalExpression
  case class NotEq(a: Expression, b: Expression)                  extends ConditionalExpression
  case class And(a: Expression, b: Expression)                    extends ConditionalExpression
  case class Or(a: Expression, b: Expression)                     extends ConditionalExpression
  case class Not(expr: Expression)                                extends ConditionalExpression
  case class LessThan(a: Expression, b: Expression)               extends ConditionalExpression
  case class LessThanOrEq(a: Expression, b: Expression)           extends ConditionalExpression
  case class GreaterThan(a: Expression, b: Expression)            extends ConditionalExpression
  case class GreaterThanOrEq(a: Expression, b: Expression)        extends ConditionalExpression
  case class Between(e: Expression, a: Expression, b: Expression) extends ConditionalExpression
  case class IsNull(a: Expression)                                extends ConditionalExpression
  case class IsNotNull(a: Expression)                             extends ConditionalExpression
  case class In(a: Expression, list: Seq[Expression])             extends ConditionalExpression
  case class NotIn(a: Expression, list: Seq[Expression])          extends ConditionalExpression
  case class InSubQuery(a: Expression, in: Relation)              extends ConditionalExpression
  case class NotInSubQuery(a: Expression, in: Relation)           extends ConditionalExpression
  case class Like(a: Expression, e: Expression)                   extends ConditionalExpression
  case class NotLike(a: Expression, e: Expression)                extends ConditionalExpression
  case class DistinctFrom(a: Expression, e: Expression)           extends ConditionalExpression
  case class NotDistinctFrom(a: Expression, e: Expression)        extends ConditionalExpression

  case class IfExpr(cond: ConditionalExpression, onTrue: Expression, onFalse: Expression) extends Expression
  case class CaseExpr(operand: Option[Expression], whenClauses: Seq[WhenClause], defaultValue: Option[Expression])
      extends Expression
  case class WhenClause(condition: Expression, result: Expression) extends Expression

  case class Exists(subQuery: Expression) extends Expression

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
  case class ArithmeticUnaryExpr(sign: Sign, value: Expression) extends ArithmeticExpression

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
    override def toString = value
  }
  case class CharLiteral(value: String) extends Literal {
    override def toString = value
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

  sealed trait IntervalField extends SQLModel
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
