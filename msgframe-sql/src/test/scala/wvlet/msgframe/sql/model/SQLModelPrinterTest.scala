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
import wvlet.msgframe.sql.parser.SQLParser

/**
  *
  */
class SQLModelPrinterTest extends AirframeSpec {

  def roundtrip(sql: String): Unit = {
    debug(sql)
    val m1 = SQLParser.parse(sql)
    debug(m1)
    val printSql = SQLModelPrinter.print(m1)
    debug(printSql)
    val m2 = SQLParser.parse(printSql)
    debug(m1)
    try {
      m1 shouldBe m2
    } catch {
      case e: Throwable =>
        warn(s"model didn't match:\n[original]\n${sql}\n\n${m1}\n\n[printed]\n${printSql}\n\n${m2}")
        throw e
    }
  }

  "print standard queries" taggedAs working in {
    SQLBenchmark.standardQueries.foreach { sql =>
      roundtrip(sql)
    }
  }

  "print TPC-H SQL" in {
    SQLBenchmark.tpcH.foreach { sql =>
      roundtrip(sql)
    }
  }

  "print TPC-DS SQL" in {
    SQLBenchmark.tpcDS.foreach { sql =>
      roundtrip(sql)
    }
  }

}
