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

import wvlet.airframe.sql.model.Expression.SingleColumn
import wvlet.airframe.sql.model.LogicalPlan.{Aggregate, Filter, Limit, Project, TableRef}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

import java.util.concurrent.atomic.AtomicInteger

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

  test("traverse child plan nodes") {
    val l       = SQLParser.parse("select * from (select a, b from x where time > 0 group by 1, 2)")
    var visited = 0
    l.traverseChildren {
      case a: Aggregate =>
        visited += 1
      case f: Filter =>
        visited += 10
      case p: Project =>
        visited += 100
      case t: TableRef =>
        visited += 1000
    }
    visited shouldBe 1011
  }

  test("traverseOnce") {
    val l       = SQLParser.parse("select * from (select a, b from x where time > 0 group by 1, 2)")
    var visited = 0
    l.traverseOnce {
      case a: Aggregate =>
        visited += 1
      case f: Filter =>
        visited += 10
      case p: Project =>
        visited += 100
      case t: TableRef =>
        visited += 1000
    }
    visited shouldBe 100
  }

  test("traverseChildrenOnce") {
    val l       = SQLParser.parse("select * from (select a, b from x where time > 0 group by 1, 2)")
    var visited = 0
    l.traverseChildrenOnce {
      case a: Aggregate =>
        visited += 1
      case f: Filter =>
        visited += 10
      case p: Project =>
        visited += 100
      case t: TableRef =>
        visited += 1000
    }
    visited shouldBe 1
  }

  test("Transform children") {
    val l = SQLParser.parse("select * from (select a from x limit 10)")
    val newPlan = l.transformChildren { case l: Limit =>
      l.child
    }
    newPlan.traverse { case l: Limit =>
      fail(s"Should not have limit")
    }
  }

  test("transform once") {
    val l = SQLParser.parse("select * from (select * from (select a from x limit 10) limit 100)")
    val newPlan = l.transformOnce { case l: Limit =>
      l.child
    }
    var count = 0
    newPlan.traverse { case l: Limit =>
      count += 1
    }
    count shouldBe 1
  }

  test("transform only child expressions") {
    val l = SQLParser.parse("select a from (select a from t)")
    val newPlan = l.transformChildExpressions { case s: SingleColumn =>
      s.withAlias("x")
    }
    newPlan.expressions.collect {
      case s: SingleColumn if s.alias == Some("x") => true
    }.nonEmpty shouldBe true
  }

  test("transform all expressions") {
    val l = SQLParser.parse("select a from (select a from t)")
    val newPlan = l.transformExpressions { case s: SingleColumn =>
      s.withAlias("x")
    }
    newPlan.collectExpressions {
      case s: SingleColumn if s.alias == Some("x") => true
    }.size shouldBe 2
  }

  test("transform down child expressions") {
    val count = new AtomicInteger(0)
    val l     = SQLParser.parse("select a from (select b from t)")
    val newPlan = l.transformExpressions { case s: SingleColumn =>
      s.withAlias(s"x${count.getAndIncrement()}")
    }

    newPlan.expressions.collect {
      case s: SingleColumn if s.alias == Some("x0") => true
    }.nonEmpty shouldBe true

    newPlan.children.head.expressions.collect {
      case s: SingleColumn if s.alias == Some("x1") => true
    }.nonEmpty shouldBe true
  }

  test("transform up child expressions") {
    val count = new AtomicInteger(0)
    val l     = SQLParser.parse("select a from (select b from t)")
    val newPlan = l.transformUpExpressions { case s: SingleColumn =>
      s.withAlias(s"x${count.getAndIncrement()}")
    }

    newPlan.expressions.collect {
      case s: SingleColumn if s.alias == Some("x1") => true
    }.nonEmpty shouldBe true

    newPlan.children.head.expressions.collect {
      case s: SingleColumn if s.alias == Some("x0") => true
    }.nonEmpty shouldBe true
  }
}
