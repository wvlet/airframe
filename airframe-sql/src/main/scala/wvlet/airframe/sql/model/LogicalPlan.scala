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
import wvlet.airframe.sql.analyzer.{QuerySignatureConfig}
import wvlet.airframe.sql.Assertion.*
import wvlet.log.LogSupport

import java.util.UUID

trait LogicalPlan extends TreeNode[LogicalPlan] with Product with SQLSig {
  def modelName: String = {
    val n = this.getClass.getSimpleName
    n.stripSuffix("$")
  }

  def pp: String = {
    LogicalPlanPrinter.print(this)
  }

  /**
    * All child nodes of this plan node
    *
    * @return
    */
  def children: Seq[LogicalPlan]

  /**
    * Return child expressions associated to this LogicalPlan node
    *
    * @return
    *   child expressions of this node
    */
  def childExpressions: Seq[Expression] = {
    def collectExpression(x: Any): Seq[Expression] = {
      x match {
        case e: Expression  => e :: Nil
        case p: LogicalPlan => Nil
        case Some(x)        => collectExpression(x)
        case s: Iterable[_] => s.flatMap(collectExpression _).toSeq
        case other          => Nil
      }
    }

    productIterator.flatMap { x =>
      collectExpression(x)
    }.toSeq
  }

  def mapChildren(f: LogicalPlan => LogicalPlan): LogicalPlan = {
    var changed = false
    def transformElement(arg: Any): AnyRef =
      arg match {
        case e: Expression => {
          val newExpr = e.transformPlan { case x =>
            f(x)
          }
          if (!newExpr.eq(e)) {
            changed = true
          }
          newExpr
        }
        case l: LogicalPlan => {
          val newPlan = f(l)
          if (!newPlan.eq(l)) {
            changed = true
          }
          newPlan
        }
        case Some(x)       => Some(transformElement(x))
        case s: Seq[_]     => s.map(transformElement _)
        case other: AnyRef => other
        case null          => null
      }

    val newArgs = productIterator.map(transformElement).toIndexedSeq
    if (changed) {
      copyInstance(newArgs)
    } else {
      this
    }
  }

  private def recursiveTraverse[U](f: PartialFunction[LogicalPlan, U])(arg: Any): Unit = {
    def loop(v: Any): Unit = {
      v match {
        case e: Expression => e.traversePlan(f)
        case l: LogicalPlan => {
          if (f.isDefinedAt(l)) {
            f.apply(l)
          }
          l.productIterator.foreach(x => loop(x))
        }
        case Some(x)       => Some(loop(x))
        case s: Seq[_]     => s.map(x => loop(x))
        case other: AnyRef =>
        case null          =>
      }
    }
    loop(arg)
  }

  /**
    * Recursively traverse plan nodes and apply the given function to LogicalPlan nodes
    *
    * @param rule
    */
  def traverse[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    recursiveTraverse(rule)(this)
  }

  /**
    * Recursively traverse the child plan nodes and apply the given function to LogicalPlan nodes
    *
    * @param rule
    */
  def traverseChildren[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    productIterator.foreach(child => recursiveTraverse(rule)(child))
  }

  private def recursiveTraverseOnce[U](f: PartialFunction[LogicalPlan, U])(arg: Any): Unit = {
    def loop(v: Any): Unit = {
      v match {
        case e: Expression => e.traversePlanOnce(f)
        case l: LogicalPlan => {
          if (f.isDefinedAt(l)) {
            f.apply(l)
          } else {
            l.productIterator.foreach(x => loop(x))
          }
        }
        case Some(x)       => Some(loop(x))
        case s: Seq[_]     => s.map(x => loop(x))
        case other: AnyRef =>
        case null          =>
      }
    }
    loop(arg)
  }

  /**
    * Recursively traverse the plan nodes until the rule matches.
    *
    * @param rule
    * @tparam U
    */
  def traverseOnce[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    recursiveTraverseOnce(rule)(this)
  }

  /**
    * Recursively traverse the child plan nodes until the rule matches.
    * @param rule
    * @tparam U
    */
  def traverseChildrenOnce[U](rule: PartialFunction[LogicalPlan, U]): Unit = {
    productIterator.foreach(child => recursiveTraverseOnce(rule)(child))
  }

