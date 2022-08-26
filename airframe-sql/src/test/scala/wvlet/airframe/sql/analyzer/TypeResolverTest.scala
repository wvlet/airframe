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

import wvlet.airframe.sql.analyzer.SQLAnalyzer.PlanRewriter
import wvlet.airframe.sql.catalog.Catalog._
import wvlet.airframe.sql.catalog.{Catalog, DataType, InMemoryCatalog}
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

class TypeResolverTest extends AirSpec {

  private def demoCatalog: Catalog = {
    val catalog = new InMemoryCatalog(
      "global",
      None,
      Seq.empty
    )

    catalog.createDatabase(
      Catalog.Database("default"),
      CreateMode.CREATE_IF_NOT_EXISTS
    )
    catalog.createTable(
      Catalog.newTable(
        "default",
        "a",
        Catalog.newSchema
          .addColumn("id", DataType.LongType, properties = Map("tag" -> Seq("personal_identifier")))
          .addColumn("name", DataType.StringType, properties = Map("tag" -> Seq("private")))
      ),
      CreateMode.CREATE_IF_NOT_EXISTS
    )
    catalog
  }

  private def analyze(sql: String) = {
    val plan            = SQLParser.parse(sql)
    val analyzerContext = AnalyzerContext("default", demoCatalog).withAttributes(plan.outputAttributes)

    val rewriter: PlanRewriter = TypeResolver.resolveTableRef(analyzerContext)
    plan.transform(rewriter)
  }

  test("resolve all columns") {
    val p = analyze("select * from a")
    debug(p)
  }
}
