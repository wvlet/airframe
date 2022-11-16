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
import wvlet.airframe.sql.model.Expression.SingleColumn
import wvlet.airframe.sql.model.{CTERelationRef, LogicalPlan}
import wvlet.airframe.sql.model.LogicalPlan.{Project, Query, Relation, With, WithQuery}
import wvlet.log.LogSupport

/**
  * Resolve sub queries defined in CTE (WITH statement)
  */
object CTEResolver extends LogSupport {

  def resolveCTE(analyzerContext: AnalyzerContext, p: LogicalPlan): LogicalPlan = {
    p.transform { case q @ Query(w @ With(recursive, queryDefs, _), body, _) =>
      if (recursive) {
        throw SQLErrorCode.UnsupportedSyntax.newException(s"recursive WITH statement is not supported", q.nodeLocation)
      }

      var currentContext = analyzerContext
      val resolvedQueries = queryDefs.map { x =>
        val resolvedQuery: Relation = TypeResolver.resolveRelation(currentContext, x.query)
        val cteBody = x.columnNames match {
          case None =>
            resolvedQuery
          case Some(aliases) =>
            // When there are aliases, WITH q(p1, p2, ...) as (select ....)
            if (resolvedQuery.outputAttributes.size != aliases.size) {
              throw SQLErrorCode.SyntaxError.newException(
                s"A wrong number of columns ${aliases.size} is used for WITH statement: ${x.name.value}",
                x.nodeLocation
              )
            }
            // Add a projection for renaming columns
            val selectItems = resolvedQuery.outputAttributes.zip(aliases).map { case (col, alias) =>
              TypeResolver.resolveAttribute(SingleColumn(col, Some(alias), None, col.nodeLocation))
            }
            Project(resolvedQuery, selectItems, resolvedQuery.nodeLocation)
        }
        currentContext = currentContext.withOuterQuery(x.name.value, cteBody)
        // cteBody already has renaming with projection, no need to propagate column name aliases
        WithQuery(x.name, cteBody, None, q.nodeLocation)
      }
      val newBody = TypeResolver.resolveRelation(currentContext, body)
      Query(With(recursive, resolvedQueries, w.nodeLocation), newBody, q.nodeLocation)
    }
  }
}
