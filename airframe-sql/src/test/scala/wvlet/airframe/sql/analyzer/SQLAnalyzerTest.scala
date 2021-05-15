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

import wvlet.airframe.sql.catalog.{Catalog, DataType}
import wvlet.airspec.AirSpec

/**
  */
class SQLAnalyzerTest extends AirSpec {
  val tbl1 =
    Catalog
      .table("public", "a")
      .addColumn("id", DataType.LongType)
      .addColumn("name", DataType.StringType)
      .addColumn("address", DataType.StringType)

  val tbl2 =
    Catalog
      .table("public", "b")
      .addColumn("id", DataType.LongType)
      .addColumn("phone", DataType.StringType)

  val catalog =
    Catalog
      .withTable(tbl1)
      .addTable(tbl2)

  test("resolve input/output types") {
    val plan = SQLAnalyzer.analyze("select id, name from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.mkString(",") shouldBe "id:long,name:string"
  }

  test("resolve select *") {
    val plan = SQLAnalyzer.analyze("select * from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.mkString(",") shouldBe "id:long,name:string,address:string"
  }

  test("resolve select with alias") {
    val plan = SQLAnalyzer.analyze("select id as person_id from a", "public", catalog)
    plan.resolved shouldBe true
    plan.outputAttributes.mkString(",") shouldBe "person_id:long"
  }

  test("resolve join attributes") {
    val plan = SQLAnalyzer.analyze(
      "select a.id, a.name, a.address, b.phone as person_id from a, b where a.id = b.id",
      "public",
      catalog
    )
    plan.resolved shouldBe true
    plan.outputAttributes.mkString(",") shouldBe "id:long,name:string,address:string,person_id:string"
  }

}
