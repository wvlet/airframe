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

import wvlet.airframe.sql.analyzer.SQLAnalyzer
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.catalog.Catalog.{CreateMode, TableColumn}
import wvlet.airspec.AirSpec

class SQLGeneratorTest extends AirSpec {

  private val a1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val a2 = TableColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val b1 = TableColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
  private val b2 = TableColumn("country", DataType.StringType, properties = Map("tag" -> Seq("private")))

  private val tableA = Catalog.newTable(
    "default",
    "A",
    Catalog.newSchema
      .addColumn(a1)
      .addColumn(a2)
  )

  private val tableB = Catalog.newTable(
    "default",
    "B",
    Catalog.newSchema
      .addColumn(b1)
      .addColumn(b2)
  )

  private def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableA, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableB, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog
  }

  test("print resolved plan") {
    val resolvedPlan = SQLAnalyzer.analyze("select * from A", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT * FROM A"
  }

  test("print resolved subquery plan") {
    val resolvedPlan = SQLAnalyzer.analyze("select id from (select id from A)", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT id FROM (SELECT id FROM A)"
  }

  test("print resolved UNION subquery plan") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id from (select id from A union all select id from A)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan)
    sql shouldBe "SELECT id FROM (SELECT id FROM A UNION ALL SELECT id FROM A)"
  }

  test("print resolved CTE plan") {
    val resolvedPlan = SQLAnalyzer.analyze("with p as (select id from A) select * from p", "default", demoCatalog)
    val sql          = SQLGenerator.print(resolvedPlan)
    sql shouldBe "WITH p AS (SELECT id FROM A) SELECT * FROM p"
  }

  test("generate aggregation without grouping keys") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select count(1) from A having count(distinct id) > 10", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan)

    sql.contains("GROUP BY") shouldBe false
    sql.contains("HAVING count(DISTINCT id) > 10") shouldBe true
  }

  test("generate select with column alias") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id as xid from A", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan)

    sql.contains("SELECT id AS xid FROM A") shouldBe true
  }

  test("generate join with USING") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id from A inner join A as B using (id)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase

    sql.contains("using (id)") shouldBe true
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
    sql.contains("ON t1.id = t2.id") shouldBe true
  }

  test("generate ORDER BY in sub-query") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select * from (select id from A order by id)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select * from (select id from a order by id)"
  }

  test("generate GROUP BY in sub-query") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select * from (select id, count(*) from A group by id)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select * from (select id, count(*) from a group by id)"
  }

  test("generate LIMIT in sub-query") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select * from (select id from A limit 10)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select * from (select id from a limit 10)"
  }

  test("generate DISTINCT in sub-query") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select * from (select distinct id from A)", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select * from (select distinct id from a)"
  }

  test("generate JOIN + SELECT * in sub-query") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select country from (select * from A inner join B using (id))", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select country from (select * from a join b using (id))"
  }

  test("preserve aggregate indexes") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select id, country, count(*) from A group by 1, 2", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select id, country, count(*) from a group by 1, 2"
  }

  test("aliased relation") {
    val resolvedPlan =
      SQLAnalyzer.analyze("select * from A as t1", "default", demoCatalog)
    val sql = SQLGenerator.print(resolvedPlan).toLowerCase
    sql shouldBe "select * from a as t1"
  }
}