  /**
    * Iterate through LogicalPlans and apply matching rules for transformation. The transformation will be applied to
    * the current node as well.
    *
    * @param rule
    * @return
    */
  def transform(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    val newNode: LogicalPlan = rule.applyOrElse(this, identity[LogicalPlan])
    if (newNode.eq(this)) {
      mapChildren(_.transform(rule))
    } else {
      newNode.mapChildren(_.transform(rule))
    }
  }

  def transformUp(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    val newNode = this.mapChildren(_.transformUp(rule))
    rule.applyOrElse(newNode, identity[LogicalPlan])
  }

  /**
    * Traverse the tree until finding the nodes matching the pattern. All nodes found from the root will be transformed,
    * and no further recursive match will occur from the transformed nodes.
    *
    * If you want to continue the transformation for the child nodes, use [[transformChildren]] or
    * [[transformChildrenOnce]] inside the rule.
    * @param rule
    * @return
    */
  def transformOnce(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    val newNode: LogicalPlan = rule.applyOrElse(this, identity[LogicalPlan])
    if (newNode.eq(this)) {
      transformChildrenOnce(rule)
    } else {
      // The root node was transformed
      newNode
    }
  }

  /**
    * Transform child node only once
    *
    * @param rule
    * @return
    */
  def transformChildren(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    var changed = false

    def transformElement(arg: Any): AnyRef =
      arg match {
        case e: Expression => e
        case l: LogicalPlan => {
          val newPlan = rule.applyOrElse(l, identity[LogicalPlan])
          if (!newPlan.eq(l)) {
            changed = true
          }
          newPlan
        }
        case Some(x)       => Some(transformElement(x))
        case s: Seq[_]     => s.map(transformElement _)
        case other: AnyRef => other
        case null          => null
      }

    val newArgs = productIterator.map(transformElement).toIndexedSeq
    if (changed) {
      copyInstance(newArgs)
    } else {
      this
    }
  }

  /**
    * Apply [[transformOnce]] for all child nodes.
    *
    * @param rule
    * @return
    */
  def transformChildrenOnce(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    var changed = false

    def recursiveTransform(arg: Any): AnyRef =
      arg match {
        case e: Expression => e
        case l: LogicalPlan => {
          val newPlan = l.transformOnce(rule)
          if (!newPlan.eq(l)) {
            changed = true
          }
          newPlan
        }
        case Some(x)       => Some(recursiveTransform(x))
        case s: Seq[_]     => s.map(recursiveTransform _)
        case other: AnyRef => other
        case null          => null
      }

    val newArgs = productIterator.map(recursiveTransform).toIndexedSeq
    if (changed) {
      copyInstance(newArgs)
    } else {
      this
    }
  }

  /**
    * Recursively transform all nested expressions
    * @param rule
    * @return
    */
  def transformExpressions(rule: PartialFunction[Expression, Expression]): LogicalPlan = {
    var changed = false
    def loopOnlyPlan(arg: Any): AnyRef = {
      arg match {
        case e: Expression => e
        case l: LogicalPlan =>
          val newPlan = l.transformExpressions(rule)
          if (l eq newPlan) {
            l
          } else {
            changed = true
            newPlan
          }
        case Some(x)       => Some(loopOnlyPlan(x))
        case s: Seq[_]     => s.map(loopOnlyPlan _)
        case other: AnyRef => other
        case null          => null
      }
    }

    // Transform child expressions first
    val newPlan = transformChildExpressions(rule)
    val newArgs = newPlan.productIterator.map(loopOnlyPlan).toSeq
    if (changed) {
      copyInstance(newArgs)
    } else {
      newPlan
    }
  }

