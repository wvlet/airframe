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
import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Project, Query, Relation, Union}
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
      TypeResolver.resolveRelation _ ::
      TypeResolver.resolveColumns _ ::
      TypeResolver.resolveUnion _ ::
      Nil

  def resolve(analyzerContext: AnalyzerContext, plan: LogicalPlan): LogicalPlan = {
    val resolvedPlan = TypeResolver.typerRules
      .foldLeft(plan) { (targetPlan, rule) =>
        val r = rule.apply(analyzerContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }
    resolvedPlan
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
    val newPlan = CTEResolver.resolveCTE(context, q)
    warn(newPlan.pp)
    newPlan
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

  def resolveRelation(context: AnalyzerContext): PlanRewriter = {
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
    val inputAttributes = child.outputAttributes
    val resolvedColumns = Seq.newBuilder[Attribute]
    columns.map {
      case a: AllColumns =>
        // TODO check (prefix).* to resolve attributes
        resolvedColumns ++= inputAttributes
      case SingleColumn(expr, alias) =>
        resolveExpression(expr, inputAttributes) match {
          case r: ResolvedAttribute if alias.isEmpty =>
            resolvedColumns += r
          case r: ResolvedAttribute if alias.nonEmpty =>
            resolvedColumns += ResolvedAttribute(alias.get.sqlExpr, r.dataType, None, None)
          case expr =>
            resolvedColumns += SingleColumn(expr, alias)
        }
      case other =>
        resolvedColumns += other
    }

    Project(child, resolvedColumns.result())
  }

  /**
    * Resolve untyped expressions
    */
  def resolveExpression(expr: Expression, inputAttributes: Seq[Attribute]): Expression = {
    def findInputAttribute(name: String): Option[Attribute] = {
      QName(name) match {
        case QName(Seq(t1, c1)) =>
          val attrs = inputAttributes.collect {
            case a @ ResolvedAttribute(c, _, Some(t), _) if t.name == t1 && c == c1 => a
            case a @ ResolvedAttribute(c, _, None, _) if c == c1                    => a
          }
          if (attrs.size > 1) {
            throw SQLErrorCode.SyntaxError.newException(s"${name} is ambiguous")
          }
          attrs.headOption
        case QName(Seq(c1)) =>
          val attrs = inputAttributes.collect {
            case a @ ResolvedAttribute(c, _, _, _) if c == c1 => a
          }
          if (attrs.size > 1) {
            throw SQLErrorCode.SyntaxError.newException(s"${name} is ambiguous")
          }
          attrs.headOption
        case _ =>
          None
      }
    }

    expr match {
      case i: Identifier =>
        findInputAttribute(i.value).getOrElse(i)
      case u @ UnresolvedAttribute(name) =>
        findInputAttribute(name).getOrElse(u)
      case _ =>
        expr
    }
  }

}
