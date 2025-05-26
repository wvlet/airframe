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
package wvlet.airframe.sql.model

import wvlet.airframe.sql.model.Expression.{FunctionCall, SortItem, Window, newIdentifier}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec
import wvlet.airspec.spi.AssertionFailure

class SQLExprTest extends AirSpec {

  test("newIdentifier") {
    val i = newIdentifier("a")
    i.value shouldBe "a"
    i.sqlExpr shouldBe "a"

    val q = newIdentifier("`x`")
    q.value shouldBe "x"
    q.sqlExpr shouldBe "`x`"

    val d = newIdentifier("1")
    d.value shouldBe "1"
    d.sqlExpr shouldBe "1"

    val qd = newIdentifier("\"database\"")
    qd.value shouldBe "database"
    qd.sqlExpr shouldBe "\"database\""
  }

  private def check(sql: String): Unit = {
    test(sql) {
      try {
        SQLParser.parseExpression(sql).sqlExpr shouldBe sql
      } catch {
        case e: AssertionFailure =>
          error(s"Failed to generate a proper SQL expression for:\n${SQLParser.parseExpression(sql)}")
          throw e
      }
    }
  }

  private def check(sql: String, expect: String): Unit = {
    test(sql) {
      try {
        SQLParser.parseExpression(sql).sqlExpr shouldBe expect
      } catch {
        case e: AssertionFailure =>
          error(s"Failed to generate a proper SQL expression for:\n${SQLParser.parseExpression(sql)}")
          throw e
      }
    }
  }

  test("Expression.sqlExpr") {
    check("func(a, b, c)")
    check("count(x)")
    check("count(DISTINCT x)")
    check("count(x) OVER ()")
    check("count(x) OVER (PARTITION BY y)")
    check("count(x) OVER (PARTITION BY y ORDER BY x)")
    check("array_agg(name) FILTER (WHERE name IS NOT NULL)")
  }

  test("unary expression") {
    check("+1")
    check("-1")
    check("+1.0", "+DECIMAL '1.0'")
    check("-1.0", "-DECIMAL '1.0'")
    check("+1.2E23", "+1.2E23")
    check("-1.2E23", "-1.2E23")
  }

  test("binary expression") {
    check("1 + 2")
    check("a - b")
    check("a * b")
    check("a / b")
    check("a AND b")
    check("a OR b")
    check("a = b")
    check("a != b")
    check("a <> b")
    check("a < b")
    check("a <= b")
    check("a > b")
    check("a >= b")
    check("a IS NULL")
    check("a IS NOT NULL")
    check("a LIKE b")
    check("a LIKE 'hello%'")
    check("a NOT LIKE b")
    check("a IN (1, 2, 3)")
    check("a NOT IN (1, 2, 3)")
    check("a BETWEEN 1 AND 10")
    check("a NOT BETWEEN 1 AND 10")
    check("a IS DISTINCT FROM b")
    check("a IS NOT DISTINCT FROM b")
  }

  test("complex expressions") {
    check("a + b * c")
    check("a + b * c + d")
    check("a + b * c + d * e")
  }

  test("in sub query") {
    check("a IN (SELECT * FROM t)")
    check("a NOT IN (SELECT * FROM t)")

  }

  test("special date and time functions") {
    check("current_date")
    check("current_time")
    check("current_time(6)")
    check("current_timestamp")
    check("current_timestamp(6)")
    check("localtime")
    check("localtime(6)")
    check("localtimestamp")
    check("localtimestamp(6)")
  }
}