  /**
    * Depth-first transformation of expression
    *
    * @param rule
    * @return
    */
  def transformUpExpressions(rule: PartialFunction[Expression, Expression]): LogicalPlan = {
    var changed = false
    def iter(arg: Any): AnyRef =
      arg match {
        case e: Expression =>
          val newExpr = e.transformUpExpression(rule)
          if (e eq newExpr) {
            e
          } else {
            changed = true
            newExpr
          }
        case l: LogicalPlan =>
          val newPlan = l.transformUpExpressions(rule)
          if (l eq newPlan) l
          else {
            changed = true
            newPlan
          }
        case Some(x)       => Some(iter(x))
        case s: Seq[_]     => s.map(iter _)
        case other: AnyRef => other
        case null          => null
      }

    val newArgs = productIterator.map(iter).toIndexedSeq
    if (changed) {
      copyInstance(newArgs)
    } else {
      this
    }
  }

  /**
    * Transform only child expressions
    * @param rule
    * @return
    */
  def transformChildExpressions(rule: PartialFunction[Expression, Expression]): LogicalPlan = {
    var changed = false
    def iterOnce(arg: Any): AnyRef =
      arg match {
        case e: Expression =>
          val newExpr = rule.applyOrElse(e, identity[Expression])
          if (e eq newExpr) {
            e
          } else {
            changed = true
            newExpr
          }
        case l: LogicalPlan => l
        case Some(x)        => Some(iterOnce(x))
        case s: Seq[_]      => s.map(iterOnce _)
        case other: AnyRef  => other
        case null           => null
      }

    val newArgs = productIterator.map(iterOnce).toIndexedSeq
    if (changed)
      copyInstance(newArgs)
    else
      this
  }

  protected def copyInstance(newArgs: Seq[AnyRef]): this.type = {
    val primaryConstructor = this.getClass.getDeclaredConstructors()(0)
    val newObj             = primaryConstructor.newInstance(newArgs: _*)
    newObj.asInstanceOf[this.type]
  }

  /**
    * List all input expressions to the plan
    * @return
    */
  def inputExpressions: List[Expression] = {
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

  /**
    * Collect from all input expressions and report matching expressions
    * @param rule
    * @return
    */
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

    productIterator.foreach(recursiveTraverse)
  }

  // Input attributes (column names) of the relation
  def inputAttributes: Seq[Attribute]
  // Output attributes (column names) of the relation
  def outputAttributes: Seq[Attribute]

  // True if all input attributes are resolved.
  lazy val resolved: Boolean    = childExpressions.forall(_.resolved) && resolvedChildren
  def resolvedChildren: Boolean = children.forall(_.resolved)

  def unresolvedExpressions: Seq[Expression] = {
    collectExpressions { case x: Expression => !x.resolved }
  }
}

trait LeafPlan extends LogicalPlan {
  override def children: Seq[LogicalPlan]      = Nil
  override def inputAttributes: Seq[Attribute] = Nil
}

trait UnaryPlan extends LogicalPlan {
  def child: LogicalPlan
  override def children: Seq[LogicalPlan] = child :: Nil

  override def inputAttributes: Seq[Attribute] = child.outputAttributes
}

trait BinaryPlan extends LogicalPlan {
  def left: LogicalPlan
  def right: LogicalPlan
  override def children: Seq[LogicalPlan] = Seq(left, right)
}

/**
  * A trait for LogicalPlan nodes that can generate SQL signatures
  */
trait SQLSig {
  def sig(config: QuerySignatureConfig = QuerySignatureConfig()): String
}

object LogicalPlan {
  import Expression.*

  private def isSelectAll(selectItems: Seq[Attribute]): Boolean = {
    selectItems.exists {
      case AllColumns(x, _, _, _) => true
      case _                      => false
    }
  }

  // Relational operator
  trait Relation extends LogicalPlan with SQLSig

  // A relation that takes a single input relation
  sealed trait UnaryRelation extends Relation with UnaryPlan {
    def inputRelation: Relation = child
    override def child: Relation
  }

  case class ParenthesizedRelation(child: Relation, nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String = child.sig(config)
    override def inputAttributes: Seq[Attribute]           = child.inputAttributes
    override def outputAttributes: Seq[Attribute]          = child.outputAttributes
  }
  case class AliasedRelation(
      child: Relation,
      alias: Identifier,
      columnNames: Option[Seq[String]],
      nodeLocation: Option[NodeLocation]
  ) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String = child.sig(config)
    override def toString: String = {
      columnNames match {
        case Some(columnNames) =>
          s"AliasedRelation[${alias}](Select[${columnNames.mkString(", ")}](${child}))"
        case None =>
          s"AliasedRelation[${alias}](${child})"
      }
    }

