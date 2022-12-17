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

import wvlet.airframe.sql.analyzer.RewriteRule.PlanRewriter
import wvlet.airframe.sql.model.LogicalPlan
import wvlet.log.{LogSupport, Logger}

trait RewriteRule extends LogSupport {
  // Prepare a logger for debugging purpose
  private val localLogger = Logger("wvlet.airframe.sql.analyzer.RewriteRule")

  def name: String = this.getClass.getSimpleName.stripSuffix("$")
  def apply(context: AnalyzerContext): PlanRewriter

  def transform(plan: LogicalPlan, context: AnalyzerContext): LogicalPlan = {
    val rule = this.apply(context)
    // Recursively transform the tree form bottom to up
    val resolved = plan.transformUp(rule)
    if (!(plan eq resolved)) {
      localLogger.trace(s"transformed with ${name}:\n[before]\n${plan.pp}\n[after]\n${resolved.pp}")
    }
    resolved
  }
}

object RewriteRule {
  type PlanRewriter = PartialFunction[LogicalPlan, LogicalPlan]
}
