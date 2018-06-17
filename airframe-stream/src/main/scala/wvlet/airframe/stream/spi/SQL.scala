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
package wvlet.airframe.stream.spi

import java.util.Locale

import wvlet.surface.Surface

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
}

/**
  *
  */
object SQL {
  sealed trait Expression

  // Qualified name (QName), such as table and column names
  case class QName(parts: Seq[String]) extends Expression {
    override def toString = parts.mkString(".")
  }

  // Operator for ign relations
  sealed trait Relation
  case class AliasedRelation(relation: Relation, alias: String, columnNames: Option[Seq[String]]) extends Relation
  case class Values(rows: Seq[Expression])                                                        extends Relation
  case class Table(name: QName)                                                                   extends Relation
  case class SubQuery(query: Query)                                                               extends Relation
  case class RawSQL(sql: String)                                                                  extends Relation
  case class Project(in: Relation, schema: Seq[Expression])                                       extends Relation

  // Joins
  case class Join(joinType: JoinType, left: Relation, right: Relation, cond: JoinCriteria) extends Relation
  sealed trait JoinType
  // Exact match (= equi join)
  case object InnerJoin extends JoinType
  // Joins for preserving left table entries
  case object LeftOuterJoin extends JoinType
  // Joins for preserving right table entries
  case object RightOuterJoin extends JoinType
  // Joins for preserbing both table entries
  case object FullOuterJoin extends JoinType
  // Cartesian product of two tables
  case object CrossJoin extends JoinType

  sealed trait JoinCriteria
  case object NaturalJoin                    extends JoinCriteria
  case class JoinUsing(columns: Seq[String]) extends JoinCriteria
  case class JoinOn(expr: Expression)        extends JoinCriteria

  case class Aggregate(in: Relation, keys: Seq[Expression], aggregate: Seq[AggregateExpression]) extends Relation
  case class Query(item: Seq[SelectItem],
                   isDistinct: Boolean,
                   from: Option[Relation],
                   where: Option[Expression],
                   groupBy: Seq[Expression],
                   having: Option[Expression],
                   orderBy: Seq[SortItem],
                   limit: Option[String])
      extends Relation

  sealed trait SelectItem
  case class AllColumns(prefix: Option[QName]) extends SelectItem {
    override def toString = s"${prefix.map(x => s"${x}.*").getOrElse("*")}"
  }
  case class SingleColumn(expr: Expression, alias: Option[String]) extends SelectItem {
    override def toString = alias.map(a => s"${expr} as ${a}").getOrElse(s"${expr}")
  }

  case class SortItem(sortKey: Expression,
                      ordering: SortOrdering = Ascending,
                      nullOrdering: NullOrdering = UndefinedOrder)

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
  case object Ascending  extends SortOrdering
  case object Descending extends SortOrdering

  sealed trait NullOrdering
  case object NullIsFirst    extends NullOrdering
  case object NullIsLast     extends NullOrdering
  case object UndefinedOrder extends NullOrdering

  // Window functions
  case class Window(partitionBy: Seq[Expression], orderBy: Seq[SortItem], frame: Option[WindowFrame])

  sealed trait FrameType
  case object RangeFrame extends FrameType
  case object RowsFrame  extends FrameType

  sealed trait FrameBound
  case object UnboundedPreceding extends FrameBound
  case object Preceding          extends FrameBound
  case object CurrentRow         extends FrameBound
  case object Following          extends FrameBound
  case object UnboundedFollowing extends FrameBound

  case class WindowFrame(frameType: FrameType, start: FrameBound, end: Option[FrameBound])

  // Function
  case class FunctionCall(name: QName, args: Seq[Expression], isDistinct: Boolean, window: Option[Window])
      extends Expression {
    def functionName: String = name.toString.toLowerCase(Locale.US)
    override def toString    = s"FunctionCall(${name}, ${args.mkString(", ")}, distinct:${isDistinct}, window:${window})"
  }
  case class LambdaExpr(body: Expression, args: Seq[String]) extends Expression

  class Ref(name: QName) extends Expression

