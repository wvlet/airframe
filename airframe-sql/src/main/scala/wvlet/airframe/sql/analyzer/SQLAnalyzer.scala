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

import wvlet.airframe.sql.catalog.Catalog
import wvlet.airframe.sql.model._
import wvlet.airframe.sql.parser.SQLParser
import wvlet.log.LogSupport

/**
  * Propagate context
  * @param database
  *   context database
  * @param catalog
  * @param parentAttributes
  *   attributes used in the parent relation. This is used for pruning unnecessary columns output attributes
  */
case class AnalyzerContext(
    database: String,
    catalog: Catalog,
    parentAttributes: Option[Seq[Attribute]] = None,
    outerQueries: Map[String, LogicalPlan] = Map.empty
) {

  /**
    * Update the relation attributes used in the plan.
    *
    * @param parentAttributes
    * @return
    */
  def withAttributes(parentAttributes: Seq[Attribute]): AnalyzerContext =
    this.copy(parentAttributes = Some(parentAttributes))

  /**
    * Add an outer query (e.g., WITH query) to the context
    */
  def withOuterQuery(name: String, relation: LogicalPlan): AnalyzerContext = {
    this.copy(outerQueries = outerQueries + (name -> relation))
  }
}

/**
  */
object SQLAnalyzer extends LogSupport {
  type PlanRewriter = PartialFunction[LogicalPlan, LogicalPlan]
  type Rule         = (AnalyzerContext) => PlanRewriter

  def analyze(sql: String, database: String, catalog: Catalog): LogicalPlan = {
    debug(s"analyze:\n${sql}")
    analyze(SQLParser.parse(sql), database, catalog)
  }

  def analyze(plan: LogicalPlan, database: String, catalog: Catalog): LogicalPlan = {
    if (plan.resolved)
      plan
    else {
      val analyzerContext =
        AnalyzerContext(database = database, catalog = catalog, parentAttributes = Some(plan.outputAttributes))
      debug(s"Unresolved plan:\n${plan.pp}")

      val resolvedPlan = TypeResolver.resolve(analyzerContext, plan)
      debug(s"Resolved plan:\n${resolvedPlan.pp}")

      val optimizedPlan = Optimizer.optimizerRules.foldLeft(resolvedPlan) { (targetPlan, rule) =>
        val r = rule.apply(analyzerContext)
        // Recursively transform the tree
        targetPlan.transform(r)
      }

      debug(s"new plan:\n${optimizedPlan.pp}")
      optimizedPlan
    }
  }

}
