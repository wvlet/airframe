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
import wvlet.airframe.sql.analyzer.QuerySignatureConfig

trait LogicalPlan extends TreeNode[LogicalPlan] with Product with SQLSig {
  def modelName: String = {
    val n = this.getClass.getSimpleName
    if (n.endsWith("$")) n.substring(0, n.length - 1) else n
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
    * Expressions associated to this LogicalPlan node
    *
    * @return
    */
  def expressions: Seq[Expression] = {
    def collectExpression(x: Any): Seq[Expression] = {
      x match {
        case e: Expression  => e :: Nil
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
    def recursiveTransform(arg: Any): AnyRef =
      arg match {
        case e: Expression => e
        case l: LogicalPlan => {
          val newPlan = f(l)
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

  def transform(rule: PartialFunction[LogicalPlan, LogicalPlan]): LogicalPlan = {
    val newNode: LogicalPlan = rule.applyOrElse(this, identity[LogicalPlan])
    if (newNode.eq(this)) {
      mapChildren(_.transform(rule))
    } else {
      newNode.mapChildren(_.transform(rule))
    }
  }

  def transformExpressions(rule: PartialFunction[Expression, Expression]): LogicalPlan = {
    def recursiveTransform(arg: Any): AnyRef =
      arg match {
        case e: Expression  => e.transformExpression(rule)
        case l: LogicalPlan => l.transformExpressions(rule)
        case Some(x)        => Some(recursiveTransform(x))
        case s: Seq[_]      => s.map(recursiveTransform _)
        case other: AnyRef  => other
        case null           => null
      }

    val newArgs = productIterator.map(recursiveTransform).toIndexedSeq
    copyInstance(newArgs)
  }

  protected def copyInstance(newArgs: Seq[AnyRef]): this.type = {
    val primaryConstructor = this.getClass.getDeclaredConstructors()(0)
    val newObj             = primaryConstructor.newInstance(newArgs: _*)
    newObj.asInstanceOf[this.type]
  }

  def collectExpressions: List[Expression] = {
    def recursiveCollect(arg: Any): List[Expression] =
      arg match {
        case e: Expression  => e :: e.collectSubExpressions
        case l: LogicalPlan => l.collectExpressions
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

    productIterator.foreach(recursiveTraverse)
  }

  // Input attributes (column names) of the relation
  def inputAttributes: Seq[Attribute]
  // Output attributes (column names) of the relation
  def outputAttributes: Seq[Attribute]

  // True if all input attributes are resolved.
  lazy val resolved: Boolean    = expressions.forall(_.resolved) && resolvedChildren
  def resolvedChildren: Boolean = children.forall(_.resolved)
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
  import Expression._

  private def isSelectAll(selectItems: Seq[Attribute]): Boolean = {
    selectItems.exists {
      case AllColumns(x) => true
      case _             => false
    }
  }

  // Relational operator
  trait Relation extends LogicalPlan with SQLSig

  // A relation that takes a single input relation
  sealed trait UnaryRelation extends Relation with UnaryPlan {
    def inputRelation: Relation = child
    override def child: Relation
  }

  case class ParenthesizedRelation(child: Relation) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String = child.sig(config)
    override def inputAttributes: Seq[Attribute]           = child.inputAttributes
    override def outputAttributes: Seq[Attribute]          = child.outputAttributes
  }
  case class AliasedRelation(child: Relation, alias: Identifier, columnNames: Option[Seq[String]])
      extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String = child.sig(config)

    override def inputAttributes: Seq[Attribute]  = child.inputAttributes
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Values(rows: Seq[Expression]) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      s"V[${rows.length}]"
    }
    override def outputAttributes: Seq[Attribute] =
      (0 until rows.size).map(x => UnresolvedAttribute(s"i${x}"))
  }

  case class TableRef(name: QName) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      if (config.embedTableNames) {
        name.toString
      } else {
        "T"
      }
    }
    override def outputAttributes: Seq[Attribute] = Nil
    override lazy val resolved: Boolean           = false
  }
  case class RawSQL(sql: String) extends Relation with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = "Q"
    override def outputAttributes: Seq[Attribute]          = Nil
  }

  // Deduplicate (duplicate elimination) the input releation
  case class Distinct(child: Relation) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"E(${child.sig(config)})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Sort(child: Relation, orderBy: Seq[SortItem]) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"O[${orderBy.length}](${child.sig(config)})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Limit(child: Relation, limit: LongLiteral) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"L(${child.sig(config)})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case class Filter(child: Relation, filterExpr: Expression) extends UnaryRelation {
    override def sig(config: QuerySignatureConfig): String =
      s"F(${child.sig(config)})"
    override def outputAttributes: Seq[Attribute] = child.outputAttributes
  }

