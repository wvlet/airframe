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

import wvlet.airframe.sql.catalog.Catalog.CreateMode
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.Expression.{Alias, AllColumns}
import wvlet.airframe.sql.model.{NodeLocation, ResolvedAttribute, SourceColumn}
import wvlet.airspec.AirSpec

/**
  */
class SQLAnalyzerTest extends AirSpec {
  private lazy val tbl1 = Catalog.newTable(
    "public",
    "a",
    Catalog.newSchema
      .addColumn("id", DataType.LongType)
      .addColumn("name", DataType.StringType)
      .addColumn("address", DataType.StringType)
  )

  private lazy val tbl2 =
    Catalog.newTable(
      "public",
      "b",
      Catalog.newSchema
        .addColumn("id", DataType.LongType)
        .addColumn("phone", DataType.StringType)
    )

  private lazy val catalog = {
    val c = new InMemoryCatalog("default", None, Seq.empty)
    c.createDatabase(Catalog.Database("public"), CreateMode.CREATE_IF_NOT_EXISTS)
    c.createTable(tbl1, CreateMode.CREATE_IF_NOT_EXISTS)
    c.createTable(tbl2, CreateMode.CREATE_IF_NOT_EXISTS)
    c
  }

  test("resolve input/output types") {
    val plan = SQLAnalyzer.analyze("select id, name from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.toList shouldBe List(
      ResolvedAttribute("id", DataType.LongType, None, Some(SourceColumn(tbl1, tbl1.column("id"))), None),
      ResolvedAttribute("name", DataType.StringType, None, Some(SourceColumn(tbl1, tbl1.column("name"))), None)
    )
  }

  test("resolve select *") {
    val plan = SQLAnalyzer.analyze("select * from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.toList shouldBe List(
      AllColumns(
        None,
        Some(
          Seq(
            ResolvedAttribute("id", DataType.LongType, None, Some(SourceColumn(tbl1, tbl1.column("id"))), None),
            ResolvedAttribute(
              "name",
              DataType.StringType,
              None,
              Some(SourceColumn(tbl1, tbl1.column("name"))),
              None
            ),
            ResolvedAttribute(
              "address",
              DataType.StringType,
              None,
              Some(SourceColumn(tbl1, tbl1.column("address"))),
              None
            )
          )
        ),
        Some(NodeLocation(1, 8))
      )
    )
  }

  test("resolve select with alias") {
    val plan = SQLAnalyzer.analyze("select id as person_id from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.toList shouldMatch {
      // Attribute should not have a qualifier
      case List(Alias(_, "person_id", r, _)) => {
        r.attributeName shouldBe "id"
        r.dataType shouldBe DataType.LongType
      }
    }
  }

  test("resolve join attributes") {
    val plan = SQLAnalyzer.analyze(
      "select a.id, a.name, a.address, b.phone as phone_num from a, b where a.id = b.id",
      "public",
      catalog
    )
    plan.resolved shouldBe true
    val attr = plan.outputAttributes.toList
    attr(0) shouldBe ResolvedAttribute(
      "id",
      DataType.LongType,
      Some("a"),
      Some(SourceColumn(tbl1, tbl1.column("id"))),
      None
    )
    attr(1) shouldBe
      ResolvedAttribute("name", DataType.StringType, Some("a"), Some(SourceColumn(tbl1, tbl1.column("name"))), None)

    attr(2) shouldBe
      ResolvedAttribute(
        "address",
        DataType.StringType,
        Some("a"),
        Some(SourceColumn(tbl1, tbl1.column("address"))),
        None
      )
    attr(3) shouldMatch { case Alias(_, "phone_num", a, _) =>
      a shouldMatch { case ResolvedAttribute("phone", DataType.StringType, _, _, _) =>
      // c shouldBe SourceColumn(tbl2, tbl2.column("phone"))
      }
    }
  }

}
