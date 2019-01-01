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

import wvlet.msgframe.sql.model.LogicalPlan.Relation

/**
  *
  */
sealed trait Expression extends TreeNode[Expression]

trait LeafExpression extends Expression {
  override def children: Seq[Expression] = Nil
}
trait UnaryExpression extends Expression {
  def child: Expression
  override def children: Seq[Expression] = Seq(child)
}
trait BinaryExpression extends Expression {
  def left: Expression
  def right: Expression
  override def children: Seq[Expression] = Seq(left, right)
}

/**
  * Attribute is used for column names of relational table inputs and outputs
  */
trait Attribute extends LeafExpression {
  def name: String
}

case class ParenthesizedExpression(child: Expression) extends UnaryExpression

// Qualified name (QName), such as table and column names
case class QName(parts: Seq[String]) extends LeafExpression {
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

sealed trait Identifier extends LeafExpression
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

sealed trait JoinCriteria extends Expression
case object NaturalJoin   extends JoinCriteria with LeafExpression
case class JoinUsing(columns: Seq[Identifier]) extends JoinCriteria {
  override def children: Seq[Expression] = columns
  override def toString: String          = s"JoinUsing(${columns.mkString(",")})"
}
case class JoinOn(expr: Expression) extends JoinCriteria with UnaryExpression {
  override def child: Expression = expr
}

sealed trait SelectItem extends Expression
case class AllColumns(prefix: Option[QName]) extends SelectItem {
  override def children: Seq[Expression] = prefix.toSeq
  override def toString                  = s"${prefix.map(x => s"${x}.*").getOrElse("*")}"
}
case class SingleColumn(expr: Expression, alias: Option[Expression]) extends SelectItem {
  override def children: Seq[Expression] = Seq(expr) ++ alias.toSeq
  override def toString                  = alias.map(a => s"${expr} as ${a}").getOrElse(s"${expr}")
}

case class SortItem(sortKey: Expression, ordering: Option[SortOrdering] = None, nullOrdering: Option[NullOrdering])
    extends Expression
    with UnaryExpression {
  override def child: Expression = sortKey
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
case class Window(partitionBy: Seq[Expression], orderBy: Seq[SortItem], frame: Option[WindowFrame]) extends Expression {
  override def children: Seq[Expression] = partitionBy ++ orderBy ++ frame.toSeq
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
    extends Expression
    with LeafExpression {
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
  override def children: Seq[Expression] = args ++ filter.toSeq ++ window.toSeq
  def functionName: String               = name.toString.toLowerCase(Locale.US)
  override def toString                  = s"FunctionCall(${name}, ${args.mkString(", ")}, distinct:${isDistinct}, window:${window})"
}
case class LambdaExpr(body: Expression, args: Seq[String]) extends Expression with UnaryExpression {
  def child = body
}

class Ref(name: QName) extends Expression with LeafExpression

// Conditional expression
sealed trait ConditionalExpression                              extends Expression
case object NoOp                                                extends ConditionalExpression with LeafExpression
case class Eq(left: Expression, right: Expression)              extends ConditionalExpression with BinaryExpression
case class NotEq(left: Expression, right: Expression)           extends ConditionalExpression with BinaryExpression
case class And(left: Expression, right: Expression)             extends ConditionalExpression with BinaryExpression
case class Or(left: Expression, right: Expression)              extends ConditionalExpression with BinaryExpression
case class Not(child: Expression)                               extends ConditionalExpression with UnaryExpression
case class LessThan(left: Expression, right: Expression)        extends ConditionalExpression with BinaryExpression
case class LessThanOrEq(left: Expression, right: Expression)    extends ConditionalExpression with BinaryExpression
case class GreaterThan(left: Expression, right: Expression)     extends ConditionalExpression with BinaryExpression
case class GreaterThanOrEq(left: Expression, right: Expression) extends ConditionalExpression with BinaryExpression
case class Between(e: Expression, a: Expression, b: Expression) extends ConditionalExpression {
  override def children: Seq[Expression] = Seq(e, a, b)
}
case class IsNull(child: Expression)    extends ConditionalExpression with UnaryExpression
case class IsNotNull(child: Expression) extends ConditionalExpression with UnaryExpression
case class In(a: Expression, list: Seq[Expression]) extends ConditionalExpression {
  override def children: Seq[Expression] = Seq(a) ++ list
}
case class NotIn(a: Expression, list: Seq[Expression]) extends ConditionalExpression {
  override def children: Seq[Expression] = Seq(a) ++ list
}
case class InSubQuery(a: Expression, in: Relation) extends ConditionalExpression {
  override def children: Seq[Expression] = Seq(a) ++ in.expressions
}
case class NotInSubQuery(a: Expression, in: Relation) extends ConditionalExpression {
  override def children: Seq[Expression] = Seq(a) ++ in.expressions
}
case class Like(left: Expression, right: Expression)            extends ConditionalExpression with BinaryExpression
case class NotLike(left: Expression, right: Expression)         extends ConditionalExpression with BinaryExpression
case class DistinctFrom(left: Expression, right: Expression)    extends ConditionalExpression with BinaryExpression
case class NotDistinctFrom(left: Expression, right: Expression) extends ConditionalExpression with BinaryExpression

case class IfExpr(cond: ConditionalExpression, onTrue: Expression, onFalse: Expression) extends Expression {
  override def children: Seq[Expression] = Seq(cond, onTrue, onFalse)
}
case class CaseExpr(operand: Option[Expression], whenClauses: Seq[WhenClause], defaultValue: Option[Expression])
    extends Expression {
  override def children: Seq[Expression] = {
    val b = Seq.newBuilder[Expression]
    operand.foreach(b += _)
    b ++= whenClauses
    defaultValue.foreach(b += _)
    b.result()
  }
}
case class WhenClause(condition: Expression, result: Expression) extends Expression {
  override def children: Seq[Expression] = Seq(condition, result)
}

case class Exists(child: Expression) extends Expression with UnaryExpression

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
    with BinaryExpression
case class ArithmeticUnaryExpr(sign: Sign, child: Expression) extends ArithmeticExpression with UnaryExpression

abstract sealed class Sign(val symbol: String)
case object Positive extends Sign("+")
case object Negative extends Sign("-")

// Set quantifier
sealed trait SetQuantifier extends LeafExpression {
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
case object NullLiteral extends Literal with LeafExpression {
  override def toString: String = "NULL"
}
sealed trait BooleanLiteral extends Literal
case object TrueLiteral extends BooleanLiteral with LeafExpression {
  override def toString: String = "TRUE"
}
case object FalseLiteral extends BooleanLiteral with LeafExpression {
  override def toString: String = "FALSE"
}
case class StringLiteral(value: String) extends Literal with LeafExpression {
  override def toString = s"'${value}'"
}
case class TimeLiteral(value: String) extends Literal with LeafExpression {
  override def toString = s"TIME '${value}'"
}
case class TimestampLiteral(value: String) extends Literal with LeafExpression {
  override def toString = s"TIMESTAMP '${value}'"
}
case class DecimalLiteral(value: String) extends Literal with LeafExpression {
  override def toString = s"DECIMAL '${value}'"
}
case class CharLiteral(value: String) extends Literal with LeafExpression {
  override def toString = s"CHAR '${value}'"
}
case class DoubleLiteral(value: Double) extends Literal with LeafExpression {
  override def toString = value.toString
}
case class LongLiteral(value: Long) extends Literal with LeafExpression {
  override def toString = value.toString
}
case class IntervalLiteral(value: String, sign: Sign, startField: IntervalField, end: Option[IntervalField])
    extends Literal {
  override def children: Seq[Expression] = Seq(startField) ++ end.toSeq

  override def toString: String = {
    s"INTERVAL ${sign.symbol} '${value}' ${startField}"
  }
}

case class GenericLiteral(tpe: String, value: String) extends Literal with LeafExpression {
  override def toString = s"${tpe} '${value}'"
}
case class BinaryLiteral(binary: String) extends Literal with LeafExpression

sealed trait IntervalField extends LeafExpression
case object Year           extends IntervalField
case object Month          extends IntervalField
case object Day            extends IntervalField
case object Hour           extends IntervalField
case object Minute         extends IntervalField
case object Second         extends IntervalField

// Value constructor
case class ArrayConstructor(values: Seq[Expression]) extends Expression {
  override def children: Seq[Expression] = values
}

abstract sealed class CurrentTimeBase(name: String, precision: Option[Int]) extends LeafExpression
case class CurrentTime(precision: Option[Int])                              extends CurrentTimeBase("current_time", precision)
case class CurrentDate(precision: Option[Int])                              extends CurrentTimeBase("current_date", precision)
case class CurrentTimestamp(precision: Option[Int])                         extends CurrentTimeBase("current_timestamp", precision)
case class CurrentLocalTime(precision: Option[Int])                         extends CurrentTimeBase("localtime", precision)
case class CurrentLocalTimeStamp(precision: Option[Int])                    extends CurrentTimeBase("localtimestamp", precision)

// 1-origin parameter
case class Parameter(index: Int) extends LeafExpression
case class SubQueryExpression(query: Relation) extends Expression {
  override def children: Seq[Expression] = query.expressions
}

case class Cast(expr: Expression, tpe: String, tryCast: Boolean = false) extends UnaryExpression {
  def child = expr
}

case class SchemaProperty(key: Identifier, value: Expression) extends Expression {
  override def children: Seq[Expression] = Seq(key, value)
}
sealed trait TableElement extends Expression
case class ColumnDef(columnName: Identifier, tpe: ColumnType) extends TableElement with UnaryExpression {
  def child = columnName
}

case class ColumnType(tpe: String) extends LeafExpression
case class ColumnDefLike(tableName: QName, includeProperties: Boolean) extends TableElement with UnaryExpression {
  def child = tableName
}
