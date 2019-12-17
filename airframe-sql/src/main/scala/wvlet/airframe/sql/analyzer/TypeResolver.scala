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
import wvlet.airframe.sql.analyzer.SQLAnalyzer.{AnalysisContext, PlanRewriter}
import wvlet.airframe.sql.model.Expression.{AllColumns, Identifier, SingleColumn}
import wvlet.airframe.sql.model.LogicalPlan.Project
import wvlet.airframe.sql.model._
import wvlet.log.LogSupport

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  def resolveTableRef(context: AnalysisContext): PlanRewriter = {
    case plan @ LogicalPlan.TableRef(qname) =>
      context.catalog.findFromQName(context.database, qname) match {
        case Some(dbTable) =>
          debug(s"Found ${dbTable}")
          TableScan(qname, dbTable, dbTable.schema.columns.map(_.name))
        case None =>
          throw new TableNotFound(qname.toString)
      }
  }

  def resolveColumns(context: AnalysisContext): PlanRewriter = {
    case p @ Project(child, columns) =>
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
              resolvedColumns += ResolvedAttribute(alias.get.sqlExpr, r.dataType)
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
    expr match {
      case i: Identifier =>
        inputAttributes
          .find(attr => attr.name == i.value)
          .getOrElse(i)
      case _ => expr
    }
  }
}
