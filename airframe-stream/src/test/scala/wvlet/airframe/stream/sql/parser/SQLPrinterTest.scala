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
import java.io.File

import wvlet.airframe.AirframeSpec
import wvlet.airframe.stream.sql.SQLPrinter
import wvlet.log.io.IOUtil

/**
  *
  */
class SQLPrinterTest extends AirframeSpec {

  def roundtrip(sql: String): Unit = {
    val m1       = SQLParser.parse(sql)
    val printSql = SQLPrinter.print(m1)
    val m2       = SQLParser.parse(printSql)
    try {
      m1 shouldBe m2
    } catch {
      case e: Throwable =>
        warn(s"model didn't match:\n[original]\n${sql}\n\n${m1}\n\n[printed]\n${printSql}\n\n${m2}")
        throw e
    }
  }

  "print SQL" in {
    roundtrip("select 1")
    roundtrip("select * from T")
    roundtrip("select distinct * from T")
    roundtrip("select * from T where a = 10")
    roundtrip("select * from T where a = 10 limit 1")
    roundtrip("select a, b, c from t where time <= 1000")
    roundtrip("select a, b, c from t where c = 'leo' and td_interval(time, '-1d')")

    roundtrip("select cast(1 as varchar)")
    roundtrip("select try_cast(1 as varchar)")
    roundtrip(s"select cast(1 as varchar) as a")
    roundtrip(s"select 1 + 2")
    roundtrip(s"select 1 - 2")
    roundtrip(s"select 1 * 2")
    roundtrip(s"select 1 / 2")
    roundtrip(s"select 1 * (2 + 4)")
    roundtrip("select 'a' || 'b'")

  }

  "print TPC-H SQL" in {
    val dir = new File("airframe-stream/src/test/resources/wvlet/airframe/stream/sql/tpc-h")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) {
      val sql = IOUtil.readAsString(f.getPath)
      roundtrip(sql)
    }
  }

  "print TPC-DS SQL" in {
    val dir = new File("airframe-stream/src/test/resources/wvlet/airframe/stream/sql/tpc-ds")
    for (f <- dir.listFiles() if f.getName.endsWith(".sql")) {
      val sql = IOUtil.readAsString(f.getPath)
      // TODO We need to support grouping sets
      if (!Seq("q18.sql", "q27.sql").contains(f.getName)) {
        roundtrip(sql)
      }
    }
  }

}
