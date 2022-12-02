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
import wvlet.airframe.sql.model.LogicalPlan._
import wvlet.airframe.sql.model._
import wvlet.log.LogSupport

import scala.util.chaining.scalaUtilChainingOps

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  def typerRules: List[Rule] = {
    // First resolve all input table types
    // CTE Table Refs must be resolved before resolving aggregation indexes
    TypeResolver.resolveCTETableRef _ ::
      TypeResolver.resolveAggregationIndexes _ ::
      TypeResolver.resolveAggregationKeys _ ::
      TypeResolver.resolveTableRef _ ::
      TypeResolver.resolveJoinUsing _ ::
      TypeResolver.resolveRegularRelation _ ::
      TypeResolver.resolveColumns _ ::
      TypeResolver.resolveUnion _ ::
      TypeResolver.resolveIntersect _ ::
      Nil
  }

  def resolve(analyzerContext: AnalyzerContext, plan: LogicalPlan): LogicalPlan = {
    resolve(analyzerContext, plan, typerRules)
  }

  private[sql] def resolve(analyzerContext: AnalyzerContext, plan: LogicalPlan, rules: List[Rule]): LogicalPlan = {
    val resolvedPlan = rules
      .foldLeft(plan) { (targetPlan, rule) =>
        val r = rule.apply(analyzerContext)
        // Recursively transform the tree form bottom to up
        val resolved = targetPlan.transformUp(r)
        resolved
      }
    resolvedPlan
  }

  /**
    * An entry point of TypeResolver for transforming a Relation to Relation
    * @param analyzerContext
    * @param plan
    * @return
    */
  def resolveRelation(analyzerContext: AnalyzerContext, plan: LogicalPlan): Relation = {
    val resolvedPlan = resolve(analyzerContext, plan)
    resolvedPlan match {
      case r: Relation =>
        r
      case other =>
        throw SQLErrorCode.InvalidArgument.newException(s"${plan} isn't a relation", plan.nodeLocation)
    }
  }

  /**
    * Translate select i1, i2, ... group by 1, 2, ... query into select i1, i2, ... group by i1, i2
    *
    * @param context
    * @return
    */
  def resolveAggregationIndexes(context: AnalyzerContext): PlanRewriter = {
    case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
      val resolvedGroupingKeys: List[GroupingKey] = groupingKeys.map {
        case k @ GroupingKey(LongLiteral(i, _), _) if i <= selectItems.length =>
          // Use a simpler form of attributes
          val keyItem = selectItems(i.toInt - 1) match {
            case SingleColumn(expr, _, _, _) =>
              expr
            case other =>
              other
          }
          GroupingKey(keyItem, k.nodeLocation)
        case other =>
          other
      }
      Aggregate(child, selectItems, resolvedGroupingKeys, having, a.nodeLocation)
  }

  /**
    * Resolve group by keys
    * @param context
    * @return
    */
  def resolveAggregationKeys(context: AnalyzerContext): PlanRewriter = {
    case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
      val resolvedChild   = resolveRelation(context, child)
      val inputAttributes = resolvedChild.outputAttributes
      val resolvedGroupingKeys =
        groupingKeys.map(x => {
          val e = resolveExpression(context, x.child, inputAttributes)
          GroupingKey(e, e.nodeLocation)
        })
      Aggregate(resolvedChild, selectItems, resolvedGroupingKeys, having, a.nodeLocation)
  }

  /**
    * Resolve TableRefs in a query inside WITH statement with CTERelationRef
    * @param context
    * @return
    */
  def resolveCTETableRef(context: AnalyzerContext): PlanRewriter = { case q @ Query(withQuery, body, _) =>
    CTEResolver.resolveCTE(context, q)
  }

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  def resolveTableRef(context: AnalyzerContext): PlanRewriter = { case plan @ LogicalPlan.TableRef(qname, _) =>
    context.catalog.findFromQName(context.database, qname) match {
      case Some(dbTable) =>
        trace(s"Found ${dbTable}")
        // Expand all table columns first, which will be pruned later by Optimizer
        TableScan(dbTable, dbTable.schema.columns, plan.nodeLocation)
      case None =>
        // Search CTE
        context.outerQueries.get(qname.fullName) match {
          case Some(cte) =>
            CTERelationRef(qname.fullName, cte.outputAttributes.map(_.withQualifier(qname.fullName)), plan.nodeLocation)
          case None =>
            throw SQLErrorCode.TableNotFound.newException(
              s"Table ${context.database}.${qname} not found",
              plan.nodeLocation
            )
        }
    }
  }

  def resolveJoinUsing(context: AnalyzerContext): PlanRewriter = {
    case j @ Join(joinType, left, right, u @ JoinUsing(joinKeys, _), _) =>
      // from A join B using(c1, c2, ...)
      val resolvedJoin =
        Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u, j.nodeLocation)
      val resolvedJoinKeys: Seq[Expression] = joinKeys.flatMap { k =>
        findMatchInInputAttributes(context, k, resolvedJoin.inputAttributes) match {
          case Nil =>
            throw SQLErrorCode.ColumnNotFound.newException(
              s"join key column: ${k.sqlExpr} is not found",
              k.nodeLocation
            )
          case other =>
            other
        }
      }
      val updated = resolvedJoin.withCond(JoinOnEq(resolvedJoinKeys, u.nodeLocation))
      updated
    case j @ Join(joinType, left, right, u @ JoinOn(Eq(leftKey, rightKey, _), _), _) =>
      val resolvedJoin =
        Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u, j.nodeLocation)
      val resolvedJoinKeys: Seq[Expression] = Seq(leftKey, rightKey).flatMap { k =>
        findMatchInInputAttributes(context, k, resolvedJoin.inputAttributes) match {
          case Nil =>
            throw SQLErrorCode.ColumnNotFound.newException(
              s"join key column: ${k.sqlExpr} is not found",
              k.nodeLocation
            )
          case other =>
            other
        }
      }
      val updated = resolvedJoin.withCond(JoinOnEq(resolvedJoinKeys, u.expr.nodeLocation))
      updated
  }

  def resolveRegularRelation(context: AnalyzerContext): PlanRewriter = {
    case filter @ Filter(child, filterExpr, _) =>
      filter.transformExpressions { case x: Expression => resolveExpression(context, x, filter.inputAttributes) }
    case u: Union     => u // UNION is resolved later by resolveUnion()
    case u: Intersect => u // INTERSECT is resolved later by resolveIntersect()
    case r: Relation =>
      r.transformExpressions { case x: Expression => resolveExpression(context, x, r.inputAttributes) }
  }

  def resolveColumns(context: AnalyzerContext): PlanRewriter = { case p @ Project(child, columns, _) =>
    val resolvedColumns = resolveOutputColumns(context, child.outputAttributes, columns)
    val resolved        = Project(child, resolvedColumns, p.nodeLocation)
    resolved
  }

  def resolveIntersect(context: AnalyzerContext): PlanRewriter = { case u @ Intersect(_, None, _) =>
    val resolvedOutputs = u.outputAttributes.collect { case SingleColumn(UnionColumn(inputs, _), _, _, _) =>
      val resolved = inputs
        .map { expr =>
          resolveExpression(context, expr, u.inputAttributes)
        }.collect { case a: ResolvedAttribute => a }
      resolved.head.copy(sourceColumns = resolved.flatMap(_.sourceColumns))
    }
    u.copy(resolvedOutputs = Some(resolvedOutputs))
  }

  def resolveUnion(context: AnalyzerContext): PlanRewriter = { case u @ Union(_, None, _) =>
    val resolvedOutputs = u.outputAttributes.collect { case SingleColumn(UnionColumn(inputs, _), _, _, _) =>
      val resolved = inputs
        .map { expr =>
          resolveExpression(context, expr, u.inputAttributes)
        }.collect { case a: ResolvedAttribute => a }
      resolved.head.copy(sourceColumns = resolved.flatMap(_.sourceColumns))
    }
    u.copy(resolvedOutputs = Some(resolvedOutputs))
  }

  /**
    * Resolve output columns by looking up the inputAttributes
    *
    * @param inputAttributes
    * @param outputColumns
    * @return
    */
  private def resolveOutputColumns(
      context: AnalyzerContext,
      inputAttributes: Seq[Attribute],
      outputColumns: Seq[Attribute]
  ): Seq[Attribute] = {

    val resolvedColumns = Seq.newBuilder[Attribute]
    outputColumns.map {
      case a: AllColumns =>
        // TODO check (prefix).* to resolve attributes
        resolvedColumns ++= inputAttributes
      case SingleColumn(expr, alias, _, nodeLocation) =>
        resolveExpression(context, expr, inputAttributes) match {
          case r: ResolvedAttribute if alias.isEmpty =>
            resolvedColumns += r
          case r: ResolvedAttribute if alias.nonEmpty =>
            resolvedColumns += ResolvedAttribute(
              alias.get,
              r.dataType,
              r.qualifier,
              r.sourceColumns,
              r.nodeLocation
            )
          case expr =>
            resolvedColumns += SingleColumn(expr, alias, None, nodeLocation)
        }
      case other =>
        resolvedColumns += other
    }
    val output = resolvedColumns.result()
    output
  }

  def resolveAttribute(attribute: Attribute): Attribute = {
    attribute match {
      case SingleColumn(r: ResolvedAttribute, alias, qualifier, _) =>
        // Preserve column alias and qualifiers
        r.pipe { attr =>
          alias match {
            case Some(x) => attr.withAlias(x)
            case None    => attr
          }
        }.pipe { attr =>
            qualifier match {
              case Some(x) => attr.withQualifier(x)
              case None    => attr
            }
          }
      case other => other
    }
  }

  /**
    * Find matching expressions in the inputAttributes
    * @param expr
    * @param inputAttributes
    * @return
    */
  private def findMatchInInputAttributes(
      context: AnalyzerContext,
      expr: Expression,
      inputAttributes: Seq[Attribute]
  ): List[Expression] = {
    trace(s"findMatchInInputAttributes: ${expr}, inputAttributes: ${inputAttributes}")
    val resolvedAttributes = inputAttributes.map(resolveAttribute)

    def lookup(name: String): List[Expression] = {
      QName(name, None) match {
        case QName(Seq(db, t1, c1), _) if context.database == db =>
          resolvedAttributes.collect {
            case a: ResolvedAttribute if a.matchesWith(t1, c1) => a.ofSourceColumn(t1, c1).getOrElse(a)
            case c: SingleColumn if c.qualifier.contains(t1) && c.alias.contains(c1) => c.expr
          }.toList
        case QName(Seq(t1, c1), _) =>
          resolvedAttributes.collect {
            case a: ResolvedAttribute if a.matchesWith(t1, c1) => a.ofSourceColumn(t1, c1).getOrElse(a)
            case c: SingleColumn if c.qualifier.contains(t1) && c.alias.contains(c1) => c.expr
          }.toList
        case QName(Seq(c1), _) =>
          resolvedAttributes.collect {
            case a: ResolvedAttribute if a.name == c1    => a
            case c: SingleColumn if c.alias.contains(c1) => c.expr
          }.toList
        case _ =>
          List.empty
      }
    }

    expr match {
      case i: Identifier =>
        lookup(i.value)
      case u @ UnresolvedAttribute(name, _) =>
        lookup(name)
      case a @ AllColumns(_, None, _) =>
        val sourceTables = inputAttributes
          .collect { case r: ResolvedAttribute =>
            r.sourceColumns.map(_.table)
          }.flatten.distinct
        List(a.copy(sourceTables = Some(sourceTables)))
      case _ =>
        List(expr)
    }
  }

  /**
    * Resolve untyped expressions
    */
  def resolveExpression(context: AnalyzerContext, expr: Expression, inputAttributes: Seq[Attribute]): Expression = {
    findMatchInInputAttributes(context, expr, inputAttributes) match {
      case lst if lst.length > 1 =>
        trace(s"${expr} -> ${lst}")
        throw SQLErrorCode.SyntaxError.newException(s"${expr.sqlExpr} is ambiguous", expr.nodeLocation)
      case lst =>
        lst.headOption.getOrElse(expr)
    }
  }

}
