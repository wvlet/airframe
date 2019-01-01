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
package wvlet.msgframe.sql.parser

import java.io.{ByteArrayOutputStream, PrintWriter, StringWriter}

import wvlet.airframe.AirframeSpec
import wvlet.msgframe.sql.SQLBenchmark
import wvlet.msgframe.sql.SQLBenchmark.TestQuery
import wvlet.msgframe.sql.analyzer.QuerySignature
import wvlet.msgframe.sql.model.LogicalPlanPrinter

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
  def roundtrip(sql: TestQuery): Unit = {
    debug(s"roundtrip test ${sql.name}:\n${sql}")
    val m1       = SQLParser.parse(sql.sql)
    val planTree = LogicalPlanPrinter.print(m1)
    debug(planTree)

    debug(m1)
    val printSql = SQLGenerator.print(m1)
    debug(printSql)
    val m2 = SQLParser.parse(printSql)
    trace(m1)
    try {
      m1 shouldBe m2
    } catch {
      case e: Throwable =>
        warn(s"model didn't match:\n[original]\n${sql}\n\n${m1}\n\n[printed]\n${printSql}\n\n${m2}")
        throw e
    }

    val sig1 = QuerySignature.of(sql.sql)
    val sig2 = QuerySignature.of(printSql)
    try {
      sig1 shouldBe sig2
    } catch {
      case e: Throwable =>
        warn(s"signature didn't match:\n[original]\n${sql}\n\n${sig1}\n\n[printed]\n${printSql}\n\n${sig2}")
        throw e
    }

  }

  "parse selection queries" taggedAs working in {
    SQLBenchmark.selection.foreach { sql =>
      roundtrip(sql)
    }
  }

  "parse DDL queries" in {
    SQLBenchmark.ddl.foreach { sql =>
      roundtrip(sql)
    }
  }

  "parse TPC-H" in {
    SQLBenchmark.tpcH.foreach { sql =>
      roundtrip(sql)
    }
  }

  "parse TPC-DS" in {
    SQLBenchmark.tpcDS.foreach { sql =>
      roundtrip(sql)
    }
  }
}
