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
package wvlet.airframe.stream.sql.parser
import wvlet.airframe.AirframeSpec
import wvlet.airframe.stream.sql.SQLPrinter
import wvlet.airframe.stream.sql.parser.SQLParser.anonymizeSQL

/**
  *
  */
class SQLPrinterTest extends AirframeSpec {

  def roundtrip(sql: String): Unit = {
    val m1       = SQLParser.parse(sql)
    val printSql = SQLPrinter.print(m1)
    val m2       = SQLParser.parse(printSql)
    debug(m1)
    debug(m2)
    m1 shouldBe m2
  }

  "print SQL" in {
    roundtrip("select 1")
    roundtrip("select * from T")
    roundtrip("select distinct * from T")
    roundtrip("select * from T where a = 10")
    roundtrip("select * from T where a = 10 limit 1")
    roundtrip("select a, b, c from t where time <= 1000")
    roundtrip("select a, b, c from t where c = 'leo' and td_interval(time, '-1d')")
  }
}
