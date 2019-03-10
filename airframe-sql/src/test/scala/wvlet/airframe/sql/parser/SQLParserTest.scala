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
package wvlet.airframe.sql.parser

import wvlet.airframe.AirframeSpec
import wvlet.airframe.sql.SQLBenchmark
import wvlet.airframe.sql.SQLBenchmark.TestQuery
import wvlet.airframe.sql.analyzer.QuerySignature

/**
  *
  */
class SQLParserTest extends AirframeSpec {

  /**
    * sql 1 -> model 1 -> sql 2 -> model 2
    *
    * model 1 should be equivalent to model 2
    *
    */
  def roundtrip(q: TestQuery): Unit = {
    debug(s"roundtrip test:\n${q.sql}")
    val m1        = SQLParser.parse(q.sql)
    val planTree1 = m1.printPlan
    debug(planTree1)

    val printSql = SQLGenerator.print(m1)
    debug(printSql)
    val m2        = SQLParser.parse(printSql)
    val planTree2 = m2.printPlan
    debug(planTree2)
    try {
      m1 shouldBe m2
    } catch {
      case e: Throwable =>
        warn(s"model didn't match:\n[original]\n${q.sql}\n\n${planTree1}\n\n[printed]\n${printSql}\n\n${planTree2}")
        throw e
    }

    val sig1 = QuerySignature.of(q.sql)
    val sig2 = QuerySignature.of(printSql)
    try {
      sig1 shouldBe sig2
    } catch {
      case e: Throwable =>
        warn(s"signature didn't match:\n[original]\n${q.sql}\n\n${sig1}\n\n[printed]\n${printSql}\n\n${sig2}")
        throw e
    }

  }

  def roundtrip(s: Seq[TestQuery]): Unit = {
    s.foreach { sql =>
      roundtrip(sql)
    }
  }

  "parse selection queries" in {
    roundtrip(SQLBenchmark.selection)
  }

  "parse DDL queries" in {
    roundtrip(SQLBenchmark.ddl)
  }

  "parse TPC-H" in {
    roundtrip(SQLBenchmark.tpcH)
  }

  "parse TPC-DS" in {
    roundtrip(SQLBenchmark.tpcDS)
  }

  "parse hive queries" in {
    roundtrip(SQLBenchmark.hive)
  }

  "parse private queries" taggedAs working in {
    roundtrip(SQLBenchmark.privateQueries)
  }
}
