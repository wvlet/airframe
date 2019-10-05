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

import wvlet.log.LogSupport
import wvlet.airframe._
import wvlet.airspec.AirSpec

object ConnectionPoolFactoryTest {
  type MyDbConfig1 = DbConfig
  type MyDbConfig2 = DbConfig
  type MyDbConfig3 = DbConfig
}

import wvlet.airframe.jdbc.ConnectionPoolFactoryTest._

trait TestConnection extends ConnectionPoolFactoryService with LogSupport {
  lazy val pool1 = bind { c: MyDbConfig1 =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pool2 = bind { c: MyDbConfig2 =>
    connectionPoolFactory.newConnectionPool(c)
  }
  lazy val pgPool = bind { c: MyDbConfig3 =>
    connectionPoolFactory.newConnectionPool(c, PostgreSQLConfig(useSSL = false))
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

    // Reconnection test for SQLite
    if (pool.config.`type` == "sqlite") {
      pool.stop
    }

    pool.updateWith("insert into test values(?, ?)") { ps =>
      ps.setInt(1, 2)
      ps.setString(2, "yui")
    }
    pool.queryWith("select * from test where id = ?") { ps =>
      ps.setInt(1, 2)
    } { rs =>
      while (rs.next()) {
        val id   = rs.getInt("id")
        val name = rs.getString("name")
        logger.debug(s"read with prepared statement: (${id}, ${name})")
      }
    }

    pool.executeUpdate("drop table if exists test")
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
    if (!inTravisCI) pending

    d.withSession { session =>
      val t = session.build[TestConnection]
      t.test(t.pool1)
      t.test(t.pool2)
    }
  }

  def `use PostgreSQL connection pool`: Unit = {
    if (!inTravisCI) pending

    d.withSession { session =>
      val t = session.build[TestConnection]
      t.test(t.pgPool)
    }
  }

  def `report error for unknown db type`: Unit = {
    intercept[IllegalArgumentException] {
      d.withSession { session =>
        val f = session.build[ConnectionPoolFactory]
        f.newConnectionPool(DbConfig.of("superdb"))
      }
    }
  }

  def `report error for missing postgresql host`: Unit = {
    intercept[IllegalArgumentException] {
      d.withSession { session =>
        val f = session.build[ConnectionPoolFactory]
        f.newConnectionPool(DbConfig.of("postgresql"))
      }
    }
  }
}
