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

package wvlet.airframe.sql.model

import java.util.Locale

/**
  */
sealed trait Expression extends TreeNode[Expression] with Product {
  def sqlExpr: String = toString()

  def transformExpression(rule: PartialFunction[Expression, Expression]): this.type = {
    def recursiveTransform(arg: Any): AnyRef =
      arg match {
        case e: Expression  => e.transformExpression(rule)
        case l: LogicalPlan => l.transformExpressions(rule)
        case Some(x)        => Some(recursiveTransform(x))
        case s: Seq[_]      => s.map(recursiveTransform _)
        case other: AnyRef  => other
        case null           => null
      }

    val newExpr = if (productArity == 0) {
      this
    } else {
      val newArgs = productIterator.map(recursiveTransform).toArray[AnyRef]

      // TODO Build this LogicalPlan using Surface
      val primaryConstructor = this.getClass.getDeclaredConstructors()(0)
      primaryConstructor.newInstance(newArgs: _*).asInstanceOf[this.type]
    }

    // Apply the rule to itself
    rule
      .applyOrElse(newExpr, { (x: Expression) => x }).asInstanceOf[this.type]
  }

  def collectSubExpressions: List[Expression] = {
    def recursiveCollect(arg: Any): List[Expression] =
      arg match {
        case e: Expression  => e :: e.collectSubExpressions
        case l: LogicalPlan => l.inputExpressions
        case Some(x)        => recursiveCollect(x)
        case s: Seq[_]      => s.flatMap(recursiveCollect _).toList
        case other: AnyRef  => Nil
        case null           => Nil
      }

    productIterator.flatMap(recursiveCollect).toList
  }

  def traverseExpressions[U](rule: PartialFunction[Expression, U]): Unit = {
    def recursiveTraverse(arg: Any): Unit =
      arg match {
        case e: Expression  => e.traverseExpressions(rule)
        case l: LogicalPlan => l.traverseExpressions(rule)
        case Some(x)        => recursiveTraverse(x)
        case s: Seq[_]      => s.foreach(recursiveTraverse _)
        case other: AnyRef  =>
        case null           =>
      }

    if (rule.isDefinedAt(this)) {
      rule.apply(this)
    }
    productIterator.foreach(recursiveTraverse)
  }

  def collectExpressions(cond: PartialFunction[Expression, Boolean]): List[Expression] = {
    val l = List.newBuilder[Expression]
    traverseExpressions(new PartialFunction[Expression, Unit] {
      override def isDefinedAt(x: Expression): Boolean = cond.isDefinedAt(x)
      override def apply(v1: Expression): Unit = {
        if (cond.apply(v1)) {
          l += v1
        }
      }
    })
    l.result()
  }

  lazy val resolved: Boolean    = resolvedChildren
  def resolvedChildren: Boolean = children.forall(_.resolved) && resolvedInputs
  def resolvedInputs: Boolean   = true
}

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

  /**
    * Set an alias for the table or subquery.
    * @param newQualifier
    * @return
    */
  def withQualifier(newQualifier: String): Attribute
}

object Expression {
  import wvlet.airframe.sql.model.LogicalPlan.Relation

  def concat(expr: Seq[Expression])(merger: (Expression, Expression) => Expression): Expression = {
    require(expr.length > 0)
    if (expr.length == 1) {
      expr.head
    } else {
      expr.tail.foldLeft(expr.head) { case (prev, next) =>
        merger(prev, next)
      }
    }
  }

  def concatWithAnd(expr: Seq[Expression]): Expression = {
    concat(expr) { case (a, b) => And(a, b, None) }
  }
  def concatWithEq(expr: Seq[Expression]): Expression = {
    concat(expr) { case (a, b) => Eq(a, b, None) }
  }

  /**
    */
  case class ParenthesizedExpression(child: Expression, nodeLocation: Option[NodeLocation]) extends UnaryExpression

  // Qualified name (QName), such as table and column names
  case class QName(parts: List[String], nodeLocation: Option[NodeLocation]) extends LeafExpression {
    def fullName: String          = parts.mkString(".")
    override def toString: String = fullName
  }
  object QName {
    def apply(s: String, nodeLocation: Option[NodeLocation]): QName = {
      // TODO handle quotation
      QName(s.split("\\.").toList, nodeLocation)
    }
  }

  case class UnresolvedAttribute(name: String, nodeLocation: Option[NodeLocation]) extends Attribute {
    override def toString        = s"UnresolvedAttribute(${name})"
    override def sqlExpr: String = name
    override lazy val resolved   = false

    override def withQualifier(newQualifier: String): Attribute = this
  }

