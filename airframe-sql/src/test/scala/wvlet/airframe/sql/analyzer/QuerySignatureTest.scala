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
import wvlet.airframe.sql.model.SQLSig
import wvlet.airframe.sql.parser.SQLParser
import wvlet.airspec.AirSpec

/**
  */
class QuerySignatureTest extends AirSpec {
  def `Find input/output tables`: Unit = {
    SQLBenchmark.allQueries.foreach { sql =>
      val g = TableGraph.of(sql.sql)
      debug(g)
    }
  }

  def `Generate signature`: Unit = {
    SQLBenchmark.allQueries.foreach { sql =>
      val s = QuerySignature.of(sql.sql)
      debug(s)
    }
  }

  def `parse q72.sql`: Unit = {
    val sql = SQLBenchmark.tpcDS_("q72")
    debug(sql)
    val p = SQLParser.parse(sql.sql)
    debug(p)
    val sig = QuerySignature.of(sql.sql)
    debug(sig)
  }

  val embedTableNames = QuerySignatureConfig(embedTableNames = true)
  def `embed table names`: Unit = {
    val plan = SQLParser.parse("select * from tbl")

    plan.sig(embedTableNames) shouldBe "P[*](tbl)"
    plan.sig(QuerySignatureConfig(embedTableNames = false)) shouldBe "P[*](T)"
  }

  def `embed table names to CTAS`: Unit = {
    SQLParser.parse("insert into tbl select * from a").sig(embedTableNames) shouldBe "I(tbl,P[*](a))"
    SQLParser.parse("drop table tbl").sig(embedTableNames) shouldBe "DT(tbl)"
    SQLParser.parse("create table tbl (id int)").sig(embedTableNames) shouldBe "CT(tbl)"

    SQLParser.parse("insert into tbl select * from a").sig() shouldBe "I(T,P[*](T))"
    SQLParser.parse("drop table tbl").sig() shouldBe "DT(T)"
    SQLParser.parse("create table tbl (id int)").sig() shouldBe "CT(T)"
  }

  def `embed table names for all queries`: Unit = {
    SQLBenchmark.allQueries.foreach { x =>
      val plan = SQLParser.parse(x.sql)
      val sig  = plan.sig(QuerySignatureConfig(embedTableNames = true))
    }
  }
}
