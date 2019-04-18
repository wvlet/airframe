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

import wvlet.airframe.AirframeSpec
import wvlet.airframe.sql.catalog.Catalog.{Catalog, DbTable, Table, TableSchema}
import wvlet.airframe.sql.catalog.{DataType, NamedType}

/**
  *
  */
class SQLAnalyzerTest extends AirframeSpec {

  val tbl =
    Table("a", TableSchema(Seq(NamedType("id", DataType.LongType), NamedType("name", DataType.StringType))))

  val catalog =
    Catalog(Seq(DbTable("public", tbl)))

  "resolve input/output types" in {
    SQLAnalyzer.analyze("select id, name from a", "public", catalog)
  }

}
