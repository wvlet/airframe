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

import wvlet.airframe.sql.analyzer.SQLAnalyzer.{AnalysisContext, OptimizerContext, OptimizerRule, PlanRewriter, Rule}
import wvlet.airframe.sql.catalog.Catalog.Catalog
import wvlet.airframe.sql.model.LogicalPlan.Project
import wvlet.airframe.sql.model.{Attribute, LogicalPlan, LogicalPlanPrinter, TableScan}
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
    TypeResolver.resolveTable _ :: Nil

  val optimizerRules: List[OptimizerRule] = {
    Optimizer.pruneProjectionColumns _ :: Nil
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
  case class OptimizerContext(attributes: Set[Attribute])

}

object TypeResolver extends LogSupport {

  def resolveTable(context: AnalysisContext): PlanRewriter = {
    case plan @ LogicalPlan.Table(qname) =>
      context.catalog.findFromQName(context.database, qname) match {
        case Some(dbTable) =>
          warn(s"Found ${dbTable}")
          TableScan(qname, dbTable, dbTable.schema.columns.map(_.name))
        case None =>
          throw new TableNotFound(qname.toString)
      }
  }

}

object Optimizer extends LogSupport {

  def pruneProjectionColumns(context: OptimizerContext): PlanRewriter = {
    case p @ Project(child, selectItems) =>
      selectItems.map { x =>
        warn(x)
      }
      p
  }

  def pruneTableScanColumns(context: OptimizerContext): PlanRewriter = {
    case t @ TableScan(name, table, columns) =>
      t
  }

}