  // Conditional expression
  sealed trait ConditionalExpression                       extends Expression
  case object NoOp                                         extends ConditionalExpression
  case class Eq(a: Expression, b: Expression)              extends ConditionalExpression
  case class And(a: Expression, b: Expression)             extends ConditionalExpression
  case class Or(a: Expression, b: Expression)              extends ConditionalExpression
  case class Not(expr: Expression)                         extends ConditionalExpression
  case class LessThan(a: Expression, b: Expression)        extends ConditionalExpression
  case class LessThanOrEq(a: Expression, b: Expression)    extends ConditionalExpression
  case class GreaterThan(a: Expression, b: Expression)     extends ConditionalExpression
  case class GreaterThanOrEq(a: Expression, b: Expression) extends ConditionalExpression
  case class Between(a: Expression, b: Expression)         extends ConditionalExpression
  case class IsNull(a: Expression)                         extends ConditionalExpression
  case class IsNotNull(a: Expression)                      extends ConditionalExpression
  case class In(a: Expression, list: Seq[Expression])      extends ConditionalExpression

  case class IfExpr(cond: ConditionalExpression, onTrue: Expression, onFalse: Expression) extends Expression
  case class CaseExpr(operand: Option[Expression], whenClauses: Seq[Expression], defaultValue: Option[Expression])
      extends Expression
  case class WhenClause(operand: Expression, result: Expression) extends Expression

  sealed trait ArithmeticExpression
  // Arithmetic expr
  abstract sealed class BinaryExprType(symbol: String)
  case object Add      extends BinaryExprType("+")
  case object Subtract extends BinaryExprType("-")
  case object Multiply extends BinaryExprType("*")
  case object Divide   extends BinaryExprType("/")
  case object Modulus  extends BinaryExprType("%")

  case class ArithmeticBinaryExpr(exprType: BinaryExprType, left: Expression, right: Expression) extends Expression
  case class ArithmeticUnaryExpr(sign: Sign, value: Expression)                                  extends Expression

  abstract sealed class Sign(symbol: String)
  case object Positive extends Sign("+")
  case object Negative extends Sign("-")

  case class AggregateExpression(func: AggregateFunction, mode: AggregateMode, isDistinct: Boolean) extends Expression

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

  // Literal
  sealed trait Literal        extends Expression
  case object NullLiteral     extends Literal
  sealed trait BooleanLiteral extends Literal
  case object TrueLiteral     extends BooleanLiteral
  case object FalseLiteral    extends BooleanLiteral
  case class StringLiteral(value: String) extends Literal {
    override def toString = s"'${value}'"
  }
  case class TimeLiteral(value: String) extends Literal {
    override def toString = s"time '${value}'"
  }
  case class TimestampLiteral(value: String) extends Literal {
    override def toString = s"timestamp '${value}'"
  }
  case class DoubleLiteral(value: Double) extends Literal {
    override def toString = value.toString
  }
  case class LongLiteral(value: Long) extends Literal {
    override def toString = value.toString
  }
  case class IntervalLiteral(value: String, sign: Sign, startField: IntervalField, end: Option[IntervalField])
      extends Literal

  sealed trait IntervalField
  case object Year   extends IntervalField
  case object Month  extends IntervalField
  case object Day    extends IntervalField
  case object Hour   extends IntervalField
  case object Minute extends IntervalField
  case object Second extends IntervalField

  // Value constructor
  case class ArrayConstructor(values: Seq[Expression])                        extends Expression
  abstract sealed class CurrentTimeBase(name: String, precision: Option[Int]) extends Expression
  case class CurrentTime(precision: Option[Int])                              extends CurrentTimeBase("current_time", precision)
  case class CurrentDate(precision: Option[Int])                              extends CurrentTimeBase("current_date", precision)
  case class CurrentTimestamp(precision: Option[Int])                         extends CurrentTimeBase("current_timestamp", precision)
  case class CurrentLocalTime(precision: Option[Int])                         extends CurrentTimeBase("localtime", precision)
  case class CurrentLocalTimeStamp(precision: Option[Int])                    extends CurrentTimeBase("localtimestamp", precision)

}
