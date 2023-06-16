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
package wvlet.airframe.jdbc

import wvlet.airspec.AirSpec

import scala.util.Using

class DuckDBTest extends AirSpec {
  test("duckdb connection") {
    Using.resource(DbConfig.ofDuckDB().newConnectionPool) { pool =>
      pool.querySingle("select 1") { rs =>
        rs.getInt(1) shouldBe 1
      }
    }
  }

  case class Person(id: Int, name: String)

  test("read a duckdb file at a path") {
    val result = Seq.newBuilder[Person]
    Using.resource(DbConfig.ofDuckDB("airframe-jdbc/src/test/resources/duckdb-test.db").newConnectionPool) { pool =>
      pool.query("select * from person order by id") { rs =>
        result += Person(rs.getInt(1), rs.getString(2))
      }
    }
    val lst = result.result()
    lst shouldContain Person(1, "leo")
    lst shouldContain Person(2, "yui")
  }
}
