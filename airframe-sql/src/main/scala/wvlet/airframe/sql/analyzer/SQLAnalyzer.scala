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
import wvlet.airframe.sql.model.Expression._
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
    // First resolve all input table types
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
      val analysysContext = AnalysisContext(database = database, catalog = catalog)
      debug(s"Unresolved plan:\n${plan.pp}")

      val resolvedPlan = rules.foldLeft(plan) { (targetPlan, rule) =>
        val r = rule.apply(analysysContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }
      debug(s"Resolved plan:\n${resolvedPlan.pp}")

      val optimizerContext = OptimizerContext(Set.empty)
      val optimizedPlan = optimizerRules.foldLeft(resolvedPlan) { (targetPlan, rule) =>
        val r = rule.apply(optimizerContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }

      debug(s"new plan:\n${optimizedPlan.pp}")
      optimizedPlan
    }
  }

  case class AnalysisContext(database: String, catalog: Catalog)

  case class OptimizerContext(inputAttributes: Set[Attribute])
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