  sealed trait Identifier extends LeafExpression {
    def value: String
  }
  case class DigitId(value: String, nodeLocation: Option[NodeLocation]) extends Identifier {
    override def sqlExpr: String  = value
    override def toString: String = s"Id(${value})"
  }
  case class UnquotedIdentifier(value: String, nodeLocation: Option[NodeLocation]) extends Identifier {
    override def sqlExpr: String  = value
    override def toString: String = s"Id(${value})"
  }
  case class BackQuotedIdentifier(value: String, nodeLocation: Option[NodeLocation]) extends Identifier {
    override def sqlExpr  = s"`${value}`"
    override def toString = s"Id(`${value}`)"
  }
  case class QuotedIdentifier(value: String, nodeLocation: Option[NodeLocation]) extends Identifier {
    override def sqlExpr  = s""""${value}""""
    override def toString = s"""Id("${value}")"""
  }

  sealed trait JoinCriteria                                  extends Expression
  case class NaturalJoin(nodeLocation: Option[NodeLocation]) extends JoinCriteria with LeafExpression
  case class JoinUsing(columns: Seq[Identifier], nodeLocation: Option[NodeLocation]) extends JoinCriteria {
    override def children: Seq[Expression] = columns
    override def toString: String          = s"JoinUsing(${columns.mkString(",")})"
  }
  case class JoinOn(expr: Expression, nodeLocation: Option[NodeLocation]) extends JoinCriteria with UnaryExpression {
    override def child: Expression = expr
  }

