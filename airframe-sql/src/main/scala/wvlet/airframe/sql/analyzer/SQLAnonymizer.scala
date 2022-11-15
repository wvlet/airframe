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
import java.util.concurrent.atomic.AtomicInteger

import wvlet.log.LogSupport
import wvlet.airframe.sql.model._
import wvlet.airframe.sql.parser.{SQLGenerator, SQLParser}

class SQLAnonymizer(dict: Map[Expression, Expression]) {
  def run(sql: String): Unit = {
    val plan = SQLParser.parse(sql)

    val anonymizationRule: PartialFunction[Expression, Expression] = {
      case x if dict.contains(x) =>
        dict(x)
    }

    plan.transformExpressions(anonymizationRule)
  }
}

/**
  */
object SQLAnonymizer extends LogSupport {
  def anonymize(sql: String): String = {
    val plan           = SQLParser.parse(sql)
    val dict           = new DictBuilder().add(plan).build
    val anonymizedPlan = anonymize(plan, dict)
    SQLGenerator.print(anonymizedPlan)
  }

  def anonymize(plan: LogicalPlan, dict: Map[Expression, Expression]): LogicalPlan = {
    val anonymizationRule: PartialFunction[Expression, Expression] = {
      case x if dict.contains(x) =>
        dict(x)
    }
    // Target: Identifier, Literal, UnresolvedAttribute, Table
    plan.transformExpressions(anonymizationRule)
  }

  def buildAnonymizationDictionary(sql: Seq[String]): Map[Expression, Expression] = {
    debug("Building a token dictionary")
    val b = new DictBuilder()
    sql.foreach { x =>
      try {
        val plan = SQLParser.parse(x)
        b.add(plan)
      } catch {
        case e: Exception =>
          warn(e)
      }
    }
    b.build
  }

  import Expression._

  private class DictBuilder {
    val m                  = Map.newBuilder[Expression, Expression]
    val identifierTable    = new SymbolTable("i")
    var stringLiteralTable = new SymbolTable("s")
    var longLiteralTable   = new SymbolTable("l")
    var qnameTable         = new SymbolTable("t")

    def build = m.result()

    def add(plan: LogicalPlan): this.type = {
      // Target: Identifier, Literal, UnresolvedAttribute, Table (QName)
      plan.traverseExpressions {
        case i: Identifier =>
          m += i -> UnquotedIdentifier(identifierTable.lookup(i.value), i.nodeLocation)
        case s: StringLiteral =>
          m += s -> StringLiteral(stringLiteralTable.lookup(s.value), s.nodeLocation)
        case q: QName =>
          m += q -> QName(q.parts.map(qnameTable.lookup), q.nodeLocation)
        case u: UnresolvedAttribute =>
          val v = UnresolvedAttribute(u.name.split("\\.").toSeq.map(qnameTable.lookup).mkString("."), u.nodeLocation)
          m += u -> v
      }
      this
    }
  }

  private class SymbolTable(prefix: String) {
    private val count       = new AtomicInteger(0)
    private val symbolTable = collection.mutable.Map.empty[String, String]

    def lookup(token: String): String = {
      symbolTable.getOrElseUpdate(
        token, {
          val c = count.incrementAndGet()
          s"${prefix}${c}"
        }
      )
    }
  }
}
