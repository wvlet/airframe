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
package wvlet.airframe.sql.analyzer
import wvlet.airframe.sql.SQLErrorCode
import wvlet.airframe.sql.analyzer.SQLAnalyzer.{PlanRewriter, Rule}
import wvlet.airframe.sql.model.Expression._
import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Join, Project, Query, Relation, Union}
import wvlet.airframe.sql.model._
import wvlet.log.LogSupport

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  def typerRules: List[Rule] =
    // First resolve all input table types
    TypeResolver.resolveAggregationIndexes _ ::
      TypeResolver.resolveCTETableRef _ ::
      TypeResolver.resolveTableRef _ ::
      TypeResolver.resolveJoinUsing _ ::
      TypeResolver.resolveRegularRelation _ ::
      TypeResolver.resolveColumns _ ::
      TypeResolver.resolveUnion _ ::
      Nil

  def resolve(analyzerContext: AnalyzerContext, plan: LogicalPlan): LogicalPlan = {
    val resolvedPlan = TypeResolver.typerRules
      .foldLeft(plan.asInstanceOf[LogicalPlan]) { (targetPlan, rule) =>
        val r = rule.apply(analyzerContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }
    resolvedPlan
  }

  def resolveRelation(analyzerContext: AnalyzerContext, plan: LogicalPlan): Relation = {
    val resolvedPlan = resolve(analyzerContext, plan)
    resolvedPlan match {
      case r: Relation => r
      case other =>
        throw SQLErrorCode.InvalidArgument.newException(s"${plan} isn't a relation")
    }
  }

  /**
    * Translate select i1, i2, ... group by 1, 2, ... query into select i1, i2, ... group by i1, i2
    *
    * @param context
    * @return
    */
  def resolveAggregationIndexes(context: AnalyzerContext): PlanRewriter = {
    case a @ Aggregate(child, selectItems, groupingKeys, having) =>
      val resolvedGroupingKeys: List[GroupingKey] = groupingKeys.map {
        case GroupingKey(LongLiteral(i)) if i <= selectItems.length =>
          // Use a simpler form of attributes
          val keyItem = selectItems(i.toInt - 1) match {
            case SingleColumn(expr, alias) =>
              expr
            case other =>
              other
          }
          GroupingKey(keyItem)
        case other => other
      }
      Aggregate(child, selectItems, resolvedGroupingKeys, having)
  }

  /**
    * Resolve TableRefs in a query inside WITH statement with CTERelationRef
    * @param context
    * @return
    */
  def resolveCTETableRef(context: AnalyzerContext): PlanRewriter = { case q @ Query(withQuery, body) =>
    CTEResolver.resolveCTE(context, q)
  }

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  def resolveTableRef(context: AnalyzerContext): PlanRewriter = { case plan @ LogicalPlan.TableRef(qname) =>
    context.catalog.findFromQName(context.database, qname) match {
      case Some(dbTable) =>
        trace(s"Found ${dbTable}")
        // Expand all table columns first, which will be pruned later by Optimizer
        TableScan(dbTable, dbTable.schema.columns)
      case None =>
        // Search CTE
        context.outerQueries.get(qname.fullName) match {
          case Some(cte) =>
            CTERelationRef(qname.fullName, cte.outputAttributes)
          case None =>
            throw SQLErrorCode.TableNotFound.newException(s"Table ${context.database}.${qname} not found")
        }
    }
  }

  def resolveJoinUsing(context: AnalyzerContext): PlanRewriter = {
    case j @ Join(joinType, left, right, u @ JoinUsing(joinKeys)) =>
      // from A join B using(c1, c2, ...)
      val resolvedJoin = Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u)
      val resolvedJoinKeys: Seq[Expression] = joinKeys.flatMap { k =>
        findMatchInInputAttributes(k, resolvedJoin.inputAttributes) match {
          case Nil =>
            throw SQLErrorCode.ColumnNotFound.newException(s"join key column: ${k.sqlExpr} is not found")
          case other =>
            other
        }
      }
      val updated = j.withCond(JoinOnEq(resolvedJoinKeys))
      updated
    case j @ Join(joinType, left, right, u @ JoinOn(Eq(leftKey, rightKey))) =>
      val resolvedJoin = Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u)
      val resolvedJoinKeys: Seq[Expression] = Seq(leftKey, rightKey).flatMap { k =>
        findMatchInInputAttributes(k, resolvedJoin.inputAttributes) match {
          case Nil =>
            throw SQLErrorCode.ColumnNotFound.newException(s"join key column: ${k.sqlExpr} is not found")
          case other =>
            other
        }
      }
      val updated = j.withCond(JoinOnEq(resolvedJoinKeys))
      updated
  }

  def resolveRegularRelation(context: AnalyzerContext): PlanRewriter = {
    case filter @ Filter(child, filterExpr) =>
      filter.transformExpressions { case x: Expression => resolveExpression(x, filter.inputAttributes) }
    case r: Relation =>
      r.transformExpressions { case x: Expression => resolveExpression(x, r.inputAttributes) }
  }

  def resolveUnion(context: AnalyzerContext): PlanRewriter = {
    // TODO: merge union columns
    case u @ Union(rels) =>
      u
  }

  def resolveColumns(context: AnalyzerContext): PlanRewriter = { case p @ Project(child, columns) =>
    val resolvedColumns = resolveOutputColumns(child.outputAttributes, columns)
    Project(child, resolvedColumns)
  }

  private def resolveOutputColumns(inputAttributes: Seq[Attribute], outputColumns: Seq[Attribute]): Seq[Attribute] = {
    val resolvedColumns = Seq.newBuilder[Attribute]
    outputColumns.map {
      case a: AllColumns =>
        // TODO check (prefix).* to resolve attributes
        resolvedColumns ++= inputAttributes
      case SingleColumn(expr, alias) =>
        resolveExpression(expr, inputAttributes) match {
          case r: ResolvedAttribute if alias.isEmpty =>
            resolvedColumns += r
          case r: ResolvedAttribute if alias.nonEmpty =>
            resolvedColumns += ResolvedAttribute(alias.get.sqlExpr, r.dataType, r.sourceTable, r.sourceColumn)
          case expr =>
            resolvedColumns += SingleColumn(expr, alias)
        }
      case other =>
        resolvedColumns += other
    }
    resolvedColumns.result()
  }

  def resolveAttribute(attribute: Attribute): Attribute = {
    attribute match {
      case SingleColumn(r: ResolvedAttribute, None) =>
        r
      case SingleColumn(r: ResolvedAttribute, Some(alias: Identifier)) =>
        r.withAlias(alias.value)
      case other => other
    }
  }

  /**
    * Find matching expressions in the inputAttributes
    * @param expr
    * @param inputAttributes
    * @return
    */
  def findMatchInInputAttributes(expr: Expression, inputAttributes: Seq[Attribute]): List[Expression] = {
    def lookup(name: String): List[Attribute] = {
      QName(name) match {
        case QName(Seq(t1, c1)) =>
          val attrs = inputAttributes.collect {
            case a @ ResolvedAttribute(c, _, Some(t), _) if t.name == t1 && c == c1 => a
            case a @ ResolvedAttribute(c, _, None, _) if c == c1                    => a
          }
          attrs.toList
        case QName(Seq(c1)) =>
          val attrs = inputAttributes.collect {
            case a @ ResolvedAttribute(c, _, _, _) if c == c1 => a
          }
          attrs.toList
        case _ =>
          List.empty
      }
    }

    expr match {
      case i: Identifier =>
        lookup(i.value)
      case u @ UnresolvedAttribute(name) =>
        lookup(name)
      case _ =>
        List(expr)
    }
  }

  /**
    * Resolve untyped expressions
    */
  def resolveExpression(expr: Expression, inputAttributes: Seq[Attribute]): Expression = {
    findMatchInInputAttributes(expr, inputAttributes) match {
      case lst if lst.length > 1 =>
        throw SQLErrorCode.SyntaxError.newException(s"${expr.sqlExpr} is ambiguous")
      case lst =>
        lst.headOption.getOrElse(expr)
    }
  }

}
