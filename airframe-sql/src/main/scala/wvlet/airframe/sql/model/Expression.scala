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

import wvlet.airframe.sql.catalog.DataType
import wvlet.airframe.sql.catalog.DataType._
import wvlet.airframe.sql.model.Expression.{AllColumns, MultiSourceColumn}
import wvlet.log.LogSupport

import java.util.Locale

/**
  */
sealed trait Expression extends TreeNode[Expression] with Product {
  def sqlExpr: String = toString()

  /**
    * * Returns "(name):(type)" of this attribute
    */
  def typeDescription: String = {
    dataType match {
      case e: EmbeddedRecordType =>
        e.typeDescription
      case _ =>
        s"${attributeName}:${dataTypeName}"
    }
  }

  /**
    * Column name without qualifier
    * @return
    */
  def attributeName: String = "?"
  def dataTypeName: String  = dataType.typeDescription
  def dataType: DataType    = DataType.UnknownType

  private def createInstance(args: Iterator[AnyRef]): Expression = {
    // TODO Build this LogicalPlan using Surface
    val primaryConstructor = this.getClass.getDeclaredConstructors()(0)
    primaryConstructor.newInstance(args.toArray[AnyRef]: _*).asInstanceOf[Expression]
  }

  def transformPlan(rule: PartialFunction[LogicalPlan, LogicalPlan]): Expression = {
    def recursiveTransform(arg: Any): AnyRef = {
      arg match {
        case e: Expression  => e.transformPlan(rule)
        case l: LogicalPlan => l.transform(rule)
        case Some(x)        => Some(recursiveTransform(x))
        case s: Seq[_]      => s.map(recursiveTransform _)
        case other: AnyRef  => other
        case null           => null
      }
    }

    val newArgs = productIterator.map(recursiveTransform)
    createInstance(newArgs)
  }

  def traversePlan[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    def recursiveTraverse(arg: Any): Unit = {
      arg match {
        case e: Expression  => e.traversePlan(rule)
        case l: LogicalPlan => l.traverse(rule)
        case Some(x)        => Some(recursiveTraverse(x))
        case s: Seq[_]      => s.map(recursiveTraverse _)
        case other: AnyRef  =>
        case null           =>
      }
    }
    productIterator.foreach(recursiveTraverse)
  }

  def traversePlanOnce[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    def recursiveTraverse(arg: Any): Unit = {
      arg match {
        case e: Expression  => e.traversePlanOnce(rule)
        case l: LogicalPlan => l.traverseOnce(rule)
        case Some(x)        => Some(recursiveTraverse(x))
        case s: Seq[_]      => s.map(recursiveTraverse _)
        case other: AnyRef  =>
        case null           =>
      }
    }
    productIterator.foreach(recursiveTraverse)
  }

  /**
    * Recursively transform the expression in breadth-first order
    * @param rule
    * @return
    */
  def transformExpression(rule: PartialFunction[Expression, Expression]): Expression = {
    def recursiveTransform(arg: Any): AnyRef = {
      arg match {
        case e: Expression  => e.transformExpression(rule)
        case l: LogicalPlan => l.transformExpressions(rule)
        case Some(x)        => Some(recursiveTransform(x))
        case s: Seq[_]      => s.map(recursiveTransform _)
        case other: AnyRef  => other
        case null           => null
      }
    }

    // First apply the rule to itself
    val newExpr: Expression = rule
      .applyOrElse(this, identity[Expression])

    // Next, apply the rule to child nodes
    if (newExpr.productArity == 0) {
      newExpr
    } else {
      val newArgs = newExpr.productIterator.map(recursiveTransform)
      newExpr.createInstance(newArgs)
    }
  }

