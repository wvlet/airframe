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

import wvlet.airspec.AirSpec

/**
  */
class ParquetQueryPlannerTest extends AirSpec {
  test("select *") {
    val p = ParquetQueryPlanner.parse("select * from _")
    p.projectedColumns shouldBe empty
    p.predicate shouldBe empty
  }

  test("select a, b from _") {
    val p = ParquetQueryPlanner.parse("select a, b from _")
    p.projectedColumns shouldBe Seq("a", "b")
    p.predicate shouldBe empty
  }

  test("select * from a > 10") {
    val p = ParquetQueryPlanner.parse("select * from _ where a > 10")
    p.projectedColumns shouldBe empty
    p.predicate shouldBe defined
  }

  test("select * from a > 1 and a <= 5") {
    val p = ParquetQueryPlanner.parse("select * from _ where a > 1 and a <= 5")
    p.projectedColumns shouldBe empty
    p.predicate shouldBe defined
  }

}
