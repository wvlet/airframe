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
package wvlet.airframe.tablet.jdbc

import java.sql.{DriverManager, JDBCType}

import wvlet.airframe.tablet.obj.ObjectTabletWriter
import wvlet.airspec.AirSpec
import wvlet.log.io.IOUtil._

/**
  *
  */
import wvlet.airframe.tablet.jdbc.SQLObjectMapperTest._
class SQLObjectMapperTest extends AirSpec {

  def `support all JDBC type mapping`: Unit = {
    // sanity test
    for (v <- JDBCType.values()) {
      SQLObjectMapper.jdbcToDataType(v)
    }
  }

  def `generate SQL`: Unit = {
    val r = R(0, 1, 2, 3, 'a', 5, true, "hello")

    // Open in-memory sqlite database
    Class.forName("org.sqlite.JDBC")
    withResource(DriverManager.getConnection("jdbc:sqlite::memory:")) { conn =>
      // Create table
      withResource(conn.createStatement()) { stmt =>
        val ct = SQLObjectMapper.createTableSQLFor[R]("mytable")
        debug(ct)
        stmt.execute(ct)
      }

      // Insert record
      SQLObjectMapper.insertRecord(conn, "mytable", r)

      // Read record
      val b = Seq.newBuilder[R]
      withResource(conn.createStatement()) { stmt =>
        withResource(stmt.executeQuery("select * from mytable")) { rs =>
          b ++= new ResultSetReader(rs).pipe(new ObjectTabletWriter[R]())
        }
      }
      val s = b.result
      debug(b.result)
    }
  }

  def `allow primary key config`: Unit = {
    val sql = SQLObjectMapper.createTableSQLFor[T1]("t1", Map("id" -> "primary key"))
    debug(sql)
    assert(sql.contains(""""id" integer primary key"""))
    assert(!sql.contains(""""name" string primary key"""))
  }
}

object SQLObjectMapperTest {

  case class T1(
      id: Long,
      name: String
  )

  case class R(
      i: Int,
      l: Long,
      f: Float,
      d: Double,
      c: Char,
      st: Short,
      b: Boolean,
      s: String
  )

}
