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

import wvlet.airframe.sql.analyzer.SQLAnalyzer.{AnalysisContext, OptimizerContext, PlanRewriter}
import wvlet.airframe.sql.catalog.Catalog.Catalog
import wvlet.airframe.sql.model.LogicalPlan.{Project, Relation}
import wvlet.airframe.sql.model._
import wvlet.airframe.sql.parser.SQLParser
import wvlet.log.LogSupport

abstract class AnalysisException(message: String) extends Exception(message)
case class TableNotFound(name: String)            extends AnalysisException(s"Table ${name} not found")

/**
  *
  */
object SQLAnalyzer extends LogSupport {

  type PlanRewriter  = PartialFunction[LogicalPlan, LogicalPlan]
  type Rule          = (AnalysisContext) => PlanRewriter
  type OptimizerRule = (OptimizerContext) => PlanRewriter

  val rules: List[Rule] =
    TypeResolver.resolveTableRef _ ::
      TypeResolver.resolveColumns _ ::
      Nil

  val optimizerRules: List[OptimizerRule] = {
    Optimizer.pruneColumns _ ::
      Nil
  }

  def analyze(sql: String, database: String, catalog: Catalog): LogicalPlan = {
    debug(s"analyze:\n${sql}")
    analyze(SQLParser.parse(sql), database, catalog)
  }

  def analyze(plan: LogicalPlan, database: String, catalog: Catalog): LogicalPlan = {
    if (plan.resolved) {
      plan
    } else {
      warn(s"Not resolved ${plan}")
      val analysysContext = AnalysisContext(database = database, catalog = catalog)

      val newPlan = rules.foldLeft(plan) { (targetPlan, rule) =>
        val r = rule.apply(analysysContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }

      val optimizerContext = OptimizerContext(Set.empty)
      val optimizedPlan = optimizerRules.foldLeft(newPlan) { (targetPlan, rule) =>
        val r = rule.apply(optimizerContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }

      warn(s"new plan:\n${LogicalPlanPrinter.print(optimizedPlan)}")
      optimizedPlan
    }
  }

  case class AnalysisContext(database: String, catalog: Catalog)

  case class OptimizerContext(inputAttributes: Set[Attribute])

}

object TypeResolver extends LogSupport {

  /**
    * Resolve TableRefs with concrete TableScans using the table schema in the catalog.
    */
  def resolveTableRef(context: AnalysisContext): PlanRewriter = {
    case plan @ LogicalPlan.TableRef(qname) =>
      context.catalog.findFromQName(context.database, qname) match {
        case Some(dbTable) =>
          warn(s"Found ${dbTable}")
          TableScan(qname, dbTable, dbTable.schema.columns.map(_.name))
        case None =>
          throw new TableNotFound(qname.toString)
      }
  }

  def resolveColumns(context: AnalysisContext): PlanRewriter = {
    case p @ Project(child, columns) =>
      val inputAttributes = child.inputAttributes
      val resolvedColumns = Seq.newBuilder[Attribute]
      columns.map {
        case a: AllColumns =>
          // TODO check (prefix).* to resolve attributes
          resolvedColumns ++= inputAttributes
        case SingleColumn(expr, None) =>
          val resolvedExpr = resolveExpression(expr)
          SingleColumn(resolvedExpr, None)
        case other =>
          resolvedColumns += other
      }

      Project(child, resolvedColumns.result())
  }

  def resolveExpression(expr: Expression): Expression = {
    expr match {
      case _ => expr
    }
  }

}

object Optimizer extends LogSupport {

  def extractInputs(expressions: Seq[Expression]): Set[Attribute] = {
    val newAttributes: Seq[Attribute] = expressions.collect {
      case s: Attribute =>
        s
    }
    newAttributes.toSet
  }

  def pruneColumns(context: OptimizerContext): PlanRewriter = {
    case p @ Project(child, selectItems) =>
      val newContext = OptimizerContext(selectItems.toSet)
      Project(pruneRelationColumns(child, newContext), selectItems)
    case r: Relation =>
      pruneRelationColumns(r, context)
  }

  def pruneRelationColumns(relation: Relation, context: OptimizerContext): Relation = {
    relation match {
      case t @ TableScan(name, table, columns) =>
        val accessedColumns = columns.filter { col =>
          context.inputAttributes.exists(x => x.name == col)
        }
        TableScan(name, table, accessedColumns)
      case _ => relation
    }
  }

}