  case object EmptyRelation extends Relation with LeafPlan {
    // Need to override this method so as not to create duplicate case object instances
    override def copyInstance(newArgs: Seq[AnyRef]) = this
    override def sig(config: QuerySignatureConfig)  = ""
    override def outputAttributes: Seq[Attribute]   = Nil
  }

// This node can be a pivot node for generating a SELECT statament
  sealed trait Selection extends UnaryRelation {
    def selectItems: Seq[Attribute]
  }

  case class Project(child: Relation, selectItems: Seq[Attribute]) extends UnaryRelation with Selection {
    override def sig(config: QuerySignatureConfig): String = {
      val proj =
        if (LogicalPlan.isSelectAll(selectItems)) "*"
        else s"${selectItems.length}"
      s"P[${proj}](${child.sig(config)})"
    }

    // TODO
    override def outputAttributes: Seq[Attribute] = {
      selectItems
    }
  }

  case class Aggregate(
      child: Relation,
      selectItems: Seq[Attribute],
      groupingKeys: Seq[GroupingKey],
      having: Option[Expression]
  ) extends UnaryRelation
      with Selection {
    override def sig(config: QuerySignatureConfig): String = {
      val proj =
        if (LogicalPlan.isSelectAll(selectItems)) "*"
        else s"${selectItems.length}"
      s"A[${proj},${groupingKeys.length}](${child.sig(config)})"
    }

    override def toString =
      s"Aggregate[${groupingKeys.mkString(",")}](Select[${selectItems.mkString(", ")}(${child})"

    override def outputAttributes: Seq[Attribute] = selectItems
  }

