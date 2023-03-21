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
import wvlet.airspec.AirSpec

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

  test("func(a, b, c)") {
    val f = FunctionCall(
      "func",
      Seq(newIdentifier("a"), newIdentifier("b"), newIdentifier("c")),
      isDistinct = false,
      None,
      None,
      None
    )
    f.sqlExpr shouldBe "func(a, b, c)"
  }

  test("count(x)") {
    val f = FunctionCall("count", Seq(newIdentifier("x")), isDistinct = false, None, None, None)
    f.sqlExpr shouldBe "count(x)"
  }

  test("count(DISTINCT x)") {
    val f = FunctionCall("count", Seq(newIdentifier("x")), isDistinct = true, None, None, None)
    f.sqlExpr shouldBe "count(DISTINCT x)"
  }

  test("count(x) over ()") {
    val f = FunctionCall(
      "count",
      Seq(newIdentifier("x")),
      isDistinct = false,
      None,
      window = Some(Window(Seq.empty, Seq.empty, None, None)),
      None
    )
    f.sqlExpr shouldBe "count(x) OVER ()"
  }

  test("count(x) over (partition by y)") {
    val f = FunctionCall(
      "count",
      Seq(newIdentifier("x")),
      isDistinct = false,
      None,
      window = Some(Window(Seq(newIdentifier("y")), Seq.empty, None, None)),
      None
    )
    f.sqlExpr shouldBe "count(x) OVER (PARTITION BY y)"
  }

  test("count(x) over (partition by y order by x)") {
    val f = FunctionCall(
      "count",
      Seq(newIdentifier("x")),
      isDistinct = false,
      None,
      window = Some(
        Window(
          Seq(newIdentifier("y")),
          Seq(SortItem(newIdentifier("x"), None, None, None)),
          None,
          None
        )
      ),
      None
    )
    f.sqlExpr shouldBe "count(x) OVER (PARTITION BY y ORDER BY x)"
  }
}
