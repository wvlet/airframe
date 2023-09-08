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
import wvlet.airframe.sql.{SQLError, SQLErrorCode}
import wvlet.airframe.sql.analyzer.RewriteRule.PlanRewriter
import wvlet.airframe.sql.model.Expression.*
import wvlet.airframe.sql.model.LogicalPlan.*
import wvlet.airframe.sql.model.*
import wvlet.log.LogSupport

import scala.annotation.tailrec

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  def preProcessingRules: List[RewriteRule] = {
    unresolveAllColumns ::
      Nil
  }

  def typerRules: List[RewriteRule] = {
    // CTE Table Refs must be resolved before resolving aggregation indexes
    resolveCTETableRef ::
      // Resolve all input table types
      resolveTableRef ::
      resolveJoinUsing ::
      resolveSubquery ::
      resolveRegularRelation ::
      resolveColumns ::
      resolveAggregationIndexes ::
      resolveAggregationKeys ::
      resolveSortItemIndexes ::
      resolveSortItems ::
      Nil
  }

  def resolve(
      analyzerContext: AnalyzerContext,
      plan: LogicalPlan,
      rules: List[RewriteRule] = typerRules,
      preProcessingRules: List[RewriteRule] = preProcessingRules
  ): LogicalPlan = {
    val resolvedPlan = (preProcessingRules ::: rules)
      .foldLeft(plan) { (targetPlan, rule) =>
        try {
          rule.transform(targetPlan, analyzerContext)
        } catch {
          case e: SQLError =>
            debug(s"Failed to resolve with: ${rule.name}\n${targetPlan.pp}")
            throw e
        }
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
    val resolvedPlan = resolve(analyzerContext, plan, typerRules, Nil)
    resolvedPlan match {
      case r: Relation =>
        r
      case other =>
        throw SQLErrorCode.InvalidArgument.newException(s"${plan} isn't a relation", plan.nodeLocation)
    }
  }

  /**
    * Unresolve previously resolved AllColumns to handle cases when sub queries are rewritten after AllColumns are
    * resolved
    */
  object unresolveAllColumns extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case r: Relation =>
      r.transformExpressions {
        case a: AllColumns if a.columns.nonEmpty =>
          a.copy(columns = None)
      }
    }
  }

  /**
    * Translate select i1, i2, ... group by 1, 2, ... query into select i1, i2, ... group by i1, i2
    */
  object resolveAggregationIndexes extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
        var changed = false
        val resolvedGroupingKeys: List[GroupingKey] = groupingKeys.map {
          case k @ UnresolvedGroupingKey(LongLiteral(i, _), _) if i <= selectItems.length =>
            // Use a simpler form of attributes
            val keyItem = resolveIndex(i.toInt - 1, selectItems)
            changed = true
            ResolvedGroupingKey(Some(i.toInt), keyItem, k.nodeLocation)
          case r: ResolvedGroupingKey =>
            r
          case other =>
            ResolvedGroupingKey(None, other.child, other.nodeLocation)
        }
        if (changed) {
          Aggregate(child, selectItems, resolvedGroupingKeys, having, a.nodeLocation)
        } else {
          a
        }
    }

    private def resolveIndex(index: Int, inputs: Seq[Attribute]): Expression = {
      inputs(index) match {
        case a: AllColumns =>
          resolveIndex(index, a.inputColumns)
        case SingleColumn(expr, _, _, _) =>
          expr
        case Alias(_, _, expr, _, _) =>
          expr
        case other =>
          other
      }
    }
  }

  /**
    * Resolve group by keys
    */
  object resolveAggregationKeys extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = {
      case a @ Aggregate(child, selectItems, groupingKeys, having, _) =>
        val resolvedChild       = resolveRelation(context, child)
        val resolvedSelectItems = resolveOutputColumns(context, resolvedChild.outputAttributes, selectItems)
        val resolvedGroupingKeys =
          groupingKeys.map { k =>
            val e = resolveExpression(context, k.child, resolvedSelectItems)
            ResolvedGroupingKey(k.index, e, e.nodeLocation)
          }
        val resolvedHaving = having.map {
          _.transformUpExpression { case x: Expression =>
            // Having recognize attributes only from the input relation
            resolveExpression(context, x, resolvedSelectItems)
          }
        }
        Aggregate(resolvedChild, resolvedSelectItems.toList, resolvedGroupingKeys, resolvedHaving, a.nodeLocation)
    }
  }

  object resolveSortItemIndexes extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case s @ Sort(child, sortItems, _) =>
      val resolvedSortItems = sortItems.map {
        case sortItem @ SortItem(LongLiteral(i, _), _, _, _) =>
          val sortKey = resolveIndex(i.toInt - 1, child.outputAttributes)
          sortItem.copy(sortKey = sortKey)
        case other => other
      }
      s.copy(orderBy = resolvedSortItems)
    }

    @tailrec
    private def resolveIndex(index: Int, inputs: Seq[Attribute]): Expression = {
      inputs(index) match {
        case a: AllColumns =>
          resolveIndex(index, a.inputColumns)
        case a: Attribute =>
          toResolvedAttribute(a.name, a)
        case other => other
      }
    }
  }

  object resolveSortItems extends RewriteRule {
    def apply(context: AnalyzerContext): PlanRewriter = { case s @ Sort(child, sortItems, _) =>
      val resolvedChild = resolveRelation(context, child)
      // Sort can access the input relation of the child
      val inputAttributes = resolvedChild.inputAttributes
      val resolvedSortItems = sortItems.map { sortItem =>
        val e = resolveExpression(context, sortItem.sortKey, inputAttributes)
        sortItem.copy(sortKey = e)
      }
      s.copy(orderBy = resolvedSortItems)
    }
  }

  /**
    * Resolve TableRefs in a query inside WITH statement with CTERelationRef
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
          TableScan(qname.fullName, dbTable, dbTable.schema.columns, plan.nodeLocation)
        case None =>
          // Search CTE
          context.outerQueries.get(qname.fullName) match {
            case Some(cte) =>
              CTERelationRef(
                qname.fullName,
                cte.outputAttributes.map(_.withTableAlias(qname.fullName)),
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

        val joinKeyOrder = joinKeys.map(_.value).zipWithIndex.map(x => x._1 -> x._2).toMap[String, Int]

        // Merge the same name join keys as MultiSourceColumn
        // TODO Extract this as a method in Attribute object
        val mergedJoinKeys = resolvedJoinKeys
          .groupBy(_.attributeName).map { case (name, keys) =>
            val resolvedKeys = keys.flatMap {
              case SingleColumn(r: ResolvedAttribute, qual, _, _) =>
                Seq(r.withQualifier(qual))
              case m: MultiSourceColumn =>
                m.inputs
              case other =>
                Seq(other)
            }
            MultiSourceColumn(resolvedKeys, None, None, None)
          }
          .toSeq
          // Preserve the original USING(k1, k2, ...) order
          .sortBy(x => joinKeyOrder(x.name))

        val updated = resolvedJoin.withCond(ResolvedJoinUsing(mergedJoinKeys, None))
        updated
      case j @ Join(joinType, left, right, u @ JoinOn(Eq(leftKey, rightKey, _), _), _) =>
        val resolvedJoin =
          Join(joinType, resolveRelation(context, left), resolveRelation(context, right), u, j.nodeLocation)
        val resolvedJoinKeys: Seq[Expression] = Seq(leftKey, rightKey).map { k =>
          k.transformUpExpression { expr =>
            findMatchInInputAttributes(context, expr, resolvedJoin.inputAttributes) match {
              case Nil =>
                throw SQLErrorCode.ColumnNotFound.newException(
                  s"join key column: ${k.sqlExpr} is not found",
                  k.nodeLocation
                )
              case other =>
                if (other.size > 1) {
                  throw SQLErrorCode.SyntaxError.newException(
                    s"ambiguous join condition: ${expr} matches with [${other.mkString(", ")}]",
                    k.nodeLocation
                  )
                }
                other.head
            }
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
      val resolvedChild   = resolveRelation(context, child)
      val resolvedColumns = resolveOutputColumns(context, resolvedChild.outputAttributes, columns)
      val resolved        = Project(resolvedChild, resolvedColumns, p.nodeLocation)
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
      case a @ Alias(qualifier, name, expr, _, _) =>
        val resolved = resolveExpression(context, expr, inputAttributes)
        if (expr eq resolved) {
          resolvedColumns += a
        } else {
          resolvedColumns += a.copy(expr = resolved)
        }
      case s @ SingleColumn(expr, qualifier, _, nodeLocation) =>
        resolveExpression(context, expr, inputAttributes) match {
          case a: Attribute =>
            resolvedColumns += a.withQualifier(qualifier)
          case resolved =>
            resolvedColumns += s.copy(expr = resolved)
        }
      case other =>
        resolvedColumns += other
    }
    // Run one-more resolution (e.g., SingleColumn -> ResolvedAttribute)
    val output = resolvedColumns.result().map(resolveAttribute)
    output
  }

  def resolveAttribute(attribute: Attribute): Attribute = {
    attribute match {
      case a @ Alias(qualifier, name, attr: Attribute, _, _) =>
        val resolved = resolveAttribute(attr)
        if (attr eq resolved) {
          a
        } else {
          a.copy(expr = resolved)
        }
      case SingleColumn(a: Attribute, qualifier, _, _) if a.resolved =>
        a
      case m: MultiSourceColumn =>
        var changed = false
        val resolvedInputs = m.inputs.map {
          case a: Attribute =>
            val x = resolveAttribute(a)
            if (!(x eq a)) {
              changed = true
            }
            x
          case other => other
        }
        if (changed) {
          m.copy(inputs = resolvedInputs)
        } else {
          m
        }
      case other => other
    }
  }

  private def toResolvedAttribute(name: String, expr: Expression): Attribute = {
    @tailrec
    def findSourceColumn(e: Expression): Option[SourceColumn] = {
      e match {
        case r: ResolvedAttribute => r.sourceColumn
        case a: Alias             => findSourceColumn(a.expr)
        case _                    => None
      }
    }

    expr match {
      case a: Alias =>
        ResolvedAttribute(a.name, a.expr.dataType, a.qualifier, findSourceColumn(a.expr), None, a.nodeLocation)
      case s: SingleColumn =>
        ResolvedAttribute(name, s.dataType, s.qualifier, findSourceColumn(s.expr), None, s.nodeLocation)
      case a: Attribute =>
        // No need to resolve Attribute expressions
        a.withTableAlias(None)
      case other =>
        // Resolve expr as ResolvedAttribute so as not to pull-up too much details
        ResolvedAttribute(name, other.dataType, None, findSourceColumn(expr), None, other.nodeLocation)
    }
  }

  /**
    * Find matching expressions in the inputAttributes
    *
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

    def lookup(name: String, context: AnalyzerContext): List[Expression] = {
      val resolvedExprs = List.newBuilder[Expression]
      ColumnPath.fromQName(name) match {
        case Some(columnPath) =>
          resolvedAttributes.foreach {
            case a: Attribute =>
              resolvedExprs ++= a.matched(columnPath, context)
            case _ =>
          }
        case None =>
      }
      resolvedExprs.result()
    }

    val results = expr match {
      case i: Identifier =>
        lookup(i.value, context).map(toResolvedAttribute(i.value, _).withQualifier(None))
      case u @ UnresolvedAttribute(qualifier, name, _, _) =>
        lookup(u.fullName, context).map(toResolvedAttribute(name, _).withQualifier(qualifier))
      case a @ AllColumns(qualifier, None, _, _) =>
        // Resolve the inputs of AllColumn as ResolvedAttribute
        // so as not to pull up too much details
        val allColumns = resolvedAttributes.map {
          // Attribute can be used as is
          case a: Attribute => a
          case other        => toResolvedAttribute(other.name, other)
        }
        List(a.copy(columns = Some((qualifier match {
          case Some(q) => allColumns.filter(_.tableAlias.contains(q))
          case None    => allColumns
        }).map(_.withQualifier(None)))))
      case _ =>
        List(expr)
    }

    trace(
      s"[findMatchInInputAttributes]\n - input:  ${expr}\n - output: ${results
          .mkString(", ")}\n[inputAttributes]:\n ${inputAttributes.mkString("\n ")}"
    )
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
        trace(s"${expr} is ambiguous in ${lst}")
        throw SQLErrorCode.SyntaxError.newException(
          s"${expr.sqlExpr} is ambiguous:\n- ${lst.mkString("\n- ")}",
          expr.nodeLocation
        )
      case lst =>
        lst.headOption.getOrElse(expr)
    }
  }
}
