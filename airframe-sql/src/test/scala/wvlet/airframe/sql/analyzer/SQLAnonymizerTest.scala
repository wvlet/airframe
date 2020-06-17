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
import wvlet.airframe.sql.SQLBenchmark
import wvlet.airframe.sql.SQLBenchmark.TestQuery
import wvlet.airframe.sql.model.Expression
import wvlet.airframe.sql.parser.{SQLGenerator, SQLParser}
import wvlet.airspec.AirSpec

/**
  */
class SQLAnonymizerTest extends AirSpec {
  protected def process(q: TestQuery, dict: Map[Expression, Expression]): Unit = {
    val l = SQLParser.parse(q.sql)
    debug(q.sql)
    trace(l.pp)

    val anonymizedPlan = SQLAnonymizer.anonymize(l, dict)
    debug(anonymizedPlan.pp)
    val anonymizedSQL = SQLGenerator.print(anonymizedPlan)
    debug(anonymizedSQL)
    val sig = QuerySignature.of(anonymizedSQL)
    debug(sig)
  }

  protected def processQueries(input: Seq[TestQuery]): Unit = {
    val queries = input.map(_.sql)
    val dict    = SQLAnonymizer.buildAnonymizationDictionary(queries)
    input.foreach { x => process(x, dict) }
  }

  def `anonymize standard queries`: Unit = {
    processQueries(SQLBenchmark.selection)
  }

  def `anonymize DDL queries`: Unit = {
    processQueries(SQLBenchmark.ddl)
  }

  def `anonymize TPC-H`: Unit = {
    processQueries(SQLBenchmark.tpcH)
  }

  def `anonymize TPC-DS`: Unit = {
    processQueries(SQLBenchmark.tpcDS)
  }
}
