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

/**
  *
  */
class QuerySignatureTest extends AirframeSpec {

  "Find input/output tables" in {
    SQLBenchmark.allQueries.foreach { sql =>
      val g = TableGraph.of(sql)
      debug(g)
    }
  }

  "Generate signature" in {
    SQLBenchmark.allQueries.foreach { sql =>
      val s = QuerySignature.of(sql)
      debug(s)
    }
  }
}
