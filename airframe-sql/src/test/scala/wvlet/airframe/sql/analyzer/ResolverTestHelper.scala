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
import wvlet.airframe.sql.model.LogicalPlan
import wvlet.airframe.sql.parser.{SQLFormatter, SQLGenerator, SQLParser}
import wvlet.airspec.AirSpec
import wvlet.log.Logger

trait ResolverTestHelper { self: AirSpec =>
  protected def demoCatalog: Catalog

  protected def resolvePlan(sql: String, rules: List[RewriteRule]): LogicalPlan = {
    val plan            = SQLParser.parse(sql)
    val analyzerContext = AnalyzerContext("default", demoCatalog).withAttributes(plan.outputAttributes)
    val resolvedPlan    = TypeResolver.resolve(analyzerContext, plan, rules)
    shouldBeResolved(resolvedPlan, sql)
    resolvedPlan
  }

  private def shouldBeResolved(p: LogicalPlan, sql: String): Unit = {
    if (!p.resolved) {
      fail(
        s"""Found unresolved expressions in:
           |[sql]
           |${sql}
           |[plan]
           |${p.pp}
           |[unresolved expressions]
           |${p.unresolvedExpressions.mkString("\n")}""".stripMargin
      )
    }
  }

  protected def analyze(
      sql: String,
      rules: List[RewriteRule] = TypeResolver.typerRules
  ): LogicalPlan = {
    val resolvedPlan = resolvePlan(sql, rules)
    val resolvedSql  = generateSql(resolvedPlan)
    debug(s"[original]\n${sql}\n\n[resolved]\n${resolvedSql}")
    trace(s"[original plan]\n${SQLParser.parse(sql).pp}\n[resolved plan]\n${resolvedPlan.pp}")

    // Suppress rewrite rule logs in the second run
    Logger("wvlet.airframe.sql.analyzer.RewriteRule").suppressLogs {
      // Round-trip plan should be able to be resolved
      resolvePlan(resolvedSql, rules)
    }
    resolvedPlan
  }

  private def generateSql(p: LogicalPlan): String = {
    SQLFormatter.format(SQLGenerator.print(p))
  }
}