    override def inputAttributes: Seq[Attribute] = child.inputAttributes
    override def outputAttributes: Seq[Attribute] = {
      val attrs = child.outputAttributes.map(_.withTableAlias(alias.value))
      val result = columnNames match {
        case Some(columnNames) =>
          attrs.zip(columnNames).map { case (a, columnName) =>
            a match {
              case a: Attribute => a.withAlias(columnName)
              case others       => others
            }
          }
        case None =>
          attrs
      }
      result
    }
  }

  case class Values(rows: Seq[Expression], nodeLocation: Option[NodeLocation]) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      s"V[${rows.length}]"
    }
    override def toString: String = s"Values(${rows.mkString(", ")})"
    override def outputAttributes: Seq[Attribute] = {
      val values = rows.map { row =>
        row match {
          case r: RowConstructor => r.values
          case other             => Seq(other)
        }
      }
      val columns = (0 until values.head.size).map { i =>
        MultiSourceColumn(values.map(_(i)), None, None, None)
      }
      columns
    }
  }

  case class TableRef(name: QName, nodeLocation: Option[NodeLocation]) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      if (config.embedTableNames) {
        name.toString
      } else {
        "T"
      }
    }
    override def toString: String                 = s"TableRef(${name})"
    override def outputAttributes: Seq[Attribute] = Nil
    override lazy val resolved: Boolean           = false
  }
  case class RawSQL(sql: String, nodeLocation: Option[NodeLocation]) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = "Q"
    override def outputAttributes: Seq[Attribute]          = Nil
  }

  // Deduplicate (duplicate elimination) the input relation
  case class Distinct(child: Relation, nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"E(${child.sig(config)})"
    override def toString: String                 = s"Distinct(${child})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Sort(child: Relation, orderBy: Seq[SortItem], nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"O[${orderBy.length}](${child.sig(config)})"
    override def toString: String                 = s"Sort[${orderBy.mkString(", ")}](${child})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Limit(child: Relation, limit: LongLiteral, nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"L(${child.sig(config)})"
    override def toString: String                 = s"Limit[${limit.value}](${child})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Filter(child: Relation, filterExpr: Expression, nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"F(${child.sig(config)})"
    override def toString: String                 = s"Filter[${filterExpr}](${child})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class EmptyRelation(nodeLocation: Option[NodeLocation]) extends Relation with LeafPlan {
    // Need to override this method so as not to create duplicate case object instances
    override def copyInstance(newArgs: Seq[AnyRef]) = this
    override def sig(config: QuerySignatureConfig)  = ""
    override def toString: String                   = s"EmptyRelation()"
    override def outputAttributes: Seq[Attribute]   = Nil
  }

