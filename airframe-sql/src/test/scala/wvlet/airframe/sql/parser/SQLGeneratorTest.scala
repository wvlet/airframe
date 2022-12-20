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
package wvlet.airframe.sql.parser

import wvlet.airframe.sql.analyzer.{SQLAnalyzer, RewriteRule}
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.catalog.Catalog.{CreateMode, TableColumn}
import wvlet.airspec.AirSpec

class SQLGeneratorTest extends AirSpec {

  private val a1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val a2 = TableColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val tableA = Catalog.newTable(
    "default",
    "A",
    Catalog.newSchema
      .addColumn(a1)
      .addColumn(a2)
  )

  private def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableA, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog
  }

  test("print resolved plan") {
    val resolvedPlan = SQLAnalyzer.analyze("select * from A", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT * FROM default.A"
  }

  test("print resolved subquery plan") {
    val resolvedPlan = SQLAnalyzer.analyze("select id from (select id from A)", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT id FROM (SELECT A.id FROM default.A)"
  }

  test("print resolved UNION subquery plan") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id from (select id from A union all select id from A)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT id FROM (SELECT A.id FROM default.A UNION ALL SELECT A.id FROM default.A)"
  }

  test("print resolved CTE plan") {
    val resolvedPlan = SQLAnalyzer.analyze("with p as (select id from A) select * from p", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "WITH p AS (SELECT A.id FROM default.A) SELECT * FROM p"
  }

  test("generate aggregation without grouping keys") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select count(1) from A having count(distinct id) > 10", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan)

    sql.contains("GROUP BY") shouldBe false
    sql.contains("HAVING count(DISTINCT A.id) > 10") shouldBe true
  }

  test("generate select with column alias") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id as xid from A", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase

    sql.contains("select a.id as xid") shouldBe true
  }

  test("generate join with USING") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id from A inner join A as B using (id)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase

    sql.contains("on a.id = b.id") shouldBe true
  }

  test("generate join with keys with qualifier") {
    val resolvedPlan =
      SQLAnalyzer.analyze(
        """select count(*)
          |  from
          |    (select * from A) t1
          |  join
          |    (select * from A) t2
          |  on t1.id = t2.id
          |""".stripMargin,
        "default",
        demoCatalog
      )

    val sql = SQLGenerator.print(resolvedPlan)
    info(sql)
    info(resolvedPlan.pp)
    sql.contains("ON t1.id = t2.id") shouldBe true
  }

}
