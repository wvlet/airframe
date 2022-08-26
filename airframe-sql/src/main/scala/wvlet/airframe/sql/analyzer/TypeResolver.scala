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
import wvlet.airframe.sql.model.LogicalPlan.{Filter, Project, Relation}
import wvlet.airframe.sql.model._
import wvlet.log.LogSupport

/**
  * Resolve untyped [[LogicalPlan]]s and [[Expression]]s into typed ones.
  */
object TypeResolver extends LogSupport {

  val typerRules: List[Rule] =
    // First resolve all input table types
    TypeResolver.resolveTableRef _ ::
      TypeResolver.resolveRelation _ ::
      TypeResolver.resolveColumns _ ::
      Nil

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  def resolveTableRef(context: AnalyzerContext): PlanRewriter = { case plan @ LogicalPlan.TableRef(qname) =>
    context.catalog.findFromQName(context.database, qname) match {
      case Some(dbTable) =>
        trace(s"Found ${dbTable}")
        TableScan(qname, dbTable, dbTable.schema.columns.map(_.name))
      case None =>
        throw SQLErrorCode.TableNotFound.toException(s"Table ${context.database}.${qname} not found")
    }
  }

  def resolveRelation(context: AnalyzerContext): PlanRewriter = {
    case filter @ Filter(child, filterExpr) =>
      filter.transformExpressions { case x: Expression => resolveExpression(x, filter.inputAttributes) }
    case r: Relation =>
      r.transformExpressions { case x: Expression => resolveExpression(x, r.inputAttributes) }
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
      case u @ UnresolvedAttribute(name) =>
        val attrName = QName(name).parts.last
        inputAttributes
          .find(attr => attr.name == attrName)
          .getOrElse(u)
      case _ =>
        expr
    }
  }
}
