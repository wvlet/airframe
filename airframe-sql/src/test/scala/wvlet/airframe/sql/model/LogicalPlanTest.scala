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

import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Project, TableRef}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

class LogicalPlanTest extends AirSpec {
  test("traverse plan nodes") {
    val l       = SQLParser.parse("select * from (select a, b from x where time > 0 group by 1, 2)")
    var visited = 0
    l.traverse {
      case a: Aggregate =>
        visited += 1
      case f: Filter =>
        visited += 10
      case p: Project =>
        visited += 100
      case t: TableRef =>
        visited += 1000
    }
    visited shouldBe 1111
  }
}