// This node can be a pivot node for generating a SELECT statement
  sealed trait Selection extends UnaryRelation {
    def selectItems: Seq[Attribute]
  }

  case class Project(child: Relation, selectItems: Seq[Attribute], nodeLocation: Option[NodeLocation])
      extends UnaryRelation
      with Selection {
    override def sig(config: QuerySignatureConfig): String = {
      val proj =
        if (LogicalPlan.isSelectAll(selectItems)) "*"
        else s"${selectItems.length}"
      s"P[${proj}](${child.sig(config)})"
    }
    override def toString: String = s"Project[${selectItems.mkString(", ")}](${child})"

    override def outputAttributes: Seq[Attribute] = {
      selectItems
    }
  }

  case class Aggregate(
      child: Relation,
      selectItems: List[Attribute],
      groupingKeys: List[GroupingKey],
      having: Option[Expression],
      nodeLocation: Option[NodeLocation]
  ) extends UnaryRelation
      with Selection {
    override def sig(config: QuerySignatureConfig): String = {
      val proj =
        if (LogicalPlan.isSelectAll(selectItems)) "*"
        else s"${selectItems.length}"
      s"A[${proj},${groupingKeys.length}](${child.sig(config)})"
    }

    override def toString =
      s"Aggregate[${groupingKeys.mkString(",")}](Select[${selectItems.mkString(", ")}](${child}))"

    override def outputAttributes: Seq[Attribute] = {
      selectItems
    }
  }

  case class Query(withQuery: With, body: Relation, nodeLocation: Option[NodeLocation]) extends Relation {
    override def children: Seq[LogicalPlan] = {
      val b = Seq.newBuilder[LogicalPlan]
      b ++= withQuery.children
      b += body
      b.result()
    }

    override def sig(config: QuerySignatureConfig): String = {
      val wq = for (q <- withQuery.queries) yield {
        s"${q.query.sig(config)}"
      }
      val wq_s = wq.mkString(",")

      s"W[${wq_s}](${body.sig(config)})"
    }

    override def toString: String = s"Query(with:${withQuery}, body:${body})"

    override def inputAttributes: Seq[Attribute]  = body.inputAttributes
    override def outputAttributes: Seq[Attribute] = body.outputAttributes
  }
  case class With(recursive: Boolean, queries: Seq[WithQuery], nodeLocation: Option[NodeLocation]) extends LogicalPlan {
    override def sig(config: QuerySignatureConfig) = ""
    override def toString: String = {
      s"With(recursive:${recursive}, ${queries.mkString(", ")})"
    }
    override def children: Seq[LogicalPlan]       = queries
    override def inputAttributes: Seq[Attribute]  = ???
    override def outputAttributes: Seq[Attribute] = ???
  }
  case class WithQuery(
      name: Identifier,
      query: Relation,
      columnNames: Option[Seq[Identifier]],
      nodeLocation: Option[NodeLocation]
  ) extends LogicalPlan
      with UnaryPlan {
    override def sig(config: QuerySignatureConfig) = ""
    override def toString: String = {
      columnNames match {
        case Some(columnNames) =>
          s"WithQuery[${name}](Select[${columnNames.mkString(", ")}](${query}))"
        case None =>
          s"WithQuery[${name}](${query})"
      }
    }
    override def child: LogicalPlan              = query
    override def inputAttributes: Seq[Attribute] = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = {
      columnNames match {
        case Some(aliases) =>
          query.outputAttributes.zip(aliases).map { case (in, alias) =>
            // TODO No need to re-wrap by SingleColumn for ResolvedAttribute, SingleColumn and MultiColumn?
            SingleColumn(
              in,
              None,
              None,
              alias.nodeLocation
            ).withAlias(alias.value)
          }
        case None =>
          query.outputAttributes
      }
    }
  }

