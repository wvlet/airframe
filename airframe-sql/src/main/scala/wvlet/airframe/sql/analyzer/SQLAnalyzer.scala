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

import wvlet.airframe.sql.analyzer.SQLAnalyzer.{AnalysisContext, warn}
import wvlet.airframe.sql.catalog.Catalog.Catalog
import wvlet.airframe.sql.model.LogicalPlan
import wvlet.airframe.sql.model.LogicalPlan.Table
import wvlet.airframe.sql.parser.{SQLInterpreter, SQLParser}
import wvlet.log.LogSupport

abstract class AnalysisException(message: String) extends Exception(message)
case class TableNotFound(name: String)            extends AnalysisException(s"Table ${name} not found")

/**
  *
  */
object SQLAnalyzer extends LogSupport {

  type Rule = (AnalysisContext) => PartialFunction[LogicalPlan, LogicalPlan]

  val rules: List[Rule] =
    TypeResolver.resolveTable _ :: Nil

  def analyze(sql: String, catalog: Catalog): LogicalPlan = {
    analyze(SQLParser.parse(sql), catalog)
  }

  def analyze(plan: LogicalPlan, catalog: Catalog): LogicalPlan = {
    if (plan.resolved) {
      plan
    } else {
      warn(s"Not resolved ${plan}")
      val context = AnalysisContext(database = "", catalog = catalog)

      val newPlan = rules.foldLeft(plan) { (targetPlan, rule) =>
        val r = rule.apply(context)
        targetPlan.transform(r)
      }
      warn(s"new plan :${newPlan}")
      newPlan
    }
  }

  case class AnalysisContext(database: String, catalog: Catalog)

}

object TypeResolver extends LogSupport {

  def resolveTable(context: AnalysisContext): PartialFunction[LogicalPlan, LogicalPlan] = {
    case plan @ LogicalPlan.Table(qname) =>
      context.catalog.findFromQName(context.database, qname) match {
        case Some(dbTable) =>
          warn(s"Found ${dbTable}")
          plan
        case None =>
          throw new TableNotFound(qname.toString)
      }
  }

}
