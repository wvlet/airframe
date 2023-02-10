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
package wvlet.airframe.sql.catalog

import wvlet.airframe.sql.SQLError
import wvlet.airframe.sql.catalog.Catalog.{CreateMode, TableSchema}
import wvlet.airspec.AirSpec

class InMemoryCatalogTest extends AirSpec {
  test("update database") {
    val c = new InMemoryCatalog("global", None, Nil)

    c.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    c.updateDatabaseProperties("default", Map("owner" -> "xxx"))
    val db = c.getDatabase("default")

    db.name shouldBe "default"
    db.properties shouldBe Map("owner" -> "xxx")

    test("forbid updating missing database") {
      intercept[SQLError] {
        c.updateDatabaseProperties("default2", Map.empty)
      }
    }
  }

  test("update table") {
    val c = new InMemoryCatalog("global", None, Nil)
    c.createDatabase(Catalog.Database("default"), CreateMode.CREATE_IF_NOT_EXISTS)
    c.createTable(
      Catalog.Table(Some("default"), "sample", TableSchema(Seq(Catalog.TableColumn("id", DataType.StringType)))),
      CreateMode.CREATE_IF_NOT_EXISTS
    )

    test("update table properties") {
      c.updateTableProperties("default", "sample", Map("table_type" -> "mapping"))

      val tbl = c.getTable("default", "sample")
      tbl.name shouldBe "sample"
      tbl.properties shouldBe Map("table_type" -> "mapping")
    }

    test("forbid updating missing table properties") {
      intercept[SQLError] {
        c.updateTableProperties("default", "sample2", Map.empty)
      }
    }

    test("update table schema") {
      val t = c.getTable("default", "sample")
      c.updateTableSchema(
        "default",
        "sample",
        TableSchema(Seq(Catalog.TableColumn("id", DataType.StringType, Map("tag" -> "pid"))))
      )

      val tbl = c.getTable("default", "sample")
      tbl.name shouldBe "sample"
      tbl.schema shouldBe TableSchema(Seq(Catalog.TableColumn("id", DataType.StringType, Map("tag" -> "pid"))))
    }
    test("forbid updating missing table schema") {
      intercept[SQLError] {
        c.updateTableSchema(
          "default",
          "sample2",
          TableSchema(Seq(Catalog.TableColumn("id", DataType.StringType, Map("tag2" -> "pid"))))
        )
      }
    }
  }
}