// Joins
  case class Join(
      joinType: JoinType,
      left: Relation,
      right: Relation,
      cond: JoinCriteria,
      nodeLocation: Option[NodeLocation]
  ) extends Relation
      with LogSupport {
    override def modelName: String          = joinType.toString
    override def children: Seq[LogicalPlan] = Seq(left, right)
    override def sig(config: QuerySignatureConfig): String = {
      s"${joinType.symbol}(${left.sig(config)},${right.sig(config)})"
    }
    override def toString: String = s"${joinType}[${cond}](left:${left}, right:${right})"
    override def inputAttributes: Seq[Attribute] = {
      left.outputAttributes ++ right.outputAttributes
    }
    override def outputAttributes: Seq[Attribute] = {
      cond match {
        case ju: ResolvedJoinUsing =>
          val joinKeys = ju.keys
          val otherAttributes = inputAttributes
            // Expand AllColumns here
            .flatMap(_.outputColumns)
            .filter { x =>
              !joinKeys.exists(jk => jk.name == x.name)
            }
          // report join keys (merged) and other attributes
          joinKeys ++ otherAttributes
        case _ =>
          // Report including duplicated name columns
          inputAttributes
      }
    }

    def withCond(cond: JoinCriteria): Join = this.copy(cond = cond)
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

  sealed trait SetOperation extends Relation with LogSupport {
    override def children: Seq[Relation]

    override def outputAttributes: Seq[Attribute] = mergeOutputAttributes
    protected def mergeOutputAttributes: Seq[Attribute] = {
      // Collect all input attributes
      def collectInputAttributes(rels: Seq[Relation]): Seq[Seq[Attribute]] = {
        rels.flatMap {
          case s: SetOperation => collectInputAttributes(s.children)
          case other =>
            Seq(other.outputAttributes.flatMap {
              case a: AllColumns => a.inputColumns
              case other =>
                other.inputColumns match {
                  case x if x.length <= 1 => x
                  case inputs             => Seq(MultiSourceColumn(inputs, None, None, None))
                }
            })
        }
      }

      val outputAttributes: Seq[Seq[Attribute]] = collectInputAttributes(children)

      // Verify all relations have the same number of columns
      require(
        outputAttributes.map(_.size).distinct.size == 1,
        "All relations in set operation must have the same number of columns",
        nodeLocation
      )

      // Transpose a set of relation columns into a list of same columns
      // relations: (Ra(a1, a2, ...), Rb(b1, b2, ...))
      // column lists: ((a1, b1, ...), (a2, b2, ...)
      val sameColumnList = outputAttributes.transpose
      sameColumnList.map { columns =>
        val head       = columns.head
        val qualifiers = columns.map(_.qualifier).distinct
        val col = MultiSourceColumn(
          inputs = columns.toSeq,
          qualifier = {
            // If all of the qualifiers are the same, use it.
            if (qualifiers.size == 1) {
              qualifiers.head
            } else {
              None
            }
          },
          None,
          None
        )
          // In set operations, if different column names are merged into one column, the first column name will be used
          .withAlias(head.name)
        col
      }.toSeq
    }
  }

  case class Intersect(
      relations: Seq[Relation],
      nodeLocation: Option[NodeLocation]
  ) extends SetOperation {
    override def children: Seq[Relation] = relations
    override def toString = {
      s"Intersect(${relations.mkString(", ")})"
    }
    override def sig(config: QuerySignatureConfig): String = {
      s"IX(${relations.map(_.sig(config)).mkString(",")})"
    }
    override def inputAttributes: Seq[Attribute] =
      relations.flatMap(_.inputAttributes)
  }

  case class Except(left: Relation, right: Relation, nodeLocation: Option[NodeLocation]) extends SetOperation {
    override def children: Seq[Relation] = Seq(left, right)
    override def toString = {
      s"Except(${left}, ${right})"
    }
    override def sig(config: QuerySignatureConfig): String = {
      s"EX(${left.sig(config)},${right.sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = left.inputAttributes
    override def outputAttributes: Seq[Attribute] = left.outputAttributes
  }

  case class Union(
      relations: Seq[Relation],
      nodeLocation: Option[NodeLocation]
  ) extends SetOperation {
    override def children: Seq[Relation] = relations
    override def toString = {
      s"Union(${relations.mkString(",")})"
    }
    override def sig(config: QuerySignatureConfig): String = {
      val in = relations.map(_.sig(config)).mkString(",")
      s"U(${in})"
    }
    override def inputAttributes: Seq[Attribute] = {
      relations.flatMap(_.inputAttributes)
    }
  }

  case class Unnest(columns: Seq[Expression], withOrdinality: Boolean, nodeLocation: Option[NodeLocation])
      extends Relation {
    override def children: Seq[LogicalPlan]      = Seq.empty
    override def inputAttributes: Seq[Attribute] = Seq.empty // TODO
    override def outputAttributes: Seq[Attribute] = {
      columns.map {
        case arr: ArrayConstructor =>
          ResolvedAttribute(UUID.randomUUID().toString, arr.elementType, None, None, None, None)
        case other =>
          SingleColumn(other, None, None, other.nodeLocation)
      }
    }
    override def sig(config: QuerySignatureConfig): String =
      s"Un[${columns.length}]"
    override def toString = {
      s"Unnest(withOrdinality:${withOrdinality}, ${columns.mkString(",")})"
    }
  }
  case class Lateral(query: Relation, nodeLocation: Option[NodeLocation]) extends UnaryRelation {
    override def child: Relation = query
    override def outputAttributes: Seq[Attribute] =
      query.outputAttributes // TODO
    override def sig(config: QuerySignatureConfig): String =
      s"Lt(${query.sig(config)})"
  }
  case class LateralView(
      child: Relation,
      exprs: Seq[Expression],
      tableAlias: Identifier,
      columnAliases: Seq[Identifier],
      nodeLocation: Option[NodeLocation]
  ) extends UnaryRelation {
    override def outputAttributes: Seq[Attribute] =
      columnAliases.map(x => UnresolvedAttribute(Some(tableAlias.value), x.value, None, None))
    override def sig(config: QuerySignatureConfig): String =
      s"LV(${child.sig(config)})"
  }

  /*
   * SQL statements for changing the table schema or catalog
   */
  sealed trait DDL extends LogicalPlan with LeafPlan with SQLSig {
    override def outputAttributes: Seq[Attribute] = Seq.empty
  }

  case class CreateSchema(
      schema: QName,
      ifNotExists: Boolean,
      properties: Option[Seq[SchemaProperty]],
      nodeLocation: Option[NodeLocation]
  ) extends DDL {
    override def sig(config: QuerySignatureConfig) = "CS"
  }

  case class DropSchema(schema: QName, ifExists: Boolean, cascade: Boolean, nodeLocation: Option[NodeLocation])
      extends DDL {
    override def sig(config: QuerySignatureConfig) = "DS"
  }

  case class RenameSchema(schema: QName, renameTo: Identifier, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig) = "RS"
  }
  case class CreateTable(
      table: QName,
      ifNotExists: Boolean,
      tableElems: Seq[TableElement],
      nodeLocation: Option[NodeLocation]
  ) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"CT(${TableRef(table, None).sig(config)})"
    }
  }
  case class DropTable(table: QName, ifExists: Boolean, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"DT(${TableRef(table, None).sig(config)})"
    }
  }
  case class RenameTable(table: QName, renameTo: QName, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"RT(${TableRef(table, None).sig(config)})"
    }
  }
  case class RenameColumn(table: QName, column: Identifier, renameTo: Identifier, nodeLocation: Option[NodeLocation])
      extends DDL {
    override def sig(config: QuerySignatureConfig) = "RC"
  }
  case class DropColumn(table: QName, column: Identifier, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig) = "DC"
  }
  case class AddColumn(table: QName, column: ColumnDef, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig) = "AC"
  }

  case class CreateView(viewName: QName, replace: Boolean, query: Relation, nodeLocation: Option[NodeLocation])
      extends DDL {
    override def sig(config: QuerySignatureConfig) = "CV"
  }
  case class DropView(viewName: QName, ifExists: Boolean, nodeLocation: Option[NodeLocation]) extends DDL {
    override def sig(config: QuerySignatureConfig) = "DV"
  }

  /**
    * A base trait for all update operations (e.g., add/delete the table contents).
    */
  trait Update extends LogicalPlan with SQLSig

  case class CreateTableAs(
      table: QName,
      ifNotEotExists: Boolean,
      columnAliases: Option[Seq[Identifier]],
      query: Relation,
      nodeLocation: Option[NodeLocation]
  ) extends DDL
      with Update
      with UnaryRelation {
    override def sig(config: QuerySignatureConfig) =
      s"CT(${TableRef(table, None).sig(config)},${query.sig(config)})"
    override def inputAttributes: Seq[Attribute]  = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = query.outputAttributes
    override def child: Relation                  = query
  }

  case class InsertInto(
      table: QName,
      columnAliases: Option[Seq[Identifier]],
      query: Relation,
      nodeLocation: Option[NodeLocation]
  ) extends Update
      with UnaryRelation {
    override def child: Relation = query
    override def sig(config: QuerySignatureConfig): String = {
      s"I(${TableRef(table, None).sig(config)},${query.sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = Nil
  }

  case class Delete(table: QName, where: Option[Expression], nodeLocation: Option[NodeLocation])
      extends Update
      with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      s"D(${TableRef(table, None).sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = Nil
    override def outputAttributes: Seq[Attribute] = Nil
  }

}
