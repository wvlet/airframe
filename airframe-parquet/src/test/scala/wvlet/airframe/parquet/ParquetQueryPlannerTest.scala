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
package wvlet.airframe.parquet

import wvlet.airframe.sql.model.Expression._
import wvlet.airspec.AirSpec

/**
  */
class ParquetQueryPlannerTest extends AirSpec {
  test("select *") {
    val p = ParquetQueryPlanner.parse("select * from _")
    p.projectedColumns shouldBe empty
    p.condition shouldBe empty
  }

  test("select a, b from _") {
    val p = ParquetQueryPlanner.parse("select a, b from _")
    p.projectedColumns shouldBe Seq("a", "b")
    p.condition shouldBe empty
  }

  test("select * from a > 10") {
    val p = ParquetQueryPlanner.parse("select * from _ where a > 10")
    p.projectedColumns shouldBe empty
    p.condition match {
      case Some(GreaterThan(id: Identifier, LongLiteral(10))) if id.value == "a" =>
      // OK
      case other =>
        fail(s"unexpected condition: ${other}")
    }
  }

  test("select * from a > 1 and a <= 5") {
    val p = ParquetQueryPlanner.parse("select * from _ where a > 1 and a <= 5")
    p.projectedColumns shouldBe empty
    p.condition match {
      case Some(And(GreaterThan(id: Identifier, LongLiteral(1)), LessThanOrEq(id2: Identifier, LongLiteral(5))))
          if id.value == "a" && id2.value == "a" =>
      // OK
      case other =>
        fail(s"unexpected condition: ${other}")
    }
  }

}
