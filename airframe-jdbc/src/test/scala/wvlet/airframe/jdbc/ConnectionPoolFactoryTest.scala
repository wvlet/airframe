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

import java.sql.SQLException

import wvlet.log.LogSupport
import wvlet.airframe._
import wvlet.airframe.control.Control
import wvlet.airspec.AirSpec

object ConnectionPoolFactoryTest {
  type MyDbConfig1 = DbConfig
  type MyDbConfig2 = DbConfig
  type MyDbConfig3 = DbConfig
}

import wvlet.airframe.jdbc.ConnectionPoolFactoryTest._

trait TestConnection extends LogSupport {

  lazy val pool1 = bind { (connectionPoolFactory: ConnectionPoolFactory, c: MyDbConfig1) =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pool2 = bind { (connectionPoolFactory: ConnectionPoolFactory, c: MyDbConfig2) =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pgPool = bind { (connectionPoolFactory: ConnectionPoolFactory, c: MyDbConfig3) =>
    connectionPoolFactory.newConnectionPool(c.withPostgreSQLConfig(PostgreSQLConfig(useSSL = false)))
  }

  def test(pool: ConnectionPool): Unit = {
    pool.executeUpdate("create table if not exists test(id int, name text)")
    pool.executeUpdate("insert into test values(1, 'leo')")
    pool.executeQuery("select * from test") { rs =>
      while (rs.next()) {
        val id   = rs.getInt("id")
        val name = rs.getString("name")
        logger.debug(s"read (${id}, ${name})")
      }
    }

    pool.updateWith("insert into test values(?, ?)") { ps =>
      ps.setInt(1, 2)
      ps.setString(2, "yui")
    }
    pool.queryWith("select * from test where id = ?") { ps => ps.setInt(1, 2) } { rs =>
      while (rs.next()) {
        val id   = rs.getInt("id")
        val name = rs.getString("name")
        logger.debug(s"read with prepared statement: (${id}, ${name})")
      }
    }

    pool.executeUpdate("drop table if exists test")

    pool.withTransaction { conn =>
      Control.withResource(conn.createStatement()) { stmt =>
        // trying to drop non-existing table
        stmt.execute("create table if not exists test2(id int)")
      }
    }

    try {
      pool.withTransaction { conn =>
        Control.withResource(conn.createStatement()) { stmt =>
          // trying to drop non-existing table
          stmt.execute("drop table test_abort")
        }
      }
      assert(false, "cannot reach here")
    } catch {
      case e: SQLException =>
      // OK
    }
  }
}

/**
  *
  */
class ConnectionPoolFactoryTest extends AirSpec {
  val d = newDesign
    .bind[ConnectionPoolFactory].toSingleton
    .bind[MyDbConfig1].toInstance(DbConfig.ofSQLite(path = "target/test/mydb1.sqlite"))
    .bind[MyDbConfig2].toInstance(DbConfig.ofSQLite(path = "target/test/mydb2.sqlite"))
    .bind[MyDbConfig3].toInstance(DbConfig.ofPostgreSQL(database = "travis_ci_test").withUser(user = "postgres"))
    .noLifeCycleLogging

  def `use multiple SQLite configs`: Unit = {
    d.build[TestConnection] { t =>
      t.test(t.pool1)
      t.test(t.pool2)
    }
  }

  def `use PostgreSQL connection pool`: Unit = {
    if (!inTravisCI) pending

    d.build[TestConnection] { t => t.test(t.pgPool) }
  }

  def `report error for unknown db type`: Unit = {
    intercept[IllegalArgumentException] {
      d.build[ConnectionPoolFactory] { f => f.newConnectionPool(DbConfig.of("superdb")) }
    }
  }

  def `report error for missing postgresql host`: Unit = {
    intercept[IllegalArgumentException] {
      d.build[ConnectionPoolFactory] { f => f.newConnectionPool(DbConfig.of("postgresql")) }
    }
  }
}
