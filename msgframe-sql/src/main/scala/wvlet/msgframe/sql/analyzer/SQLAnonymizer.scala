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
import java.util.concurrent.atomic.AtomicInteger

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

    val expressions = plan.collectExpressions.distinct

    val dict = createAnonymizationDictionary(expressions)

    val anonymizationRule: PartialFunction[Expression, Expression] = {
      case x if dict.contains(x) =>
        dict(x)
    }

    // Target: Identifier, Literal, UnresolvedAttribute, Table
    plan.transformExpressions(anonymizationRule)
  }

  private def createAnonymizationDictionary(list: List[Expression]): Map[Expression, Expression] = {
    val m = Map.newBuilder[Expression, Expression]

    val identifierTable    = new SymbolTable("i")
    var stringLiteralTable = new SymbolTable("s")
    var longLiteralTable   = new SymbolTable("l")
    var qnameTable         = new SymbolTable(s"t")

    list.collect {
      case i: Identifier =>
        m += i -> UnquotedIdentifier(identifierTable.lookup(i.value))
      case s: StringLiteral =>
        // TODO understand the context of the expression
        m += s -> StringLiteral(stringLiteralTable.lookup(s.value))
      case q: QName =>
        m += q -> QName(q.parts.map(qnameTable.lookup))
      case u: UnresolvedAttribute =>
        m += u -> UnresolvedAttribute(u.parts.map(qnameTable.lookup))
    }

    m.result()
  }

  private class SymbolTable(prefix: String) {
    private val count       = new AtomicInteger(0)
    private val symbolTable = collection.mutable.Map.empty[String, String]

    def lookup(token: String): String = {
      symbolTable.getOrElseUpdate(token, {
        val c = count.incrementAndGet()
        s"${prefix}${c}"
      })
    }
  }
}
