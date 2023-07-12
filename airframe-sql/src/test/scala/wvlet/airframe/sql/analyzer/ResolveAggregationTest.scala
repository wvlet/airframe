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

import wvlet.airframe.sql.catalog.Catalog.{CreateMode, TableColumn}
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.model.LogicalPlan.Aggregate
import wvlet.airframe.sql.model.ResolvedAttribute
import wvlet.airspec.AirSpec

class ResolveAggregationTest extends AirSpec with ResolverTestHelper {

  private val a1 = TableColumn("guid", DataType.StringType, properties = Map("tag" -> Seq("personal_identifier")))
  private val a2 = TableColumn("country", DataType.StringType)

  private val tableA = Catalog.newTable(
    "default",
    "users",
    Catalog.newSchema
      .addColumn(a1)
      .addColumn(a2)
  )

  override protected def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    catalog.createTable(tableA, CreateMode.CREATE_IF_NOT_EXISTS)
    catalog
  }

  test("resolve nested aggregation") {
    val plan = analyze("""select B.max_id, count(*) from (
      | select A.max_id, A.country from(
      |   select country, max(guid) max_id from users group by 1
      |  ) A
      |) B
      |group by 1""".stripMargin)

    plan shouldMatch { case a: Aggregate =>
      a.selectItems shouldMatch {
        case Seq(
              ResolvedAttribute("max_id", _, Some("B"), _, _, _),
              _
            ) =>
      }
    }
  }
}