  /**
    * Recursively transform the expression in depth-first order
    * @param rule
    * @return
    */
  def transformUpExpression(rule: PartialFunction[Expression, Expression]): Expression = {
    def iter(arg: Any): AnyRef =
      arg match {
        case e: Expression  => e.transformUpExpression(rule)
        case l: LogicalPlan => l.transformUpExpressions(rule)
        case Some(x)        => Some(iter(x))
        case s: Seq[_]      => s.map(iter _)
        case other: AnyRef  => other
        case null           => null
      }

    // Apply the rule first to child nodes
    val newExpr = if (productArity == 0) {
      this
    } else {
      val newArgs = productIterator.map(iter)
      createInstance(newArgs)
    }

    // Finally, apply the rule to itself
    rule
      .applyOrElse(newExpr, identity[Expression])
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
    // Unlike transform, this will traverse the selected children by the Expression
    children.foreach(recursiveTraverse)
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
  override def toString: String          = s"${getClass.getSimpleName}(left:${left}, right:${right})"
}

/**
  * Used for matching column name with Attribute
  * @param database
  * @param table
  * @param columnName
  */
case class ColumnPath(database: Option[String], table: Option[String], columnName: String)

object ColumnPath {
  def fromQName(contextDatabase: String, fullName: String): Option[ColumnPath] = {
    // TODO Should we handle quotation in the name or just reject such strings?
    fullName.split("\\.").toList match {
      case List(db, t, c) if db == contextDatabase =>
        Some(ColumnPath(Some(db), Some(t), c))
      case List(t, c) =>
        Some(ColumnPath(None, Some(t), c))
      case List(c) =>
        Some(ColumnPath(None, None, c))
      case _ =>
        None
    }
  }
}

/**
  * Attribute is used for column names of relational table inputs and outputs
  */
trait Attribute extends LeafExpression with LogSupport {
  override def attributeName: String = name
  def name: String
  def fullName: String = {
    s"${prefix}${name}"
  }
  def prefix: String = qualifier.map(q => s"${q}.").getOrElse("")

  /**
    * Returns the unmodified source columns referenced by this Attribute
    */
  def sourceColumns: Seq[SourceColumn]

  // (database name)?.(table name) given in the original SQL
  def qualifier: Option[String]
  def withQualifier(newQualifier: String): Attribute = withQualifier(Some(newQualifier))
  def withQualifier(newQualifier: Option[String]): Attribute
  def setQualifierIfEmpty(newQualifier: Option[String]): Attribute = {
    qualifier match {
      case Some(q) => this
      case None    => this.withQualifier(newQualifier)
    }
  }

  import Expression.Alias
  def alias: Option[String] = {
    this match {
      case a: Expression.Alias => Some(a.name)
      case _                   => None
    }
  }
  def withAlias(newAlias: String): Attribute = withAlias(Some(newAlias))
  def withAlias(newAlias: Option[String]): Attribute = {
    newAlias match {
      case None => this
      case Some(alias) =>
        this match {
          case a: Alias =>
            if (name != alias) a.copy(name = alias) else a
          case other if other.name == alias =>
            // No need to have alias
            other
          case other =>
            Alias(qualifier, alias, other, None)
        }
    }
  }

  /**
    * Return columns generated from this Attribute
    */
  def inputColumns: Seq[Attribute]

  /**
    * Return true if this Attribute matches with a given column path
    * @param columnPath
    * @return
    */
  def matchesWith(columnPath: ColumnPath): Boolean = {
    def matchesWith(columnName: String): Boolean = {
      this match {
        case a: AllColumns =>
          a.inputColumns.exists(_.name == columnName)
        case a: Attribute if a.name == columnName =>
          true
        case _ =>
          false
      }
    }

    columnPath.table match {
      // TODO handle (catalog).(database).(table) names in the qualifier
      case Some(tableName) =>
        qualifier.exists(_ == tableName) && matchesWith(columnPath.columnName)
      case None =>
        matchesWith(columnPath.columnName)
    }
  }

  /**
    * If a given column name matches with this Attribute, return this. If there are multiple candidate attributes (e.g.,
    * via Join, Union), return MultiSourceAttribute.
    */
  def matched(columnPath: ColumnPath): Option[Attribute] = {
    def findMatched(columnName: String): Seq[Attribute] = {
      this match {
        case a: AllColumns =>
          a.inputColumns.filter(_.name == columnName)
        case a: Attribute if a.name == columnName =>
          Seq(a)
        case _ =>
          Seq.empty
      }
    }

    val result: Seq[Attribute] = columnPath.table match {
      // TODO handle (catalog).(database).(table) names in the qualifier
      case Some(tableName) =>
        if (qualifier.exists(_ == tableName)) {
          findMatched(columnPath.columnName).map(_.withQualifier(qualifier))
        } else {
          this match {
            case r: ResolvedAttribute if r.sourceColumn.nonEmpty && r.sourceColumn.get.table.name == tableName =>
              findMatched(columnPath.columnName)
            case _ =>
              Nil
          }
        }
      case None =>
        findMatched(columnPath.columnName)
    }

    if (result.size > 1) {
      val q = if (result.forall(_.qualifier == result.head.qualifier)) {
        // Preserve the qualifier
        result.head.qualifier
      } else {
        qualifier
      }
      Some(MultiSourceColumn(result, qualifier = q, None))
    } else {
      result.headOption
    }
  }

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

