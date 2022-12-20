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
import wvlet.airframe.sql.analyzer.RewriteRule.PlanRewriter
import wvlet.airframe.sql.model.Expression._
import wvlet.airframe.sql.model.LogicalPlan._
import wvlet.airframe.sql.model._
import wvlet.log.LogSupport

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  def typerRules: List[RewriteRule] = {
    // First resolve all input table types
    // CTE Table Refs must be resolved before resolving aggregation indexes
    TypeResolver.resolveCTETableRef ::
      TypeResolver.resolveAggregationIndexes ::
      TypeResolver.resolveAggregationKeys ::
      TypeResolver.resolveSortItemIndexes ::
      TypeResolver.resolveSortItems ::
      TypeResolver.resolveTableRef ::
      TypeResolver.resolveJoinUsing ::
      TypeResolver.resolveSubquery ::
      TypeResolver.resolveRegularRelation ::
      TypeResolver.resolveColumns ::
      Nil
  }

  def resolve(analyzerContext: AnalyzerContext, plan: LogicalPlan): LogicalPlan = {
    resolve(analyzerContext, plan, typerRules)
  }

  private[sql] def resolve(
      analyzerContext: AnalyzerContext,
      plan: LogicalPlan,
      rules: List[RewriteRule]
  ): LogicalPlan = {
    val resolvedPlan = rules
      .foldLeft(plan) { (targetPlan, rule) =>
        rule.transform(targetPlan, analyzerContext)
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
  object resolveAggregationIndexes extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
        var changed = false
        val resolvedGroupingKeys: List[GroupingKey] = groupingKeys.map {
          case k @ GroupingKey(LongLiteral(i, _), _) if i <= selectItems.length =>
            // Use a simpler form of attributes
            val keyItem = selectItems(i.toInt - 1) match {
              case SingleColumn(expr, _, _, _) =>
                expr
              case other =>
                other
            }
            changed = true
            GroupingKey(keyItem, k.nodeLocation)
          case other =>
            other
        }
        if (changed) {
          Aggregate(child, selectItems, resolvedGroupingKeys, having, a.nodeLocation)
        } else {
          a
        }
    }
  }

  /**
    * Resolve group by keys
    * @param context
    * @return
    */
  object resolveAggregationKeys extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
        val resolvedChild         = resolveRelation(context, child)
        val childOutputAttributes = resolvedChild.outputAttributes
        val resolvedGroupingKeys =
          groupingKeys.map(x => {
            val e = resolveExpression(context, x.child, childOutputAttributes)
            GroupingKey(e, e.nodeLocation)
          })
        val resolvedHaving = having.map {
          _.transformUpExpression { case x: Expression =>
            // Having recognize attributes only from the input relation
            resolveExpression(context, x, childOutputAttributes)
          }
        }
        Aggregate(resolvedChild, selectItems, resolvedGroupingKeys, resolvedHaving, a.nodeLocation)
    }
  }

  object resolveSortItemIndexes extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case s @ Sort(child, sortItems, _) =>
      val resolvedSortItems = sortItems.map {
        case sortItem @ SortItem(LongLiteral(i, _), _, _, _) =>
          val sortKey = child.outputAttributes(i.toInt - 1) match {
            case SingleColumn(expr, _, _, _) => expr
            case other                       => other
          }
          sortItem.copy(sortKey = sortKey)
        case other => other
      }
      s.copy(orderBy = resolvedSortItems)
    }
  }

  object resolveSortItems extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case s @ Sort(child, sortItems, _) =>
      val resolvedChild   = resolveRelation(context, child)
      val inputAttributes = resolvedChild.outputAttributes
      val resolvedSortItems = sortItems.map { sortItem =>
        val e = resolveExpression(context, sortItem.sortKey, inputAttributes)
        sortItem.copy(sortKey = e)
      }
      s.copy(orderBy = resolvedSortItems)
    }
  }

  /**
    * Resolve TableRefs in a query inside WITH statement with CTERelationRef
    * @param context
    * @return
    */
  object resolveCTETableRef extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case q @ Query(withQuery, body, _) =>
      CTEResolver.resolveCTE(context, q)
    }
  }

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  object resolveTableRef extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case plan @ LogicalPlan.TableRef(qname, _) =>
      context.catalog.findFromQName(context.database, qname) match {
        case Some(dbTable) =>
          trace(s"Found ${dbTable}")
          // Expand all table columns first, which will be pruned later by Optimizer
          TableScan(dbTable, dbTable.schema.columns, plan.nodeLocation)
        case None =>
          // Search CTE
          context.outerQueries.get(qname.fullName) match {
            case Some(cte) =>
              CTERelationRef(
                qname.fullName,
                cte.outputAttributes.map(_.withQualifier(qname.fullName)),
                plan.nodeLocation
              )
            case None =>
              throw SQLErrorCode.TableNotFound.newException(
                s"Table ${context.database}.${qname} not found",
                plan.nodeLocation
              )
          }
      }
    }
  }

  object resolveJoinUsing extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case j @ Join(joinType, left, right, u @ JoinUsing(joinKeys, _), _) =>
        // from A join B using(c1, c2, ...)
        val resolvedJoin =
          Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u, j.nodeLocation)
        val resolvedJoinKeys: Seq[Expression] = joinKeys.flatMap { k =>
          findMatchInInputAttributes(context, k, resolvedJoin.inputAttributes) match {
            case x if x.size < 2 =>
              throw SQLErrorCode.ColumnNotFound.newException(
                s"join key column: ${k.sqlExpr} is not found",
                k.nodeLocation
              )
            case other =>
              Seq(other.head, other.last)
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
  }

  object resolveSubquery extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case filter @ Filter(child, filterExpr, _) =>
      filter.transformUpExpressions {
        case q: InSubQuery         => q.copy(in = resolveRelation(context, q.in))
        case q: NotInSubQuery      => q.copy(in = resolveRelation(context, q.in))
        case q: SubQueryExpression => q.copy(query = resolveRelation(context, q.query))
      }
    }
  }

  object resolveRegularRelation extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case filter @ Filter(child, filterExpr, _) =>
        filter.transformUpExpressions { case x: Expression =>
          resolveExpression(context, x, filter.inputAttributes)
        }
      case u: Union     => u // UNION is resolved later by resolveUnion()
      case u: Intersect => u // INTERSECT is resolved later by resolveIntersect()
      case r: Relation =>
        r.transformUpExpressions { case x: Expression => resolveExpression(context, x, r.inputAttributes) }
    }
  }

  object resolveColumns extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case p @ Project(child, columns, _) =>
      val resolvedColumns = resolveOutputColumns(context, child.outputAttributes, columns)
      val resolved        = Project(child, resolvedColumns, p.nodeLocation)
      resolved
    }
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
//      case a: AllColumns =>
//        // TODO check (prefix).* to resolve attributes
//        resolvedColumns ++= inputAttributes
      case SingleColumn(expr, alias, qualifier, nodeLocation) =>
        resolveExpression(context, expr, inputAttributes) match {
          case s: SingleColumn =>
            resolvedColumns += s
          case r: ResolvedAttribute if alias.isEmpty =>
            resolvedColumns += r
          case r: ResolvedAttribute if alias.nonEmpty =>
            resolvedColumns += SingleColumn(r, alias, None, nodeLocation)
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
        r.withAlias(alias).withQualifier(qualifier)
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
    val resolvedAttributes = inputAttributes.map(resolveAttribute)

    def lookup(name: String): List[Expression] = {
      val resolvedExprs = List.newBuilder[Expression]
      ColumnPath.fromQName(context.database, name) match {
        case Some(columnPath) =>
          resolvedAttributes.foreach {
            case a: Attribute =>
              resolvedExprs ++= a.matched(columnPath)
            case _ =>
          }
        case None =>
      }
      resolvedExprs.result()
    }

    def toResolvedAttribute(name: String, expr: Expression): ResolvedAttribute = {
      ResolvedAttribute(
        name,
        expr.dataType,
        None,
        None,
        expr.nodeLocation
      )
    }

    val results = expr match {
      case i: Identifier =>
        lookup(i.value).map {
          // No need to resolve Attribute expressions
          case a: Attribute =>
            a
          case expr =>
            // Resolve expr as ResolvedAttribute so as not to pull-up too much details
            toResolvedAttribute(i.value, expr)
        }
      case u @ UnresolvedAttribute(name, _) =>
        lookup(name)
      case a @ AllColumns(_, None, _) =>
        // Resolve the inputs of AllColumn as ResolvedAttribute
        // so as not to pull up too much details
        val allColumns = resolvedAttributes.map {
          case s @ SingleColumn(m: MultiSourceColumn, alias, qualifier, _) =>
            // Pull-up MultiColumn to simplify the expression
            m.copy(alias = m.alias.orElse(s.alias), qualifier = qualifier)
          case m: MultiSourceColumn =>
            // MultiColumn is already resolved
            m
          case r: ResolvedAttribute =>
            // This path preserves already resolved column tags
            r
          case other =>
            toResolvedAttribute(other.name, other)
        }
        List(a.copy(columns = Some(allColumns)))
      case _ =>
        List(expr)
    }

    trace(s"findMatchInInputAttributes: ${expr}, inputAttributes: ${inputAttributes} -> ${results}")
    results
  }

  /**
    * Resolve untyped expressions
    */
  private def resolveExpression(
      context: AnalyzerContext,
      expr: Expression,
      inputAttributes: Seq[Attribute]
  ): Expression = {
    findMatchInInputAttributes(context, expr, inputAttributes) match {
      case lst if lst.length > 1 =>
        trace(s"${expr} -> ${lst}")
        throw SQLErrorCode.SyntaxError.newException(s"${expr.sqlExpr} is ambiguous", expr.nodeLocation)
      case lst =>
        lst.headOption.getOrElse(expr)
    }
  }
}
