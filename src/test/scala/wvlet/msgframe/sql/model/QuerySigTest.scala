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

package wvlet.msgframe.sql.model
import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark

/**
  *
  */
class QuerySigTest extends AirframeSpec {

  "Find input/output tables" in {
    for (sql <- SQLBenchmark.allQueries) {
      val g = QuerySig.findInputOutputTableGraph(sql)
      debug(g)
    }
  }

  "Generate signature" in {
    SQLBenchmark.allQueries.foreach { sql =>
      val s = QuerySig.sig(sql)
      debug(s)
    }
  }
}