  case class UnresolvedAttribute(
      override val qualifier: Option[String],
      name: String,
      nodeLocation: Option[NodeLocation]
  ) extends Attribute {
    override def toString: String = s"UnresolvedAttribute(${fullName})"
    override def sqlExpr: String  = name
    override lazy val resolved    = false
    override def withQualifier(newQualifier: Option[String]): UnresolvedAttribute = {
      this.copy(qualifier = newQualifier)
    }
    override def inputColumns: Seq[Attribute]     = Seq.empty
    override def sourceColumns: Seq[SourceColumn] = Seq.empty

  }

  sealed trait Identifier extends LeafExpression {
    def value: String
    override def attributeName: String  = value
    override lazy val resolved: Boolean = false
    def toResolved: ResolvedIdentifier  = ResolvedIdentifier(this)
  }
  case class ResolvedIdentifier(id: Identifier) extends Identifier {
    override def value: String                      = id.value
    override def nodeLocation: Option[NodeLocation] = id.nodeLocation
    override def sqlExpr: String                    = id.sqlExpr
    override lazy val resolved: Boolean             = true
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

  sealed trait JoinCriteria extends Expression
  case class NaturalJoin(nodeLocation: Option[NodeLocation]) extends JoinCriteria with LeafExpression {
    override def toString: String = "NaturalJoin"
  }
  case class JoinUsing(columns: Seq[Identifier], nodeLocation: Option[NodeLocation]) extends JoinCriteria {
    override def children: Seq[Expression] = columns
    override def toString: String          = s"JoinUsing(${columns.mkString(",")})"
  }
  case class ResolvedJoinUsing(keys: Seq[MultiSourceColumn], nodeLocation: Option[NodeLocation]) extends JoinCriteria {
    override def children: Seq[Expression] = keys
    override def toString: String          = s"ResolvedJoinUsing(${keys.mkString(",")})"
    override lazy val resolved: Boolean    = true
  }
  case class JoinOn(expr: Expression, nodeLocation: Option[NodeLocation]) extends JoinCriteria with UnaryExpression {
    override def child: Expression = expr
    override def toString: String  = s"JoinOn(${expr})"
  }

  /**
    * Join condition used only when join keys are resolved
    */
  case class JoinOnEq(keys: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends JoinCriteria
      with LeafExpression {
    require(keys.forall(_.resolved), s"all keys of JoinOnEq must be resolved: ${keys}")

    override def children: Seq[Expression] = keys
    override def toString: String          = s"JoinOnEq(${keys.mkString(", ")})"
  }

  case class AllColumns(
      override val qualifier: Option[String],
      columns: Option[Seq[Attribute]],
      nodeLocation: Option[NodeLocation]
  ) extends Attribute
      with LogSupport {
    override def name: String = "*"

    override def children: Seq[Expression] = {
      // AllColumns is a reference to the input attributes.
      // Return empty so as not to traverse children from here.
      Seq.empty
    }
    override def inputColumns: Seq[Attribute] = {
      columns match {
        case Some(columns) =>
          columns.flatMap {
            case a: AllColumns => a.inputColumns
            case a             => Seq(a)
          }
        case None => Nil
      }
    }

    override def dataType: DataType = {
      columns
        .map(cols => EmbeddedRecordType(cols.map(x => NamedType(x.name, x.dataType))))
        .getOrElse(DataType.UnknownType)
    }

    override def withQualifier(newQualifier: Option[String]): Attribute = {
      this.copy(qualifier = newQualifier)
    }

    override def toString = {
      columns match {
        case Some(attrs) if attrs.nonEmpty =>
          val inputs = attrs
            .map(a => s"${a.fullName}:${a.dataTypeName}").mkString(", ")
          s"AllColumns(${inputs})"
        case _ =>
          s"AllColumns(${fullName})"
      }
    }

    override def sourceColumns: Seq[SourceColumn] = {
      columns.map(_.flatMap(_.sourceColumns)).getOrElse(Seq.empty)
    }

    override lazy val resolved = columns.isDefined
  }

  case class Alias(
      qualifier: Option[String],
      name: String,
      expr: Expression,
      nodeLocation: Option[NodeLocation]
  ) extends Attribute {
    override def inputColumns: Seq[Attribute] = Seq(this)
    override def children: Seq[Expression]    = Seq(expr)

    override def withQualifier(newQualifier: Option[String]): Attribute = {
      this.copy(qualifier = newQualifier)
    }

    override def toString: String = {
      s"<${fullName}> := ${expr}"
    }
    override def dataType: DataType = expr.dataType

    override def sqlExpr: String = {
      s"${expr.sqlExpr} AS ${fullName}"
    }

    override def sourceColumns: Seq[SourceColumn] = {
      expr match {
        case a: Attribute => a.sourceColumns
        case _            => Seq.empty
      }
    }
  }

  /**
    * An attribute that produces a single column value with a given expression.
    *
    * @param expr
    * @param qualifier
    * @param nodeLocation
    */
  case class SingleColumn(
      expr: Expression,
      qualifier: Option[String] = None,
      nodeLocation: Option[NodeLocation]
  ) extends Attribute {
    override def name: String       = expr.attributeName
    override def dataType: DataType = expr.dataType

    override def inputColumns: Seq[Attribute] = Seq(this)
    override def children: Seq[Expression]    = Seq(expr)
    override def toString                     = s"${fullName}:${dataTypeName} := ${expr}"

    override def sqlExpr: String = expr.sqlExpr
    override def withQualifier(newQualifier: Option[String]): Attribute = {
      this.copy(qualifier = newQualifier)
    }

    override def sourceColumns: Seq[SourceColumn] = {
      expr match {
        case a: Attribute => a.sourceColumns
        case _            => Seq.empty
      }
    }
  }

  /**
    * A single column merged from multiple input expressions (e.g., union, join)
    * @param inputs
    * @param qualifier
    * @param nodeLocation
    */
  case class MultiSourceColumn(
      inputs: Seq[Expression],
      qualifier: Option[String],
      nodeLocation: Option[NodeLocation]
  ) extends Attribute {
    require(inputs.nonEmpty, s"The inputs of MultiSourceColumn should not be empty: ${this}")

    override def toString: String = s"${fullName}:${dataTypeName} := {${inputs.mkString(", ")}}"

    override def inputColumns: Seq[Attribute] = {
      inputs.map {
        case a: Attribute => a
        case e: Expression =>
          SingleColumn(e, qualifier, e.nodeLocation)
      }
    }
    override def children: Seq[Expression] = {
      // MultiSourceColumn is a reference to the multiple columns. Do not traverse here
      Seq.empty
    }

    override def sqlExpr: String = fullName

    override def name: String = {
      inputs.head.attributeName
    }
    override def dataType: DataType = {
      inputs.head.dataType
    }

    override def withQualifier(newQualifier: Option[String]): Attribute = {
      this.copy(qualifier = newQualifier)
    }

    override def sourceColumns: Seq[SourceColumn] = {
      inputs.flatMap {
        case a: Attribute => a.sourceColumns
        case _            => Seq.empty
      }
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
    override def toString: String  = s"SortItem(sortKey:${sortKey}, ordering:${ordering}, nullOrdering:${nullOrdering})"
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
    override def toString: String =
      s"Window(partitionBy:${partitionBy.mkString(", ")}, orderBy:${orderBy.mkString(", ")}, frame:${frame})"
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
    override def dataType: DataType = {
      if (functionName == "count") {
        DataType.LongType
      } else {
        // TODO: Resolve the function return type using a function catalog
        DataType.UnknownType
      }
    }

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
      with UnaryExpression {
    override def toString: String = s"IsNull(${child})"
  }
  case class IsNotNull(child: Expression, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression
      with UnaryExpression {
    override def toString: String = s"IsNotNull(${child})"
  }
  case class In(a: Expression, list: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ list
    override def toString: String          = s"In(${a} <in> [${list.mkString(", ")}])"
  }
  case class NotIn(a: Expression, list: Seq[Expression], nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ list
    override def toString: String          = s"NotIn(${a} <not in> [${list.mkString(", ")}])"
  }
  case class InSubQuery(a: Expression, in: Relation, nodeLocation: Option[NodeLocation]) extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ in.childExpressions
    override def toString: String          = s"InSubQuery(${a} <in> ${in})"
  }
  case class NotInSubQuery(a: Expression, in: Relation, nodeLocation: Option[NodeLocation])
      extends ConditionalExpression {
    override def children: Seq[Expression] = Seq(a) ++ in.childExpressions
    override def toString: String          = s"NotInSubQuery(${a} <not in> ${in})"
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
      with BinaryExpression {
    override def dataType: DataType = {
      if (left.dataType == right.dataType) {
        left.dataType
      } else {
        // TODO type escalation e.g., (Double) op (Long) -> (Double)
        DataType.UnknownType
      }
    }
    override def toString: String = {
      s"${exprType}(left:$left, right:$right)"
    }
  }
  case class ArithmeticUnaryExpr(sign: Sign, child: Expression, nodeLocation: Option[NodeLocation])
      extends ArithmeticExpression
      with UnaryExpression

  abstract sealed class Sign(val symbol: String)
  case object Positive extends Sign("+")
  case object Negative extends Sign("-")

  // Set quantifier
  sealed trait SetQuantifier extends LeafExpression {
    def isDistinct: Boolean
    override def toString: String = getClass.getSimpleName
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
    override def dataType: DataType  = DataType.NullType
    override def stringValue: String = "null"
    override def sqlExpr: String     = "NULL"
    override def toString: String    = "Literal(NULL)"
  }
  sealed trait BooleanLiteral extends Literal {
    override def dataType: DataType = DataType.BooleanType
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
    override def dataType: DataType  = DataType.StringType
    override def stringValue: String = value
    override def sqlExpr: String     = s"'${value}'"
    override def toString            = s"StringLiteral('${value}')"
  }
  case class TimeLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.TimestampType(TimestampField.TIME, false)
    override def stringValue: String = value
    override def sqlExpr             = s"TIME '${value}'"
    override def toString            = s"Literal(TIME '${value}')"
  }
  case class TimestampLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.TimestampType(TimestampField.TIMESTAMP, false)
    override def stringValue: String = value
    override def sqlExpr             = s"TIMESTAMP '${value}'"
    override def toString            = s"Literal(TIMESTAMP '${value}')"
  }
  case class DecimalLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.DecimalType(TypeVariable("precision"), TypeVariable("scale"))
    override def stringValue: String = value
    override def sqlExpr             = s"DECIMAL '${value}'"
    override def toString            = s"Literal(DECIMAL '${value}')"
  }
  case class CharLiteral(value: String, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.CharType(None)
    override def stringValue: String = value
    override def sqlExpr             = s"CHAR '${value}'"
    override def toString            = s"Literal(CHAR '${value}')"
  }
  case class DoubleLiteral(value: Double, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.DoubleType
    override def stringValue: String = value.toString
    override def sqlExpr             = value.toString
    override def toString            = s"DoubleLiteral(${value.toString})"
  }
  case class LongLiteral(value: Long, nodeLocation: Option[NodeLocation]) extends Literal with LeafExpression {
    override def dataType: DataType  = DataType.LongType
    override def stringValue: String = value.toString
    override def sqlExpr             = value.toString
    override def toString            = s"LongLiteral(${value.toString})"
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
    def elementType: DataType = {
      val elemTypes = values.map(_.dataType).distinct
      if (elemTypes.size == 1) {
        elemTypes.head
      } else {
        AnyType
      }
    }
    override def dataType: DataType = {
      ArrayType(elementType)
    }
    override def children: Seq[Expression] = values
    override def toString: String          = s"Array(${values.mkString(", ")})"
  }

  case class RowConstructor(values: Seq[Expression], nodeLocation: Option[NodeLocation]) extends Expression {
    override def dataType: DataType = {
      EmbeddedRecordType(values.map(_.dataType))
    }
    override def children: Seq[Expression] = values
    override def toString: String          = s"Row(${values.mkString(", ")})"
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
    override def children: Seq[Expression] = query.childExpressions
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
  case class GroupingKey(child: Expression, nodeLocation: Option[NodeLocation]) extends UnaryExpression {
    override def toString: String = s"GroupingKey($child)"
  }
}