  case class Query(withQuery: With, body: Relation) extends Relation {
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
    override def inputAttributes: Seq[Attribute]  = body.inputAttributes
    override def outputAttributes: Seq[Attribute] = body.outputAttributes
  }
  case class With(recursive: Boolean, queries: Seq[WithQuery]) extends LogicalPlan {
    override def sig(config: QuerySignatureConfig) = ""
    override def children: Seq[LogicalPlan]        = queries
    override def inputAttributes: Seq[Attribute]   = ???
    override def outputAttributes: Seq[Attribute]  = ???
  }
  case class WithQuery(name: Identifier, query: Relation, columnNames: Option[Seq[Identifier]])
      extends LogicalPlan
      with UnaryPlan {
    override def sig(config: QuerySignatureConfig) = ""
    override def child: LogicalPlan                = query
    override def inputAttributes: Seq[Attribute]   = query.inputAttributes
    override def outputAttributes: Seq[Attribute]  = query.outputAttributes
  }

// Joins
  case class Join(joinType: JoinType, left: Relation, right: Relation, cond: JoinCriteria) extends Relation {
    override def modelName: String          = joinType.toString
    override def children: Seq[LogicalPlan] = Seq(left, right)
    override def sig(config: QuerySignatureConfig): String = {
      s"${joinType.symbol}(${left.sig(config)},${right.sig(config)})"
    }
    override def inputAttributes: Seq[Attribute] =
      left.outputAttributes ++ right.outputAttributes
    override def outputAttributes: Seq[Attribute] = inputAttributes
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

  sealed trait SetOperation extends Relation {
    override def children: Seq[Relation]
  }
  case class Intersect(relations: Seq[Relation]) extends SetOperation {
    override def children: Seq[Relation] = relations
    override def sig(config: QuerySignatureConfig): String = {
      s"IX(${relations.map(_.sig(config)).mkString(",")})"
    }
    override def inputAttributes: Seq[Attribute] =
      relations.head.inputAttributes
    override def outputAttributes: Seq[Attribute] =
      relations.head.outputAttributes
  }
  case class Except(left: Relation, right: Relation) extends SetOperation {
    override def children: Seq[Relation] = Seq(left, right)
    override def sig(config: QuerySignatureConfig): String = {
      s"EX(${left.sig(config)},${right.sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = left.inputAttributes
    override def outputAttributes: Seq[Attribute] = left.outputAttributes
  }
  case class Union(relations: Seq[Relation]) extends SetOperation {
    override def children: Seq[Relation] = relations
    override def toString = {
      s"Union(${relations.mkString(",")})"
    }
    override def sig(config: QuerySignatureConfig): String = {
      val in = relations.map(_.sig(config)).mkString(",")
      s"U(${in})"
    }
    override def inputAttributes: Seq[Attribute] =
      relations.head.inputAttributes
    override def outputAttributes: Seq[Attribute] =
      relations.head.outputAttributes
  }

  case class Unnest(columns: Seq[Expression], withOrdinality: Boolean) extends Relation {
    override def children: Seq[LogicalPlan]       = Seq.empty
    override def inputAttributes: Seq[Attribute]  = Seq.empty // TODO
    override def outputAttributes: Seq[Attribute] = Seq.empty // TODO
    override def sig(config: QuerySignatureConfig): String =
      s"Un[${columns.length}]"
  }
  case class Lateral(query: Relation) extends UnaryRelation {
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
      columnAliases: Seq[Identifier]
  ) extends UnaryRelation {
    override def outputAttributes: Seq[Attribute] =
      columnAliases.map(x => UnresolvedAttribute(x.value))
    override def sig(config: QuerySignatureConfig): String =
      s"LV(${child.sig(config)})"
  }

  /*
   * SQL statements for changing the table schema or catalog
   */
  sealed trait DDL extends LogicalPlan with LeafPlan with SQLSig {
    override def outputAttributes: Seq[Attribute] = Seq.empty
  }

  case class CreateSchema(schema: QName, ifNotExists: Boolean, properties: Option[Seq[SchemaProperty]]) extends DDL {
    override def sig(config: QuerySignatureConfig) = "CS"
  }

  case class DropSchema(schema: QName, ifExists: Boolean, cascade: Boolean) extends DDL {
    override def sig(config: QuerySignatureConfig) = "DS"
  }

  case class RenameSchema(schema: QName, renameTo: Identifier) extends DDL {
    override def sig(config: QuerySignatureConfig) = "RS"
  }
  case class CreateTable(table: QName, ifNotExists: Boolean, tableElems: Seq[TableElement]) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"CT(${TableRef(table).sig(config)})"
    }
  }
  case class DropTable(table: QName, ifExists: Boolean) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"DT(${TableRef(table).sig(config)})"
    }
  }
  case class RenameTable(table: QName, renameTo: QName) extends DDL {
    override def sig(config: QuerySignatureConfig): String = {
      s"RT(${TableRef(table).sig(config)})"
    }
  }
  case class RenameColumn(table: QName, column: Identifier, renameTo: Identifier) extends DDL {
    override def sig(config: QuerySignatureConfig) = "RC"
  }
  case class DropColumn(table: QName, column: Identifier) extends DDL {
    override def sig(config: QuerySignatureConfig) = "DC"
  }
  case class AddColumn(table: QName, column: ColumnDef) extends DDL {
    override def sig(config: QuerySignatureConfig) = "AC"
  }

  case class CreateView(viewName: QName, replace: Boolean, query: Relation) extends DDL {
    override def sig(config: QuerySignatureConfig) = "CV"
  }
  case class DropView(viewName: QName, ifExists: Boolean) extends DDL {
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
      query: Relation
  ) extends DDL
      with Update
      with UnaryRelation {
    override def sig(config: QuerySignatureConfig) =
      s"CT(${TableRef(table).sig(config)},${query.sig(config)})"
    override def inputAttributes: Seq[Attribute]  = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = query.outputAttributes
    override def child: Relation                  = query
  }

  case class InsertInto(table: QName, columnAliases: Option[Seq[Identifier]], query: Relation)
      extends Update
      with UnaryRelation {
    override def child: Relation = query
    override def sig(config: QuerySignatureConfig): String = {
      s"I(${TableRef(table).sig(config)},${query.sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = query.inputAttributes
    override def outputAttributes: Seq[Attribute] = Nil
  }

  case class Delete(table: QName, where: Option[Expression]) extends Update with LeafPlan {
    override def sig(config: QuerySignatureConfig): String = {
      s"D(${TableRef(table).sig(config)})"
    }
    override def inputAttributes: Seq[Attribute]  = Nil
    override def outputAttributes: Seq[Attribute] = Nil
  }

}
