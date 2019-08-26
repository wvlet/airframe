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

import wvlet.airframe.sql.catalog.Catalog.{Catalog, DbTable, TableSchema}
import wvlet.airframe.sql.catalog.{DataType, NamedType}
import wvlet.airspec.AirSpec

/**
  *
  */
class SQLAnalyzerTest extends AirSpec {

  val tbl =
    DbTable(
      Some("public"),
      "a",
      TableSchema(
        Seq(
          NamedType("id", DataType.LongType),
          NamedType("name", DataType.StringType),
          NamedType("address", DataType.StringType)
        )
      )
    )

  val catalog =
    Catalog(Seq(tbl))

  def `resolve input/output types`: Unit = {
    SQLAnalyzer.analyze("select id, name from a", "public", catalog)
  }
}