  /**
    * Join condition used only when join keys are resolved
    * @param leftKey
    * @param rightKey
    */
  case class JoinOnEq(keys: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends JoinCriteria
      with LeafExpression {
    require(keys.forall(_.resolved), s"all keys of JoinOnEq must be resolved: ${keys}")

    /**
      * Report duplicate name join keys, which can be excluded from the parent
      * @return
      */
    def duplicateKeys: Seq[Expression] = {
      // remove duplicate column names
      var seen = Set.empty[String]
      val uniqueNameKeys = keys.collect {
        case r: ResolvedAttribute if !seen.contains(r.name) =>
          seen += r.name
          r
      }
      keys.collect {
        case x if !uniqueNameKeys.contains(x) => x
      }
    }
    override def children: Seq[Expression] = keys
  }

  case class AllColumns(qualifier: Option[QName], nodeLocation: Option[NodeLocation]) extends Attribute {
    override def name: String              = qualifier.map(x => s"${x}.*").getOrElse("*")
    override def children: Seq[Expression] = qualifier.toSeq
    override def toString                  = s"AllColumns(${name})"
    override lazy val resolved             = false

    override def withQualifier(newQualifier: String): Attribute = {
      this.copy(qualifier = Some(QName(newQualifier, nodeLocation)))
    }
  }
  case class SingleColumn(
      expr: Expression,
      alias: Option[Expression],
      qualifier: Option[String] = None,
      nodeLocation: Option[NodeLocation]
  ) extends Attribute {
    override def name: String              = alias.getOrElse(expr).toString
    override def children: Seq[Expression] = Seq(expr) ++ alias.toSeq
    override def toString = s"SingleColumn(${alias.map(a => s"${expr} as ${a}").getOrElse(s"${expr}")})"

    override def withQualifier(newQualifier: String): Attribute = {
      this.copy(qualifier = Some(newQualifier))
    }
  }

  case class SortItem(
      sortKey: Expression,
      ordering: Option[SortOrdering] = None,
      nullOrdering: Option[NullOrdering],
      nodeLocation: Option[NodeLocation]
  ) extends Expression
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
  case class Window(
      partitionBy: Seq[Expression],
      orderBy: Seq[SortItem],
      frame: Option[WindowFrame],
      nodeLocation: Option[NodeLocation]
  ) extends Expression {
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

  case class WindowFrame(
      frameType: FrameType,
      start: FrameBound,
      end: Option[FrameBound],
      nodeLocation: Option[NodeLocation]
  ) extends Expression
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
  case class FunctionCall(
      name: String,
      args: Seq[Expression],
      isDistinct: Boolean,
      filter: Option[Expression],
      window: Option[Window],
      nodeLocation: Option[NodeLocation]
  ) extends Expression {
    override def children: Seq[Expression] = args ++ filter.toSeq ++ window.toSeq
    def functionName: String               = name.toString.toLowerCase(Locale.US)
    override def toString = s"FunctionCall(${name}, ${args.mkString(", ")}, distinct:${isDistinct}, window:${window})"
  }
  case class LambdaExpr(body: Expression, args: Seq[String], nodeLocation: Option[NodeLocation])
      extends Expression
      with UnaryExpression {
    def child = body
  }

  case class Ref(name: QName, nodeLocation: Option[NodeLocation]) extends Expression with LeafExpression

  // Conditional expression
  sealed trait ConditionalExpression                  extends Expression
  case class NoOp(nodeLocation: Option[NodeLocation]) extends ConditionalExpression with LeafExpression
  case class Eq(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class NotEq(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class And(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class Or(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class Not(child: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with UnaryExpression
  case class LessThan(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class LessThanOrEq(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class GreaterThan(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class GreaterThanOrEq(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class Between(e: Expression, a: Expression, b: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(e, a, b)
  }
  case class IsNull(child: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with UnaryExpression
  case class IsNotNull(child: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with UnaryExpression
  case class In(a: Expression, list: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ list
  }
  case class NotIn(a: Expression, list: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ list
  }
  case class InSubQuery(a: Expression, in: Relation, nodeLocation: Option[NodeLocation]) extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ in.expressions
  }
  case class NotInSubQuery(a: Expression, in: Relation, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ in.expressions
  }
  case class Like(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class NotLike(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class DistinctFrom(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression
  case class NotDistinctFrom(left: Expression, right: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with BinaryExpression

  case class IfExpr(
      cond: ConditionalExpression,
      onTrue: Expression,
      onFalse: Expression,
      nodeLocation: Option[NodeLocation]
  ) extends Expression {
    override def children: Seq[Expression] = Seq(cond, onTrue, onFalse)
  }
  case class CaseExpr(
      operand: Option[Expression],
      whenClauses: Seq[WhenClause],
      defaultValue: Option[Expression],
      nodeLocation: Option[NodeLocation]
  ) extends Expression {
    override def children: Seq[Expression] = {
      val b = Seq.newBuilder[Expression]
      operand.foreach(b += _)
      b ++= whenClauses
      defaultValue.foreach(b += _)
      b.result()
    }
  }
  case class WhenClause(condition: Expression, result: Expression, nodeLocation: Option[NodeLocation])
      extends Expression {
    override def children: Seq[Expression] = Seq(condition, result)
  }

  case class Exists(child: Expression, nodeLocation: Option[NodeLocation]) extends Expression with UnaryExpression

  // Arithmetic expr
  abstract sealed class BinaryExprType(val symbol: String)
  case object Add      extends BinaryExprType("+")
  case object Subtract extends BinaryExprType("-")
  case object Multiply extends BinaryExprType("*")
  case object Divide   extends BinaryExprType("/")
  case object Modulus  extends BinaryExprType("%")

  sealed trait ArithmeticExpression extends Expression
  case class ArithmeticBinaryExpr(
      exprType: BinaryExprType,
      left: Expression,
      right: Expression,
      nodeLocation: Option[NodeLocation]
  ) extends ArithmeticExpression
      with BinaryExpression
  case class ArithmeticUnaryExpr(sign: Sign, child: Expression, nodeLocation: Option[NodeLocation])
      extends ArithmeticExpression
      with UnaryExpression

  abstract sealed class Sign(val symbol: String)
  case object Positive extends Sign("+")
  case object Negative extends Sign("-")

  // Set quantifier
  sealed trait SetQuantifier extends LeafExpression {
    def isDistinct: Boolean
  }
  case class All(nodeLocation: Option[NodeLocation]) extends SetQuantifier {
    override def isDistinct: Boolean = false
  }
  case class DistinctSet(nodeLocation: Option[NodeLocation]) extends SetQuantifier {
    override def toString: String    = "DISTINCT"
    override def isDistinct: Boolean = true
  }

  // Literal
  sealed trait Literal extends Expression {
    def stringValue: String
  }
  case class NullLiteral(nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = "null"
    override def sqlExpr: String     = "NULL"
    override def toString: String    = "Literal(NULL)"
  }
  sealed trait BooleanLiteral extends Literal {
    def booleanValue: Boolean
  }
  case class TrueLiteral(nodeLocation: Option[NodeLocation]) extends BooleanLiteral with LeafExpression {
    override def stringValue: String   = "true"
    override def sqlExpr: String       = "TRUE"
    override def toString: String      = "Literal(TRUE)"
    override def booleanValue: Boolean = true
  }
  case class FalseLiteral(nodeLocation: Option[NodeLocation]) extends BooleanLiteral with LeafExpression {
    override def stringValue: String   = "false"
    override def sqlExpr: String       = "FALSE"
    override def toString: String      = "Literal(FALSE)"
    override def booleanValue: Boolean = false
  }
  case class StringLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value
    override def sqlExpr: String     = s"'${value}'"
    override def toString            = s"Literal('${value}')"
  }
  case class TimeLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value
    override def sqlExpr             = s"TIME '${value}'"
    override def toString            = s"Literal(TIME '${value}')"
  }
  case class TimestampLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value
    override def sqlExpr             = s"TIMESTAMP '${value}'"
    override def toString            = s"Literal(TIMESTAMP '${value}')"
  }
  case class DecimalLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value
    override def sqlExpr             = s"DECIMAL '${value}'"
    override def toString            = s"Literal(DECIMAL '${value}')"
  }
  case class CharLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value
    override def sqlExpr             = s"CHAR '${value}'"
    override def toString            = s"Literal(CHAR '${value}')"
  }
  case class DoubleLiteral(value: Double, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value.toString
    override def sqlExpr             = value.toString
    override def toString            = s"Literal(${value.toString})"
  }
  case class LongLiteral(value: Long, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = value.toString
    override def sqlExpr             = value.toString
    override def toString            = s"Literal(${value.toString})"
  }
  case class IntervalLiteral(
      value: String,
      sign: Sign,
      startField: IntervalField,
      end: Option[IntervalField],
      nodeLocation: Option[NodeLocation]
  ) extends Literal {
    override def children: Seq[Expression] = Seq(startField) ++ end.toSeq
    override def stringValue: String       = s"${sign.symbol} '${value}' ${startField}"

    override def sqlExpr: String = {
      s"INTERVAL ${sign.symbol} '${value}' ${startField}"
    }
    override def toString: String = {
      s"Literal(INTERVAL ${sign.symbol} '${value}' ${startField})"
    }
  }

  case class GenericLiteral(tpe: String, value: String, nodeLocation: Option[NodeLocation])
      extends Literal
      with LeafExpression {
    override def stringValue: String = value

    override def sqlExpr  = s"${tpe} '${value}'"
    override def toString = s"Literal(${tpe} '${value}')"
  }
  case class BinaryLiteral(binary: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def stringValue: String = binary
  }

  sealed trait IntervalField extends LeafExpression {
    override def toString(): String = getClass.getSimpleName
  }
  case class Year(nodeLocation: Option[NodeLocation])   extends IntervalField
  case class Month(nodeLocation: Option[NodeLocation])  extends IntervalField
  case class Day(nodeLocation: Option[NodeLocation])    extends IntervalField
  case class Hour(nodeLocation: Option[NodeLocation])   extends IntervalField
  case class Minute(nodeLocation: Option[NodeLocation]) extends IntervalField
  case class Second(nodeLocation: Option[NodeLocation]) extends IntervalField

  // Value constructor
  case class ArrayConstructor(values: Seq[Expression], nodeLocation: Option[NodeLocation]) extends Expression {
    override def children: Seq[Expression] = values
  }

  case class RowConstructor(values: Seq[Expression], nodeLocation: Option[NodeLocation]) extends Expression {
    override def children: Seq[Expression] = values
  }

  abstract sealed class CurrentTimeBase(name: String, precision: Option[Int]) extends LeafExpression
  case class CurrentTime(precision: Option[Int], nodeLocation: Option[NodeLocation])
      extends CurrentTimeBase("current_time", precision)
  case class CurrentDate(precision: Option[Int], nodeLocation: Option[NodeLocation])
      extends CurrentTimeBase("current_date", precision)
  case class CurrentTimestamp(precision: Option[Int], nodeLocation: Option[NodeLocation])
      extends CurrentTimeBase("current_timestamp", precision)
  case class CurrentLocalTime(precision: Option[Int], nodeLocation: Option[NodeLocation])
      extends CurrentTimeBase("localtime", precision)
  case class CurrentLocalTimeStamp(precision: Option[Int], nodeLocation: Option[NodeLocation])
      extends CurrentTimeBase("localtimestamp", precision)

  // 1-origin parameter
  case class Parameter(index: Int, nodeLocation: Option[NodeLocation]) extends LeafExpression
  case class SubQueryExpression(query: Relation, nodeLocation: Option[NodeLocation]) extends Expression {
    override def children: Seq[Expression] = query.expressions
  }

  case class Cast(expr: Expression, tpe: String, tryCast: Boolean = false, nodeLocation: Option[NodeLocation])
      extends UnaryExpression {
    def child = expr
  }

  case class SchemaProperty(key: Identifier, value: Expression, nodeLocation: Option[NodeLocation]) extends Expression {
    override def children: Seq[Expression] = Seq(key, value)
  }
  sealed trait TableElement extends Expression
  case class ColumnDef(columnName: Identifier, tpe: ColumnType, nodeLocation: Option[NodeLocation])
      extends TableElement
      with UnaryExpression {
    def child = columnName
  }

  case class ColumnType(tpe: String, nodeLocation: Option[NodeLocation]) extends LeafExpression
  case class ColumnDefLike(tableName: QName, includeProperties: Boolean, nodeLocation: Option[NodeLocation])
      extends TableElement
      with UnaryExpression {
    def child = tableName
  }

  // Aggregation
  case class GroupingKey(child: Expression, nodeLocation: Option[NodeLocation]) extends UnaryExpression
}
