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

package wvlet.msgframe.sql.analyzer
import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark
import wvlet.msgframe.sql.SQLBenchmark.TestQuery
import wvlet.msgframe.sql.model.Expression
import wvlet.msgframe.sql.parser.{SQLGenerator, SQLParser}

/**
  *
  */
class SQLAnonymizerTest extends AirframeSpec {

  def process(q: TestQuery, dict: Map[Expression, Expression]): Unit = {
    val l = SQLParser.parse(q.sql)
    debug(q.sql)
    trace(l.printPlan)

    val anonymizedPlan = SQLAnonymizer.anonymize(l, dict)
    debug(anonymizedPlan.printPlan)
    val anonymizedSQL = SQLGenerator.print(anonymizedPlan)
    debug(anonymizedSQL)
    val sig = QuerySignature.of(anonymizedSQL)
    debug(sig)
  }

  def processQueries(input: Seq[TestQuery]): Unit = {
    val queries = input.map(_.sql)
    val dict    = SQLAnonymizer.buildAnonymizationDictionary(queries)
    input.foreach { x =>
      process(x, dict)
    }
  }

  "anonymize standard queries" taggedAs working in {
    processQueries(SQLBenchmark.selection)
  }

  "anonymize DDL queries" in {
    processQueries(SQLBenchmark.ddl)
  }

  "anonymize TPC-H" in {
    processQueries(SQLBenchmark.tpcH)
  }

  "anonymize TPC-DS" in {
    processQueries(SQLBenchmark.tpcDS)
  }

}
