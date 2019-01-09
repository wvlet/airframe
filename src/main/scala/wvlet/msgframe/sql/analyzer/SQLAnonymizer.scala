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

package wvlet.msgframe.sql.analyzer
import wvlet.msgframe.sql.model._
import wvlet.msgframe.sql.parser.{SQLGenerator, SQLParser}

/**
  *
  */
object SQLAnonymizer {

  def anonymize(sql: String): String = {
    val plan = SQLParser.parse(sql)

    val anonymizedPlan = anonymize(plan)

    SQLGenerator.print(anonymizedPlan)
  }

  def anonymize(plan: LogicalPlan): LogicalPlan = {
    // TODO

    // Target: Identifier, Literal, UnresolvedAttribute, Table
    plan.transformExpressions(anonymizationRule)
  }

  private val anonymizationRule: PartialFunction[Expression, Expression] = {
    case i: Identifier =>
      UnquotedIdentifier("?")
    case s: StringLiteral =>
      StringLiteral("?")
    // TODO cover more different literal types
    case q: QName =>
      // TODO distinguish table QName and others
      QName("?")
    case u: UnresolvedAttribute =>
      UnresolvedAttribute(u.parts.map(x => "X"))
  }

}
