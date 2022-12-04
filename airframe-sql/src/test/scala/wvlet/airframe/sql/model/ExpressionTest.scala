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

import wvlet.airframe.sql.model.Expression.{FunctionCall, SingleColumn, UnquotedIdentifier}
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger

class ExpressionTest extends AirSpec {
  test("transform down") {
    val f = FunctionCall("count", Seq(UnquotedIdentifier("x", None)), isDistinct = true, None, None, None)
    val expr = SingleColumn(
      f,
      None,
      None,
      None
    )

    val newExpr = expr.transformExpression {
      case s @ SingleColumn(f: FunctionCall, _, _, _) if f.functionName == "count" =>
        s.withAlias("xxx")
    }

    newExpr shouldBe SingleColumn(f, Some("xxx"), None, None)
  }

  test("transform up in breadth-first order") {
    val f1 = FunctionCall("count_a", Seq.empty, false, None, None, None)
    val f2 = FunctionCall("count_b", Seq(f1), false, None, None, None)

    val count = new AtomicInteger(0)
    val newExpr = f2.transformExpression {
      case f: FunctionCall if f.functionName == "count_a" =>
        FunctionCall(s"count_a${count.getAndIncrement()}", f.args, false, None, None, None)
      case f: FunctionCall if f.functionName == "count_b" =>
        FunctionCall(s"count_b${count.getAndIncrement()}", f.args, false, None, None, None)
    }

    newExpr shouldBe FunctionCall(
      "count_b0",
      Seq(FunctionCall("count_a1", Seq.empty, false, None, None, None)),
      false,
      None,
      None,
      None
    )
  }

  test("transform up") {
    val f = FunctionCall("count", Seq(UnquotedIdentifier("x", None)), isDistinct = true, None, None, None)
    val expr = SingleColumn(
      f,
      None,
      None,
      None
    )

    val newExpr = expr.transformUpExpression {
      case s @ SingleColumn(f: FunctionCall, _, _, _) if f.functionName == "count" =>
        s.withAlias("xxx")
    }

    newExpr shouldBe SingleColumn(f, Some("xxx"), None, None)
  }

  test("transform up in depth-first order") {
    val f1 = FunctionCall("count_a", Seq.empty, false, None, None, None)
    val f2 = FunctionCall("count_b", Seq(f1), false, None, None, None)

    val count = new AtomicInteger(0)
    val newExpr = f2.transformUpExpression {
      case f: FunctionCall if f.functionName == "count_a" =>
        FunctionCall(s"count_a${count.getAndIncrement()}", f.args, false, None, None, None)
      case f: FunctionCall if f.functionName == "count_b" =>
        FunctionCall(s"count_b${count.getAndIncrement()}", f.args, false, None, None, None)
    }

    newExpr shouldBe FunctionCall(
      "count_b1",
      Seq(FunctionCall("count_a0", Seq.empty, false, None, None, None)),
      false,
      None,
      None,
      None
    )
  }

}
